package es.kotlin.net.async

import es.kotlin.async.AsyncStream
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class AsyncServer {
	val ssc = AsynchronousServerSocketChannel.open()

	fun listen(port: Int, host: String = "127.0.0.1"): AsyncStream<AsyncSocket> = listen(InetSocketAddress(host, port))

	fun listen(local: SocketAddress): AsyncStream<AsyncSocket> {
		ssc.bind(local)

		val emitter = AsyncStream.Emitter<AsyncSocket>()

		class Accept : CompletionHandler<AsynchronousSocketChannel, AsyncServer> {
			override fun completed(result: AsynchronousSocketChannel, attachment: AsyncServer?) {
				emitter.emit(AsyncSocket(result))
				ssc.accept(this@AsyncServer, this)
			}

			override fun failed(exc: Throwable?, attachment: AsyncServer?) {
				exc?.printStackTrace()
			}
		}

		ssc.accept(this, Accept())

		return emitter.stream
	}
}