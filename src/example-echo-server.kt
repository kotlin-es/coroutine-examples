import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.async
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLineAsync

// Multi-client echo server using AsyncStream for listening clients
fun main(args: Array<String>) = EventLoop.mainAsync {
	val server = AsyncServer()
	val port = 9090

	println("Preparing...")
	val charset = Charsets.UTF_8
	var lastClientId = 0
	val serverPromise = server.listen(port).eachAsync { client ->
		val clientId = lastClientId++
		println("Client#$clientId connected!")
		async<Unit> {
			try {
				while (true) {
					val line = client.readLineAsync(charset).await()
					println("Client#$clientId said: $line")
					client.writeAsync(line.toByteArray(charset)).await()
				}
			} catch (t: Throwable) {
				t.printStackTrace()
			} finally {
				println("Client#$clientId disconnected!")
			}
		}
	}
	println("Listening at $port")
	serverPromise.await()
}
