package es.kotlin.db.async.redis

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import es.kotlin.net.async.AsyncClient
import es.kotlin.net.async.readLineAsync

// Ported from by .NET code: https://github.com/soywiz/NodeNetAsync/blob/master/NodeNetAsync/Db/Redis/RedisClient.cs
class RedisClient(
	private val host: String = "127.0.0.1",
	private val port: Int = 6379
) {
	private val charset = Charsets.UTF_8
	private val socket = AsyncClient()

	protected fun ensureConnectAsync(): Promise<Unit> = async<Unit> {
		if (!socket.connected) {
			await(socket.connectAsync(host, port))
		}
	}

	fun commandAsync(vararg args: String): Promise<Any> {
		var cmd = "*${args.size}\r\n"

		for (arg in args) {
			val argBytes = arg.toByteArray(charset)
			cmd += "\$${argBytes.size}\r\n"
			cmd += "$arg\r\n"
		}

		val data = cmd.toByteArray(charset)
		return async<Any> {
			await(ensureConnectAsync())
			await(socket.writeAsync(data))
			await(readValueAsync())
		}
	}

	private fun readValueAsync(): Promise<Any> = async {
		val firstLine = await(socket.readLineAsync(charset))
		val type = firstLine[0]
		val data = firstLine.substring(1)

		val out: Any = when (type) {
		// Status reply
			'+' -> data
		// Error reply
			'-' -> throw RedisResponseException(data)
		// Integer reply
			':' -> data.toLong()
		// Bulk replies
			'$' -> {
				//println("data\n\n: '$data'")
				val bytesToRead = data.toInt()
				if (bytesToRead == -1) {
					Unit
				} else {
					/////////////////////////////////////////////////////////
					// @TODO: @BUG with git version:
					/////////////////////////////////////////////////////////
					// @TODO: https://youtrack.jetbrains.com/issue/KT-12958
					/////////////////////////////////////////////////////////

					//val data2 = socket.readAsync(bytesToRead).await()
					//socket.readAsync(2).await()
					//data2.toString(charset)

					// @WORKS
					val data2 = await(socket.readAsync(bytesToRead + 2))
					val out = data2.toString(charset)
					out.substring(0, out.length - 2)
				}
			}
		// Array reply
			'*' -> {
				val bulksToRead = data.toLong()
				val bulks = arrayListOf<Any>()
				for (n in 0 until bulksToRead) {
					bulks += await(readValueAsync())
				}
				bulks
			}
			else -> throw RedisResponseException("Unknown param type '$type'")
		}
		//println("out:$out")
		out
	}
}

/////////////////////////////////////////////////////////
// @TODO: @BUG with git version? Should produce error because there is no suitable handleResult? Or Just call handleResult with Unit
// @TODO: Instead of it, it just "hangs", because promise is not resolved because handleResult is not called and it is hard to locate the problem (though in this case it was easy)
/////////////////////////////////////////////////////////
//fun RedisClient.setAsync(key: String, value: String) = async<Unit> { commandAsync("set", key, value).await() }


fun RedisClient.setAsync(key: String, value: String) = commandAsync("set", key, value)
//fun RedisClient.setAsync(key: String, value: String) = commandAsync("set", key, value)


fun RedisClient.getAsync(key: String) = commandAsync("get", key)
//fun RedisClient.getAsync(key: String) = commandAsync("get", key)

fun RedisClient.existsAsync(key: String) = async<Boolean> { await(commandAsync("exists", key)) == 1L }

class RedisResponseException(message: String) : RuntimeException(message)