package es.kotlin.db.async.mysql

import es.kotlin.async.AsyncStream
import es.kotlin.async.EventLoop
import es.kotlin.async.Promise
import es.kotlin.async.coroutine.AwaitAsyncController
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.generateAsync
import es.kotlin.net.async.AsyncSocket
import es.kotlin.time.seconds
import java.nio.charset.Charset

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

	private fun CheckConnectedAsync(): Promise<Unit> = async {
		if (!isConnected) {
			ConnectAsync().await()
		}
		Unit
	}

	fun ConnectAsync(): Promise<Unit> = async {
		TcpSocket = AsyncSocket.createAndConnectAsync(host = Host, port = Port, bufferSize = 1024).await()
		HandleHandshakePacket(ReadPacketAsync().await())
		SendPacketAsync(CreateNewAuthPacket()).await()
		HandleResultPacket(ReadPacketAsync().await())
		if (KeepAlive) {
			EventLoop.setInterval(5.seconds) {
				PingAsync()
			}
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
		var ProtocolVersion = MysqlProtocolVersionEnum.forInt(Packet.ReadByte())
		var ServerVersion = Packet.ReadStringz(ConnectionEncoding)
		var ThreadId = Packet.ReadUInt32()
		val Scramble0 = Packet.ReadStringzBytes()
		val ServerCapabilitiesLow = Packet.ReadUInt16()
		var ServerLanguage = Packet.ReadByte()
		var ServerStatus = Packet.ReadUInt16()
		val ServerCapabilitiesHigh = Packet.ReadUInt16()
		var PluginLength = Packet.ReadByte()
		Packet.ReadBytes(10)
		val Scramble1 = Packet.ReadStringzBytes()
		var Extra = Packet.ReadStringz(ConnectionEncoding)

		this.ScrambleBuffer = (Scramble0 + Scramble1)

		var ServerCapabilities = MysqlCapabilitiesSet.forInt((ServerCapabilitiesLow shl 0) or (ServerCapabilitiesHigh shl 16))

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
		val Token = MysqlAuth.Token(if (this.Password != null) ConnectionEncoding.GetBytes(this.Password) else null, ScrambleBuffer)
		val Packet = MysqlPacket(ConnectionEncoding, 0 + 1)
		Packet.WriteNumber(4, MysqlCapabilitiesSet.DEFAULT)
		Packet.WriteNumber(4, MaxPacketSize)
		Packet.WriteNumber(1, ConnectionEncodingInternal)
		Packet.WriteFiller(23)
		Packet.WriteNullTerminated(this.User, ConnectionEncoding)
		Packet.WriteLengthCodedString(Token)
		Packet.WriteNullTerminated(this.Database, ConnectionEncoding)
		return Packet
	}

	private fun SendPacketAsync(Packet: MysqlPacket): Promise<Unit> = async {
		CheckConnectedAsync().await()
		Packet.SendToAsync(this.TcpSocket).await()
		Unit
	}

	private fun ReadPacketAsync(): Promise<MysqlPacket> = async {
		val HeaderData = ByteArray(4)
		TcpSocket!!.readAsync(HeaderData, 0, HeaderData.size).await()
		val PacketLength = ((HeaderData[0] and 0xFF) shl 0) or ((HeaderData[1] and 0xFF) shl 8) or ((HeaderData[2] and 0xFF) shl 16)
		val PacketNumber = HeaderData[3]
		LastPackedId = PacketNumber
		val Data = ByteArray(PacketLength)
		TcpSocket!!.readAsync(Data).await()

		// Error Packet
		if ((Data[0].toInt() and 0xFF) == 0xFF) {
			val Packet = MysqlPacket(ConnectionEncoding, PacketNumber, Data)
			Packet.ReadByte()
			val Errno = Packet.ReadUInt16()
			var SqlStateMarker = Packet.ReadByte()
			val SqlState = Packet.ReadBytes(5).toString(Charsets.UTF_8)
			val Message = Packet.ReadBytes(Packet.available).toString(Charsets.UTF_8)
			throw MysqlException(Errno, SqlState, Message)
		}

		MysqlPacket(ConnectionEncoding, PacketNumber, Data)
	}

	fun <TType> QueryAs(Query: String, vararg Params: Any?): AsyncStream<TType> = generateAsync {
		for (Row in QueryAsync(Query, Params).await()) {
			emit(Row.CastTo<TType>())
		}
	}

	private fun CheckEofPacket(Packet: MysqlPacket): Boolean {
		try {
			if (Packet.ReadUByte() == 0xFE) return true
		} finally {
			Packet.Reset()
		}
		return false
	}

	private fun HandleResultSetHeaderPacket(Packet: MysqlPacket): Int {
		// field_count: See the section "Types Of Result Packets" to see how one can distinguish the first byte of field_count from the first byte of an OK Packet, or other packet types.
		val FieldCount = Packet.ReadLengthCoded()
		// extra: For example, SHOW COLUMNS uses this to send the number of rows in the table.
		var Extra = Packet.ReadLengthCoded()
		return FieldCount?.toInt() ?: 0
	}

	private fun HandleRowDataPacket(Packet: MysqlPacket, MysqlColumns: MysqlColumns): MysqlRow {
		val MysqlRow = MysqlRow(MysqlColumns)

		for (n in 0 until MysqlColumns.length) {
			val Cell = Packet.ReadLengthCodedString()
			MysqlRow.Cells += Cell
			//Console.WriteLine(Cell);
		}

		return MysqlRow
	}

	private fun HandleFieldPacket(Packet: MysqlPacket): MysqlField = MysqlField(
		Catalog = Packet.ReadLengthCodedString(),
		Database = Packet.ReadLengthCodedString(),
		Table = Packet.ReadLengthCodedString(),
		OrgTable = Packet.ReadLengthCodedString(),
		Name = Packet.ReadLengthCodedString(),
		OrgName = Packet.ReadLengthCodedString(),
		Unk1 = Packet.ReadByte().toInt(),
		Charset = Packet.ReadUInt16(),
		Length = Packet.ReadUInt32(),
		Type = MysqlFieldTypeEnum.fromInt(Packet.ReadByte ()),
		Flags = MysqlFieldFlagsSet.fromInt(Packet.ReadUInt16 ()),
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
		var Out = ""
		for (Char in Param.toString()) {
			when (Char) {
				'"' -> Out += "\\\""
				'\'' -> Out += "\\'"
				'\\' -> Out += "\\\\"
				else -> Out += Char
			}
		}
		return "'$Out'"
	}

	private fun ReplaceParameters(Query: String, vararg Params: Any?): String {
		if (Params.size == 0) return Query
		var ParamIndex = 0
		var Out = ""

		for (Char in Query) {
			if (Char == '?') {
				Out += Quote(Params[ParamIndex])
				ParamIndex++
			} else {
				Out += Char
			}
		}
		//Console.WriteLine("---{0}", Out.ToString());
		return Out
	}

	val asyncTaskQueue = AsyncTaskQueue()

	fun QuitAsync(): Promise<Unit> = async {
		asyncTaskQueue.enqueueAsync {
			val OutPacket = MysqlPacket(ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, MysqlCommandEnum.COM_QUIT.type)
			SendPacketAsync(OutPacket).await()
		}.await()
		Unit
	}

	fun PingAsync(): Promise<Unit> = async {
		asyncTaskQueue.enqueueAsync {
			val OutPacket = MysqlPacket(ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, MysqlCommandEnum.COM_PING.type)
			SendPacketAsync(OutPacket).await()
		}.await()
		Unit
	}

	fun SelectDatabaseAsync(DatabaseName: String): Promise<Unit> = async {
		asyncTaskQueue.enqueueAsync {
			val OutPacket = MysqlPacket(ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, MysqlCommandEnum.COM_INIT_DB.type)
			OutPacket.WryteBytes(DatabaseName.toByteArray(ConnectionEncoding))
			SendPacketAsync(OutPacket).await()
			ReadPacketAsync().await()
		}.await()
		Unit
	}

	fun QueryAsync(Query: String, vararg Params: Any?): Promise<MysqlQueryResult> = async {
		val MysqlQueryResult = MysqlQueryResult()

		val Query = ReplaceParameters(Query, Params)

		asyncTaskQueue.enqueueAsync {
			val OutPacket = MysqlPacket(ConnectionEncoding, 0)
			OutPacket.WriteNumber(1, MysqlCommandEnum.COM_QUERY.type)
			OutPacket.WryteBytes(Query.toByteArray(ConnectionEncoding))
			SendPacketAsync(OutPacket).await()

			val NumberOfFields = HandleResultSetHeaderPacket(ReadPacketAsync().await())
			//Console.WriteLine("Number of fields: {0}", NumberOfFields);

			if (NumberOfFields > 0) {
				// Read fields
				while (true) {
					val InPacket = ReadPacketAsync().await()
					if (CheckEofPacket(InPacket)) break
					MysqlQueryResult.columns += HandleFieldPacket(InPacket)
				}

				// Read words
				while (true) {
					val InPacket = ReadPacketAsync().await()
					if (CheckEofPacket(InPacket)) break
					MysqlQueryResult.rows += HandleRowDataPacket(InPacket, MysqlQueryResult.columns)
				}
			}
		}.await()

		return MysqlQueryResult
	}
}

class AsyncTaskQueue {
	fun enqueueAsync(coroutine routine: AwaitAsyncController<Unit>.() -> Continuation<Unit>): Promise<Unit> {
		async(routine)
	}
	Unit
}
	*/
