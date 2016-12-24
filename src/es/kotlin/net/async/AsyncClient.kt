package es.kotlin.net.async

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import es.kotlin.async.coroutine.awaitAsync
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.util.*

class AsyncClient(
	private val sc: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
) {
	private var _connected = false

	val millisecondsTimeout = 60 * 1000L

	companion object {
		fun createAndConnectAsync(host: String, port: Int, bufferSize: Int = 1024): Promise<AsyncClient> = async {
			val socket = AsyncClient()
			await(socket.connectAsync(host, port))
			socket
		}
	}

	fun connectAsync(host: String, port: Int) = connectAsync(InetSocketAddress(host, port))

	fun connectAsync(remote: SocketAddress): Promise<Unit> {
		val deferred = Promise.Deferred<Unit>()
		sc.connect(remote, this, object : CompletionHandler<Void, AsyncClient> {
			override fun completed(result: Void?, attachment: AsyncClient): Unit = run { _connected = true; deferred.resolve(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { _connected = false; deferred.reject(exc) }
		})
		return deferred.promise
	}

	val connected: Boolean get() = this._connected

	//fun getAsyncStream(): Stream<ByteArray> {
	//Stream.
	//}

	fun readAsync(size: Int): Promise<ByteArray> {
		val deferred = Promise.Deferred<ByteArray>()
		val out = ByteArray(size)
		val buffer = ByteBuffer.wrap(out)
		sc.read(buffer, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = run {
				if (result < 0) {
					deferred.reject(RuntimeException("EOF"))
				} else {
					deferred.resolve(Arrays.copyOf(out, result))
				}
			}

			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { deferred.reject(exc) }
		})
		deferred.onCancel {
			// @TODO: Cancel reading!
			println("@TODO: Cancel reading!")
		}
		return deferred.promise
	}

	suspend fun write(data: ByteArray) = await(writeAsync(data))

	fun writeAsync(data: ByteArray): Promise<Unit> {
		val deferred = Promise.Deferred<Unit>()
		val buffer = ByteBuffer.wrap(data)
		sc.write(buffer, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = run { deferred.resolve(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { deferred.reject(exc) }
		})
		deferred.onCancel {
			// @TODO: Cancel writting!
			println("@TODO: Cancel writting!")
		}
		return deferred.promise
	}

	fun closeAsync(): Promise<Unit> {
		sc.close()
		return Promise.resolved(Unit)
	}
}

suspend fun AsyncClient.readLine(charset: Charset = Charsets.UTF_8) = await(readLineAsync(charset))

fun AsyncClient.readLineAsync(charset: Charset = Charsets.UTF_8): Promise<String> = async<String> {
	val os = ByteArrayOutputStream()
	// @TODO: optimize this!
	while (true) {
		val ba = await(readAsync(1))
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
