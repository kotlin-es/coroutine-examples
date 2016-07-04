package es.kotlin.db.async.mysql

import com.sun.xml.internal.ws.api.message.Packet
import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.async
import es.kotlin.crypto.Hash
import es.kotlin.net.async.AsyncSocket
import es.kotlin.time.seconds
import java.nio.charset.Charset

// Converted from my .NET code at: https://github.com/soywiz/NodeNetAsync/blob/master/NodeNetAsync/Db/Mysql/

/*
class MysqlClient(
	private val Host: String = "localhost",
	private val Port: Int = 3306,
	private val User: String? = null,
	private val Password: String? = null,
	private val Database: String? = null,
	private val Debug: Boolean = false,
	private val MaxPacketSize: Int = 0x01000000,
	private val KeepAlive: Boolean = false
) {
	private var TcpSocket: AsyncSocket? = null
	private lateinit var ScrambleBuffer: ByteArray
	private lateinit var ConnectionEncoding: Charset
	private var ConnectionEncodingInternal: MysqlLanguageEnum = MysqlLanguageEnum.UTF8_UNICODE_CI
	var LastPackedId: Byte = 0

	val isConnected: Boolean get() = TcpSocket?.connected ?: false

	private fun CheckConnectedAsync() = async {
		if (!isConnected) {
			ConnectAsync().await()
		}
	}

	fun ConnectAsync() = async {
		TcpSocket = AsyncSocket.createAndConnectAsync(host = Host, port = Port, bufferSize = 1024).await()
		HandleHandshakePacket(ReadPacketAsync().await())
		SendPacketAsync(CreateNewAuthPacket()).await()
		HandleResultPacket(ReadPacketAsync().await())
		if (KeepAlive) {
			EventLoop.setInterval(5.seconds, async {
				PingAsync().await()
			})
		}
	}

	private fun HandleResultPacket(Packet: MysqlPacket) {
		var FieldCount = Packet.ReadByte()
		var AffectedRows = Packet.ReadLengthCoded()
		var InsertId = Packet.ReadLengthCoded()
		var ServerStatus = Packet.ReadUInt16()
		var WarningCount = Packet.ReadUInt16()
		var Message = Packet.ReadStringz(ConnectionEncoding)
		//Console.WriteLine("PacketNumber: {0}", Packet.PacketNumber);
		//Console.WriteLine("Result:");
		//Console.WriteLine("AffectedRows:{0}", AffectedRows);
		//Console.WriteLine("InsertId:{0}", InsertId);
		//Console.WriteLine("ServerStatus:{0}", ServerStatus);
		//Console.WriteLine("WarningCount:{0}", WarningCount);
		//Console.WriteLine("Message:{0}", Message);
	}

	private fun HandleHandshakePacket(Packet: MysqlPacket) {
		//Console.WriteLine(Packet.PacketNumber);

		//Trace.Assert(Packet.Number == 0);
		var ProtocolVersion = (MysqlProtocolVersionEnum) Packet . ReadByte ()
		var ServerVersion = Packet.ReadStringz(ConnectionEncoding)
		var ThreadId = Packet.ReadUInt32()
		var Scramble0 = Packet.ReadStringzBytes()
		var ServerCapabilitiesLow = Packet.ReadUInt16()
		var ServerLanguage = Packet.ReadByte()
		var ServerStatus = Packet.ReadUInt16()
		var ServerCapabilitiesHigh = Packet.ReadUInt16()
		var PluginLength = Packet.ReadByte()
		Packet.ReadBytes(10)
		var Scramble1 = Packet.ReadStringzBytes()
		var Extra = Packet.ReadStringz(ConnectionEncoding)

		this.ScrambleBuffer = (Scramble0 + Scramble1)

		var ServerCapabilities = (MysqlCapabilitiesSet)((ServerCapabilitiesLow shl 0) or (ServerCapabilitiesHigh shl 16))

		//Console.WriteLine("PacketNumber: {0}", Packet.PacketNumber);
		//Console.WriteLine(ProtocolVersion);
		//Console.WriteLine(ServerVersion);
		//Console.WriteLine(ServerCapabilities.ToString());
		//Console.WriteLine(Scramble0);
		//Console.WriteLine(Scramble1);
		//Console.WriteLine(Extra);
	}

	private fun CreateNextPacket(): MysqlPacket {
		//return new MysqlPacket(++LastPackedId);
		return MysqlPacket(ConnectionEncoding, 0 + 1)
	}

	private fun CreateNewAuthPacket(): MysqlPacket {
		var Token = MysqlAuth.Token(if (this.Password != null) ConnectionEncoding.GetBytes(this.Password) else null, ScrambleBuffer)
		var Packet = MysqlPacket(ConnectionEncoding, 0 + 1)
		Packet.WriteNumber(4, MysqlCapabilitiesSet.DEFAULT)
		Packet.WriteNumber(4, MaxPacketSize)
		Packet.WriteNumber(1, ConnectionEncodingInternal)
		Packet.WriteFiller(23)
		Packet.WriteNullTerminated(this.User, ConnectionEncoding)
		Packet.WriteLengthCodedString(Token)
		Packet.WriteNullTerminated(this.Database, ConnectionEncoding)
		return Packet
	}

	private fun SendPacketAsync(Packet: MysqlPacket) = async
	{
		CheckConnectedAsync().await()
		Packet.SendToAsync(this.TcpSocket).await()
	}

	private fun ReadPacketAsync(): Promise<MysqlPacket> = async {
		var HeaderData = ByteArray(4)
		TcpSocket.ReadAsync(HeaderData, 0, HeaderData.Length).await()
		var PacketLength = (HeaderData[0] < < 0) | (HeaderData[1] << 8) | (HeaderData[2] << 16)
		var PacketNumber = HeaderData[3]
		LastPackedId = PacketNumber
		var Data = ByteArray(PacketLength)
		TcpSocket.ReadAsync(Data).await()

		// Error Packet
		if (Data[0] == 0xFF) {
			var Packet = new MysqlPacket (ConnectionEncoding, PacketNumber, Data)
			Packet.ReadByte()
			var Errno = Packet.ReadUInt16()
			var SqlStateMarker = Packet.ReadByte()
			var SqlState = Encoding.UTF8.GetString(Packet.ReadBytes(5))
			var Message = Encoding.UTF8.GetString(Packet.ReadBytes(Packet.Available))
			throw(new MysqlException (Errno, SqlState, Message))
		}

		return new MysqlPacket (ConnectionEncoding, PacketNumber, Data)
	}

	fun <TType> QueryAsAsync(Query: String, vararg Params: Any?) = async
	{
		var List = arrayListOf<TType>()

		for (var Row in await QueryAsync (Query, Params))
		{
			List.Add(Row.CastTo<TType>())
		}

		return List
	}

	private fun CheckEofPacket(Packet: MysqlPacket): Boolean {
		try {
			if (Packet.ReadByte() == 0xFE) return true
		} finally {
			Packet.Reset()
		}
		return false
	}

	private fun HandleResultSetHeaderPacket(Packet: MysqlPacket): Int {
		// field_count: See the section "Types Of Result Packets" to see how one can distinguish the first byte of field_count from the first byte of an OK Packet, or other packet types.
		var FieldCount = Packet.ReadLengthCoded()
		// extra: For example, SHOW COLUMNS uses this to send the number of rows in the table.
		var Extra = Packet.ReadLengthCoded()
		return FieldCount.toInt()
	}

	private fun HandleRowDataPacket(Packet: MysqlPacket, MysqlColumns: MysqlColumns): MysqlRow {
		var MysqlRow = MysqlRow(MysqlColumns)

		for (n in 0 until MysqlColumns.Length) {
			var Cell = Packet.ReadLengthCodedString()
			MysqlRow.Cells.Add(Cell)
			//Console.WriteLine(Cell);
		}

		return MysqlRow
	}

	private fun HandleFieldPacket(Packet: MysqlPacket): MysqlField = MysqlField = MysqlField(
		Catalog = Packet.ReadLengthCodedString(),
		Database = Packet.ReadLengthCodedString(),
		Table = Packet.ReadLengthCodedString(),
		OrgTable = Packet.ReadLengthCodedString(),
		Name = Packet.ReadLengthCodedString(),
		OrgName = Packet.ReadLengthCodedString(),
		Unk1 = Packet.ReadByte(),
		Charset = Packet.ReadUInt16(),
		Length = Packet.ReadUInt32(),
		Type = (MysqlFieldTypeEnum) Packet . ReadByte (),
		Flags = (MysqlFieldFlagsSet) Packet . ReadUInt16 (),
		Decimals = Packet.ReadByte(),
		Unk2 = Packet.ReadByte(),
		Default = Packet.ReadLengthCodedBinary()
	)

	fun CloseAsync() = async<Unit> {
		TcpSocket?.closeAsync()?.await()
		TcpSocket = null
		Unit
	}

	fun Dispose() {
		//AsyncHelpers.
		//AsyncHelpers.
		//CloseAsync().GetAwaiter().
	}

	fun Quote(Param: Any?): String {
		if (Param == null) return "NULL"

		var Out = StringBuilder ()

		Out.Append("'")

		foreach(var Char in Param . ToString ())
		{
			switch(Char)
			{
				case '"': Out.Append("\\\""); break
				case '\'': Out.Append("\\'"); break
				case '\\': Out.Append("\\\\"); break
				default: Out.Append(Char); break
			}
		}

		Out.Append("'")

		return Out.ToString()
	}

	private fun ReplaceParameters(Query: String, vararg Params: Any?): String {
		if (Params.Length == 0) {
			return Query
		} else {
			int ParamIndex = 0
			var Out = new StringBuilder ()

			foreach(var Char in Query)
			{
				if (Char == '?') {
					Out.Append(Quote(Params[ParamIndex]))
					ParamIndex++
				} else {
					Out.Append(Char)
				}
			}

			//Console.WriteLine("---{0}", Out.ToString());

			return Out.ToString()
		}
	}

	AsyncTaskQueue AsyncTaskQueue = new AsyncTaskQueue()

	fun QuitAsync() = async {
		AsyncTaskQueue.EnqueueAsync(async {
			val OutPacket = MysqlPacket (ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, MysqlCommandEnum.COM_QUIT.value)
			SendPacketAsync (OutPacket).await()
		}).await()
	}

	fun PingAsync() = async

	{
		await AsyncTaskQueue . EnqueueAsync (async() =>
		{
			var OutPacket = new MysqlPacket (ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, (uint) MysqlCommandEnum . COM_PING)
			await SendPacketAsync (OutPacket)
		})
	}

	/// <summary>
	///
	/// </summary>
	/// <param name="DatabaseName"></param>
	/// <returns></returns>
	fun SelectDatabaseAsync(DatabaseName: String) = async

	{
		await AsyncTaskQueue . EnqueueAsync (async() =>
		{
			var OutPacket = new MysqlPacket (ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, (uint) MysqlCommandEnum . COM_INIT_DB)
			OutPacket.WryteBytes(ConnectionEncoding.GetBytes(DatabaseName))
			await SendPacketAsync (OutPacket)

			await ReadPacketAsync ()
		})
	}

	fun QueryAsync(Query: String, vararg Params: Any?): Promise<MysqlQueryResult> = async {
		var MysqlQueryResult = MysqlQueryResult()

		Query = ReplaceParameters(Query, Params)

		await AsyncTaskQueue . EnqueueAsync (async {
			var OutPacket = new MysqlPacket (ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, (uint) MysqlCommandEnum . COM_QUERY)
			OutPacket.WryteBytes(ConnectionEncoding.GetBytes(Query))
			await SendPacketAsync (OutPacket)

			int NumberOfFields = HandleResultSetHeaderPacket (await ReadPacketAsync ())
			//Console.WriteLine("Number of fields: {0}", NumberOfFields);

			if (NumberOfFields > 0) {
				// Read fields
				while (true) {
					var InPacket = await ReadPacketAsync ()
					if (CheckEofPacket(InPacket)) break
					MysqlQueryResult.Columns.Add(HandleFieldPacket(InPacket))
				}

				// Read words
				while (true) {
					var InPacket = await ReadPacketAsync ()
					if (CheckEofPacket(InPacket)) break
					MysqlQueryResult.Rows.Add(HandleRowDataPacket(InPacket, MysqlQueryResult.Columns))
				}
			}
		})

		return MysqlQueryResult
	}
}

object MysqlCapabilitiesSet {
	val CLIENT_LONG_PASSWORD = 1 // new more secure passwords
	val CLIENT_FOUND_ROWS = 2 // Found instead of affected rows
	val CLIENT_LONG_FLAG = 4 // Get all column flags
	val CLIENT_CONNECT_WITH_DB = 8 // One can specify db on connect
	val CLIENT_NO_SCHEMA = 16 // Don't allow database.table.column
	val CLIENT_COMPRESS = 32 // Can use compression protocol
	val CLIENT_ODBC = 64 // Odbc client
	val CLIENT_LOCAL_FILES = 128 // Can use LOAD DATA LOCAL
	val CLIENT_IGNORE_SPACE = 256 // Ignore spaces before '('
	val CLIENT_PROTOCOL_41 = 512 // New 4.1 protocol
	val CLIENT_INTERACTIVE = 1024 // This is an interactive client
	val CLIENT_SSL = 2048 // Switch to SSL after handshake
	val CLIENT_IGNORE_SIGPIPE = 4096 // IGNORE sigpipes
	val CLIENT_TRANSACTIONS = 8192 // Client knows about transactions
	val CLIENT_RESERVED = 16384 // Old flag for 4.1 protocol
	val CLIENT_SECURE_CONNECTION = 32768 // New 4.1 authentication
	val CLIENT_MULTI_STATEMENTS = 65536 // Enable/disable multi-stmt support
	val CLIENT_MULTI_RESULTS = 131072 // Enable/disable multi-results

	val DEFAULT = CLIENT_LONG_PASSWORD or CLIENT_FOUND_ROWS or
		CLIENT_LONG_FLAG or CLIENT_CONNECT_WITH_DB or
		CLIENT_ODBC or CLIENT_LOCAL_FILES or
		CLIENT_IGNORE_SPACE or CLIENT_PROTOCOL_41 or
		CLIENT_INTERACTIVE or CLIENT_IGNORE_SIGPIPE or
		CLIENT_TRANSACTIONS or CLIENT_RESERVED or
		CLIENT_SECURE_CONNECTION or CLIENT_MULTI_STATEMENTS or
		CLIENT_MULTI_RESULTS
}

internal object MysqlAuth {
	fun Token(password: ByteArray?, scrambleBuffer: ByteArray): ByteArray {
		if (password == null || password.size === 0) return ByteArray(0)
		val Stage1 = sha1(password)
		val Stage2 = sha1(Stage1)
		val Stage3 = sha1(scrambleBuffer + Stage2)
		return xor(Stage3, Stage1)
	}

	private fun sha1(data: ByteArray): ByteArray = Hash.SHA1.hash(data)

	private fun xor(left: ByteArray, right: ByteArray): ByteArray {
		val out = ByteArray(left.size)
		for (n in 0 until out.size) out[n] = (left[n].toInt() xor right[n].toInt()).toByte()
		return out
	}
}

enum class MysqlCommandEnum(val type: Byte) {
	COM_SLEEP(0x00), COM_QUIT(0x01), COM_INIT_DB(0x02), COM_QUERY(0x03),
	COM_FIELD_LIST(0x04), COM_CREATE_DB(0x05), COM_DROP_DB(0x06), COM_REFRESH(0x07),
	COM_SHUTDOWN(0x08), COM_STATISTICS(0x09), COM_PROCESS_INFO(0x0a), COM_CONNECT(0x0b),
	COM_PROCESS_KILL(0x0c), COM_DEBUG(0x0d), COM_PING(0x0e), COM_TIME(0x0f),
	COM_DELAYED_INSERT(0x10), COM_CHANGE_USER(0x11), COM_BINLOG_DUMP(0x12), COM_TABLE_DUMP(0x13),
	COM_CONNECT_OUT(0x14), COM_REGISTER_SLAVE(0x15), COM_STMT_PREPARE(0x16), COM_STMT_EXECUTE(0x17),
	COM_STMT_SEND_LONG_DATA(0x18), COM_STMT_CLOSE(0x19), COM_STMT_RESET(0x1a), COM_SET_OPTION(0x1b),
	COM_STMT_FETCH(0x1c),
}

class MysqlException(val errorCode: Int, val sqlState: String, message: String) : RuntimeException(message)

class MysqlField(
	val Catalog: String,
	val Database: String,
	val Table: String,
	val OrgTable: String,
	val Name: String,
	val OrgName: String,
	val Unk1: Int,
	val Charset: Short,
	val Length: Int,
	val Type: MysqlFieldTypeEnum,
	val Flags: MysqlFieldFlagsSet,
	val Decimals: Byte,
	val Unk2: Byte,
	val Default: ByteArray
)

enum class MysqlFieldFlagsSet(val value: Short) {
	NOT_NULL_FLAG(0x0001), PRI_KEY_FLAG(0x0002),
	UNIQUE_KEY_FLAG(0x0004), MULTIPLE_KEY_FLAG(0x0008),
	BLOB_FLAG(0x0010), UNSIGNED_FLAG(0x0020),
	ZEROFILL_FLAG(0x0040), BINARY_FLAG(0x0080),
	ENUM_FLAG(0x0100), AUTO_INCREMENT_FLAG(0x0200),
	TIMESTAMP_FLAG(0x0400), SET_FLAG(0x0800),
}

enum class MysqlFieldTypeEnum(value: Int) {
	FIELD_TYPE_DECIMAL(0x00), FIELD_TYPE_TINY(0x01),
	FIELD_TYPE_SHORT(0x02), FIELD_TYPE_LONG(0x03),
	FIELD_TYPE_FLOAT(0x04), FIELD_TYPE_DOUBLE(0x05),
	FIELD_TYPE_NULL(0x06), FIELD_TYPE_TIMESTAMP(0x07),
	FIELD_TYPE_LONGLONG(0x08), FIELD_TYPE_INT24(0x09),
	FIELD_TYPE_DATE(0x0a), FIELD_TYPE_TIME(0x0b),
	FIELD_TYPE_DATETIME(0x0c), FIELD_TYPE_YEAR(0x0d),
	FIELD_TYPE_NEWDATE(0x0e),
	FIELD_TYPE_VARCHAR(0x0f), // (new in MySQL 5.0)
	FIELD_TYPE_BIT(0x10), // (new in MySQL 5.0)
	FIELD_TYPE_NEWDECIMAL(0xf6), // (new in MYSQL 5.0)
	FIELD_TYPE_ENUM(0xf7), FIELD_TYPE_SET(0xf8),
	FIELD_TYPE_TINY_BLOB(0xf9), FIELD_TYPE_MEDIUM_BLOB(0xfa),
	FIELD_TYPE_LONG_BLOB(0xfb), FIELD_TYPE_BLOB(0xfc),
	FIELD_TYPE_VAR_STRING(0xfd), FIELD_TYPE_STRING(0xfe),
	FIELD_TYPE_GEOMETRY(0xff),
}

enum class MysqlLanguageEnum(val value: Int) {
	BIG5_CHINESE_CI(1), LATIN2_CZECH_CS(2), DEC8_SWEDISH_CI(3), CP850_GENERAL_CI(4),
	LATIN1_GERMAN1_CI(5), HP8_ENGLISH_CI(6), KOI8R_GENERAL_CI(7), LATIN1_SWEDISH_CI(8),
	LATIN2_GENERAL_CI(9), SWE7_SWEDISH_CI(10), ASCII_GENERAL_CI(11), UJIS_JAPANESE_CI(12),
	SJIS_JAPANESE_CI(13), CP1251_BULGARIAN_CI(14), LATIN1_DANISH_CI(15), HEBREW_GENERAL_CI(16),
	TIS620_THAI_CI(18), EUCKR_KOREAN_CI(19), LATIN7_ESTONIAN_CS(20), LATIN2_HUNGARIAN_CI(21),
	KOI8U_GENERAL_CI(22), CP1251_UKRAINIAN_CI(23), GB2312_CHINESE_CI(24), GREEK_GENERAL_CI(25),
	CP1250_GENERAL_CI(26), LATIN2_CROATIAN_CI(27), GBK_CHINESE_CI(28), CP1257_LITHUANIAN_CI(29),
	LATIN5_TURKISH_CI(30), LATIN1_GERMAN2_CI(31), ARMSCII8_GENERAL_CI(32), UTF8_GENERAL_CI(33),
	CP1250_CZECH_CS(34), UCS2_GENERAL_CI(35), CP866_GENERAL_CI(36), KEYBCS2_GENERAL_CI(37),
	MACCE_GENERAL_CI(38), MACROMAN_GENERAL_CI(39), CP852_GENERAL_CI(40), LATIN7_GENERAL_CI(41),
	LATIN7_GENERAL_CS(42), MACCE_BIN(43), CP1250_CROATIAN_CI(44), LATIN1_BIN(47),
	LATIN1_GENERAL_CI(48), LATIN1_GENERAL_CS(49), CP1251_BIN(50), CP1251_GENERAL_CI(51),
	CP1251_GENERAL_CS(52), MACROMAN_BIN(53), CP1256_GENERAL_CI(57), CP1257_BIN(58),
	CP1257_GENERAL_CI(59), BINARY(63), ARMSCII8_BIN(64), ASCII_BIN(65),
	CP1250_BIN(66), CP1256_BIN(67), CP866_BIN(68), DEC8_BIN(69),
	GREEK_BIN(70), HEBREW_BIN(71), HP8_BIN(72), KEYBCS2_BIN(73),
	KOI8R_BIN(74), KOI8U_BIN(75), LATIN2_BIN(77), LATIN5_BIN(78),
	LATIN7_BIN(79), CP850_BIN(80), CP852_BIN(81), SWE7_BIN(82),
	UTF8_BIN(83), BIG5_BIN(84), EUCKR_BIN(85), GB2312_BIN(86),
	GBK_BIN(87), SJIS_BIN(88), TIS620_BIN(89), UCS2_BIN(90),
	UJIS_BIN(91), GEOSTD8_GENERAL_CI(92), GEOSTD8_BIN(93), LATIN1_SPANISH_CI(94),
	CP932_JAPANESE_CI(95), CP932_BIN(96), EUCJPMS_JAPANESE_CI(97), EUCJPMS_BIN(98),
	CP1250_POLISH_CI(99), UCS2_UNICODE_CI(128), UCS2_ICELANDIC_CI(129), UCS2_LATVIAN_CI(130),
	UCS2_ROMANIAN_CI(131), UCS2_SLOVENIAN_CI(132), UCS2_POLISH_CI(133), UCS2_ESTONIAN_CI(134),
	UCS2_SPANISH_CI(135), UCS2_SWEDISH_CI(136), UCS2_TURKISH_CI(137), UCS2_CZECH_CI(138),
	UCS2_DANISH_CI(139), UCS2_LITHUANIAN_CI(140), UCS2_SLOVAK_CI(141), UCS2_SPANISH2_CI(142),
	UCS2_ROMAN_CI(143), UCS2_PERSIAN_CI(144), UCS2_ESPERANTO_CI(145), UCS2_HUNGARIAN_CI(146),
	UTF8_UNICODE_CI(192), UTF8_ICELANDIC_CI(193), UTF8_LATVIAN_CI(194), UTF8_ROMANIAN_CI(195),
	UTF8_SLOVENIAN_CI(196), UTF8_POLISH_CI(197), UTF8_ESTONIAN_CI(198), UTF8_SPANISH_CI(199),
	UTF8_SWEDISH_CI(200), UTF8_TURKISH_CI(201), UTF8_CZECH_CI(202), UTF8_DANISH_CI(203),
	UTF8_LITHUANIAN_CI(204), UTF8_SLOVAK_CI(205), UTF8_SPANISH2_CI(206), UTF8_ROMAN_CI(207),
	UTF8_PERSIAN_CI(208), UTF8_ESPERANTO_CI(209), UTF8_HUNGARIAN_CI(210),
}

enum class MysqlProtocolVersionEnum(value: Int) {
	Version0(0), Version10(10), Error(0xFF),
}

class MysqlColumns : Iterable<MysqlField> {
	private val columnsByIndex = arrayListOf<MysqlField>()
	private val columnsByName = hashMapOf<String, MysqlField>()
	private val columnIndexByName = hashMapOf<String, Int>()

	fun add(Column: MysqlField) {
		columnsByIndex += Column
		columnsByName[Column.Name] = Column
		columnIndexByName[Column.Name] = columnsByIndex.size - 1
	}

	operator fun get(index: Int) = columnsByIndex[index]
	operator fun get(name: String) = columnsByName[name]

	fun getIndexByColumnName(Name: String): Int = columnIndexByName[Name]!!

	val length: Int get() = columnsByIndex.size

	override fun iterator(): Iterator<MysqlField> = columnsByIndex.iterator()
}

class MysqlQueryResult : Iterable<MysqlRow> {
	val columns = MysqlColumns()
	val rows = arrayListOf<MysqlRow>()

	override fun iterator(): Iterator<MysqlRow> = rows.iterator()
}

//class MysqlRow : IEnumerable<KeyValuePair<string, string>>, IDictionary {
class MysqlRow(val columns: MysqlColumns) {
	val Cells = arrayListOf<String>()

	operator fun get(name: String) = this[columns.getIndexByColumnName(name)]
	operator fun get(index: Int) = Cells[index]

	//public fun CastTo<TType>(): TType
	//{
	//	var ItemValue = (TType)Activator.CreateInstance(typeof(TType));
	//	var ItemType = typeof(TType);
//
	//	for (int n = 0; n < Columns.Length; n++)
	//	{
	//		var Column = Columns[n];
	//		var Value = Cells[n];
	//		var Field = ItemType.GetField(Column.Name);
	//		if (Field != null)
	//		{
	//			object ValueObject = null;
//
	//			if (Field.FieldType == typeof(bool))
	//			{
	//				ValueObject = (int.Parse(Value) != 0);
	//			}
	//			else if (Field.FieldType == typeof(int))
	//			{
	//				ValueObject = int.Parse(Value);
	//			}
	//			else if (Field.FieldType == typeof(string))
	//			{
	//				ValueObject = Value;
	//			}
	//			else
	//			{
	//				throw(new NotImplementedException("Can't handle type '" + Field.FieldType + "'"));
	//			}
//
	//			Field.SetValueDirect(__makeref(ItemValue), ValueObject);
	//		}
	//	}
//
	//	return ItemValue;
	//}

	//public override string ToString()
	//{
	//	var Parts = new List<string>();
	//	for (int n = 0; n < Cells.Count; n++)
	//	{
	//		Parts.Add("'" + Columns[n].Name + "': '" + Cells[n] + "'");
	//	}
	//	return "MysqlRow(" + String.Join(", ", Parts) + ")";
	//}
//
	//IEnumerator<KeyValuePair<string, string>> IEnumerable<KeyValuePair<string, string>>.GetEnumerator()
	//{
	//	for (int n = 0; n < Columns.Length; n++)
	//	{
	//		var Column = Columns[n];
	//		var Value = Cells[n];
	//		yield return new KeyValuePair<string, string>(Column.Name, Value);
	//	}
	//}
//
	//IEnumerator IEnumerable.GetEnumerator()
	//{
	//	foreach (var Item in ((IEnumerable<KeyValuePair<string, string>>)this).AsEnumerable()) yield return Item;
	//}
//
	//IDictionaryEnumerator IDictionary.GetEnumerator()
	//{
	//	return new Enumerator(this);
	//}
}
*/
