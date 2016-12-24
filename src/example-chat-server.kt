import es.kotlin.async.EventLoop
import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import es.kotlin.lang.tryInt
import es.kotlin.net.async.AsyncServer
import es.kotlin.net.async.AsyncSocket
import es.kotlin.net.async.readLineAsync

// Multi-client telnet chat server using AsyncStream for listening clients
fun main(args: Array<String>) = EventLoop.mainAsync {
	val server = AsyncServer()
	val port = args.getOrNull(0)?.tryInt() ?: 9090

	println("Preparing telnet chat server...")
	val charset = Charsets.UTF_8
	var lastClientId = 0L

	val clients = hashMapOf<Long, AsyncSocket>()
	val EMPTY_SET = setOf<AsyncSocket>()

	fun broadcastAsync(message: ByteArray, exclude: Set<AsyncSocket> = EMPTY_SET): Promise<Unit> = async {
		for (client in clients.values) {
			if (client in exclude) continue
			client.writeAsync(message).await()
			//client.writeAsync(message) // Do not wait!
		}
	}

	fun broadcastLineAsync(line: String, exclude: Set<AsyncSocket> = EMPTY_SET): Promise<Unit> = async {
		broadcastAsync("$line\r\n".toByteArray(charset), exclude = exclude).await()
	}

	val serverPromise = server.listen(port).eachAsync { me ->
		async<Unit> {
			val id = lastClientId++
			clients[id] = me
			val meSet = setOf(me)
			println("Client#$id connected!")

			me.writeAsync("You are connected as Client#$id\r\n".toByteArray(charset)).await()
			broadcastLineAsync("Client#$id connected!", exclude = meSet).await()

			try {
				while (true) {
					val line = me.readLineAsync(charset).await()
					println("Client#$id said: $line")
					broadcastLineAsync("Client#$id said: $line", exclude = meSet).await()
				}
			} catch (t: Throwable) {
				t.printStackTrace()
			} finally {
				clients.remove(id)
				println("Client#$id disconnected!")
				broadcastLineAsync("Client#$id disconnected!", exclude = meSet).await()
			}
		}
	}
	println("Listening at $port")
	serverPromise.await()
}
