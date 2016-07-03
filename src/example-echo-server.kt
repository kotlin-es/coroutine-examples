import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.async
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLineAsync

fun main(args: Array<String>) = EventLoop.mainAsync {
	val server = AsyncServer()
	val port = 9090

	println("Preparing...")
	val serverPromise = server.listen(port).eachAsync { client ->
		println("Client connected!")
		async<Unit> {
			try {
				while (true) {
					val charset = Charsets.UTF_8
					val line = client.readLineAsync(charset).await()
					println("Client said: $line")
					client.writeAsync(line.toByteArray(charset)).await()
				}
			} catch (t: Throwable) {
				t.printStackTrace()
			} finally {
				println("Client disconnected!")
			}
		}
	}
	println("Listening at $port")
	serverPromise.await()
}
