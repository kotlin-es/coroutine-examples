import es.kotlin.async.EventLoop
import es.kotlin.lang.tryInt
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLine

// Multi-client echo server using AsyncStream for listening clients
fun main(args: Array<String>) = EventLoop.mainAsync {
	val port = args.getOrNull(0)?.tryInt() ?: 9090
	val server = AsyncServer(port)
	println("Listening at $port")

	println("Preparing...")
	val charset = Charsets.UTF_8
	var lastClientId = 0
	for (client in server.listen()) {
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
