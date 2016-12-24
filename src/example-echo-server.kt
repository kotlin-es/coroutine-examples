import es.kotlin.async.EventLoop
import es.kotlin.lang.tryInt
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLine

// Multi-client echo server using AsyncStream for listening clients
fun main(args: Array<String>) = EventLoop.mainAsync {
	val server = AsyncServer()
	val port = args.getOrNull(0)?.tryInt() ?: 9090

	println("Preparing...")
	val charset = Charsets.UTF_8
	var lastClientId = 0
	for (client in server.listen(port, started = { println("Listening at $port") })) {
		val id = lastClientId++
		println("Client#$id connected!")
		try {
			while (true) {
				val line = client.readLine(charset)
				println("Client#$id said: $line")
				client.write(line.toByteArray(charset))
			}
		} catch (t: Throwable) {
			t.printStackTrace()
		} finally {
			println("Client#$id disconnected!")
		}
	}
	println("Listening at $port")
}
