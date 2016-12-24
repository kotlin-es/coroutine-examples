import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.asyncFun
import es.kotlin.lang.tryInt
import es.kotlin.net.async.AsyncClient
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.readLine

// Multi-client telnet chat server using AsyncStream for listening clients
object ChatServer {
	val server = AsyncServer()

	val charset = Charsets.UTF_8
	var lastClientId = 0L

	val clients = hashMapOf<Long, AsyncClient>()
	val EMPTY_SET = setOf<AsyncClient>()

	suspend fun broadcast(message: ByteArray, exclude: Set<AsyncClient> = EMPTY_SET) = asyncFun {
		for (client in clients.values) {
			if (client in exclude) continue
			client.write(message)
			//client.writeAsync(message) // Do not wait!
		}
	}

	suspend fun broadcastLine(line: String, exclude: Set<AsyncClient> = EMPTY_SET) = asyncFun {
		broadcast("$line\r\n".toByteArray(charset), exclude = exclude)
	}

	@JvmStatic fun main(args: Array<String>) = EventLoop.mainAsync {
		val port = args.getOrNull(0)?.tryInt() ?: 9090
		println("Preparing telnet chat server...")

		for (me in server.listen(port, started = { println("Listening at $port") })) {
			val id = lastClientId++
			clients[id] = me
			val meSet = setOf(me)
			println("Client#$id connected!")

			me.write("You are connected as Client#$id\r\n".toByteArray(charset))
			broadcastLine("Client#$id connected!", exclude = meSet)

			try {
				while (true) {
					val line = me.readLine(charset)
					println("Client#$id said: $line")
					broadcastLine("Client#$id said: $line", exclude = meSet)
				}
			} catch (t: Throwable) {
				t.printStackTrace()
			} finally {
				clients.remove(id)
				println("Client#$id disconnected!")
				broadcastLine("Client#$id disconnected!", exclude = meSet)
			}
		}
	}
}
