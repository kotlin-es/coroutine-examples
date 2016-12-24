import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import es.kotlin.lang.tryInt
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLineAsync

// Multi-client echo server using AsyncStream for listening clients
fun main(args: Array<String>) = EventLoop.mainAsync {
	val server = AsyncServer()
	val port = args.getOrNull(0)?.tryInt() ?: 9090

	println("Preparing...")
	val charset = Charsets.UTF_8
	var lastClientId = 0
	val serverPromise = server.listen(port).eachAsync { client ->
		val id = lastClientId++
		println("Client#$id connected!")
		async<Unit> {
			try {
				while (true) {
					val line = client.readLineAsync(charset).await()
					println("Client#$id said: $line")
					client.writeAsync(line.toByteArray(charset)).await()
				}
			} catch (t: Throwable) {
				t.printStackTrace()
			} finally {
				println("Client#$id disconnected!")
			}
		}
	}
	println("Listening at $port")
	serverPromise.await()
}
