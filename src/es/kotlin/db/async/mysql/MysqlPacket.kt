package es.kotlin.db.async.mysql

import es.kotlin.async.coroutine.async
import es.kotlin.net.async.AsyncSocket
import java.nio.charset.Charset

/*
class MysqlPacket(
	val Encoding: Charset,
	val PacketNumber: Byte,
	val ByteData: ByteArray = ByteArray(0)
) {
	val Stream = MemoryStream(ByteData)
	val StreamReader = StreamReader (Stream)

	val length: Int get() = Stream.size
	val available: Int get() = Stream.available

	fun ReadStringzMemoryStream(): MemoryStream {
		var Buffer = MemoryStream ()
		while (Stream.Position < Stream.Length) {
			val c = Stream . ReadByte ()
			if (c == -1) break
			if (c == 0) break
			Buffer.WriteByte((byte) c)
		}
		Buffer.Position = 0
		return Buffer
	}

	fun GetPacketBytes(): ByteArray {
		return this.Stream.ToArray()
	}

	fun ReadLengthCodedStringBytes(): ByteArray? {
		val Size = ReadLengthCoded() ?: return null
		return ReadBytes(Size.toInt())
	}

	fun ReadLengthCodedString(): String? {
		val Bytes: ByteArray = ReadLengthCodedStringBytes() ?: return null
		return Encoding.GetString(Bytes)
	}

	fun ReadLengthCodedBinary(): ByteArray {
		return ReadLengthCodedStringBytes()
	}

	fun Reset() {
		Stream.Position = 0
	}

	fun ReadStringzBytes(): ByteArray {
		var Buffer = ReadStringzMemoryStream()
		var Out = ByteArray(Buffer.Length)
		Buffer.Read(Out, 0, Out.Length)
		return Out
	}

	fun ReadStringz(Encoding: Charset): String {
		var Buffer = ReadStringzMemoryStream()
		return Encoding.GetString(Buffer.GetBuffer(), 0, (int) Buffer . Length)
	}

	fun ReadByte(): Byte {
		return Stream.ReadByte()
	}

	fun ReadUByte(): Int {
		return Stream.ReadByte() and 0xFF
	}

	fun ReadBytes(Count: Int): ByteArray {
		val Bytes = ByteArray(Count)
		Stream.Read(Bytes, 0, Count)
		return Bytes
	}

	fun ReadUInt16(): Int {
		val V0 = ReadUByte()
		val V1 = ReadUByte()
		return ((V0 shl 0) or (V1 shl 8))
	}

	fun ReadUInt32(): Long {
		val V0 = ReadUInt16()
		val V1 = ReadUInt16()
		return (((V0 shl 0) or (V1 shl 16))).toLong()
	}

	fun ReadUInt64(): Long {
		val V0 = ReadUInt32()
		val V1 = ReadUInt32()
		return ((V0 shl 0) or (V1 shl 32)).toLong()
	}

	fun ReadLengthCoded(): Long? {
		var ReadCount = 0
		val First = ReadUByte()
		if (First <= 250) {
			return First.toLong()
		} else {
			when (First) {
				251 -> return null
				252 -> ReadCount = 2
				253 -> ReadCount = 3
				254 -> ReadCount = 8
			}
		}
		var Value = 0L
		for (n in 0 until ReadCount) {
			Value = Value or (ReadUByte().toLong() shl (8 * n))
		}
		return Value
	}

	fun SendToAsync(Client: AsyncSocket): Promise<Unit> = async {
		Stream.Position = 0
		val PacketSize = Stream.size
		val Header = ByteArray(4)
		Header[0] = (PacketSize ushr 0).toByte()
		Header[1] = (PacketSize ushr 8).toByte()
		Header[2] = (PacketSize ushr 16).toByte()
		Header[3] = this.PacketNumber
		Client.WriteAsync(Header).await()
		Client.WriteAsync(GetPacketBytes()).await()
		Client.FlushAsync().await()
		Unit
	}

	fun WriteNumber(BytesCount: Int, Value: Int) = async
	{
		for (int n = 0; n < BytesCount; n++)
		{
			this.Stream.WriteByte((byte) Value)
			Value > >= 8
		}
	}

	fun WriteFiller(Count: Int) {
		this.Stream.Write(ByteArray(Count), 0, Count)
	}

	fun WriteNullTerminated(Data: ByteArray?) {
		if (Data != null) this.Stream.Write(Data, 0, Data.Length)
		this.Stream.WriteByte(0)
	}

	fun WriteNullTerminated(string: String?, encoding: Charset) = WriteNullTerminated(string?.toByteArray(encoding))

	fun WriteLengthCodedInt(Value: Long) {
		var Count = 0

		if (Value <= 250) {
			Count = 1
		}
		// 16 bits
		else if (Value <= 0xffff) {
			this.Stream.WriteByte(252)
			Count = 2
		}
		// 24 bits
		else if (Value <= 0xffffff) {
			this.Stream.WriteByte(253)
			Count = 3
		}
		// 64 bits
		else {
			this.Stream.WriteByte(254)
			Count = 8
		}

		while (Count-- > 0) {
			this.Stream.WriteByte((byte)(Value))
			Value > >= 8
		}
	}

	fun WriteLengthCodedString(Value: ByteArray) {
		WriteLengthCodedInt((uint) Value . Length)
		this.Stream.Write(Value, 0, Value.Length)
	}

	fun WryteBytes(Value: ByteArray, Offset: Int = 0, Length: Int = Value.Length) {
		this.Stream.Write(Value, Offset, Length)
	}
}
*/
