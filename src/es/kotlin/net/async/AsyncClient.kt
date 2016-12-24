package es.kotlin.net.async

import es.kotlin.async.coroutine.asyncFun
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.util.*
import kotlin.coroutines.suspendCoroutine

class AsyncClient(
	private val sc: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
) {
	private var _connected = false

	val millisecondsTimeout = 60 * 1000L

	companion object {
		suspend fun createAndConnect(host: String, port: Int, bufferSize: Int = 1024) = asyncFun {
			val socket = AsyncClient()
			socket.connect(host, port)
			socket
		}
	}

	suspend fun connect(host: String, port: Int) = connect(InetSocketAddress(host, port))

	suspend fun connect(remote: SocketAddress): Unit = suspendCoroutine { c ->
		sc.connect(remote, this, object : CompletionHandler<Void, AsyncClient> {
			override fun completed(result: Void?, attachment: AsyncClient): Unit = run { _connected = true; c.resume(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { _connected = false; c.resumeWithException(exc) }
		})
	}

	val connected: Boolean get() = this._connected

	//fun getAsyncStream(): Stream<ByteArray> {
	//Stream.
	//}

	suspend fun read(size: Int): ByteArray = suspendCoroutine { c ->
		val out = ByteArray(size)
		val buffer = ByteBuffer.wrap(out)
		sc.read(buffer, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = run {
				if (result < 0) {
					c.resumeWithException(RuntimeException("EOF"))
				} else {
					c.resume(Arrays.copyOf(out, result))
				}
			}

			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run {
				c.resumeWithException(exc)
			}
		})
	}

	suspend fun write(data: ByteArray) = suspendCoroutine<Unit> { c ->
		val buffer = ByteBuffer.wrap(data)
		sc.write(buffer, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = run { c.resume(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { c.resumeWithException(exc) }
		})
	}

	suspend fun close(): Unit = asyncFun {
		sc.close()
	}
}

suspend fun AsyncClient.readLine(charset: Charset = Charsets.UTF_8) = asyncFun {
	val os = ByteArrayOutputStream()
	// @TODO: optimize this!
	while (true) {
		val ba = read(1)
		os.write(ba[0].toInt())
		if (ba[0].toChar() == '\n') break
	}
	val out = os.toByteArray().toString(charset)
	val res = if (out.endsWith("\r\n")) {
		out.substring(0, out.length - 2)
	} else if (out.endsWith("\n")) {
		out.substring(0, out.length - 1)
	} else {
		out
	}
	res
}
