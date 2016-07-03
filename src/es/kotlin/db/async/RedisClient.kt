package es.kotlin.db.async

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.AwaitAsyncController
import es.kotlin.async.coroutine.async
import es.kotlin.net.async.AsyncSocket
import es.kotlin.net.async.readLineAsync
import java.util.*

class RedisClient(
        private val host: String = "127.0.0.1",
        private val port: Int = 6379
) {
    private val charset = Charsets.UTF_8
    private val socket = AsyncSocket()

    protected fun ensureConnectAsync(): Promise<Unit> = async<Unit> {
        if (!socket.connected) {
            socket.connectAsync(host, port).await()
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
            ensureConnectAsync().await()
            socket.writeAsync(data).await()
            readValueAsync().await()
        }
    }

    fun readValueAsync(): Promise<Any> = async {
        val firstLine = socket.readLineAsync(charset).await()
        val type = firstLine[0]
        val data = firstLine.substring(1)

        when (type) {
        // Status reply
            '+' -> data
        // Error reply
            '-' -> throw RedisResponseException(data)
        // Integer reply
            ':' -> data.toLong()
        // Bulk replies
            '$' -> {
                val bytesToRead = data.toInt()
                if (bytesToRead == -1) {
                    Unit
                } else {
                    val data2 = socket.readAsync(bytesToRead).await()
                    socket.readAsync(2).await()
                    data2.toString(charset)
                }
            }
        // Array reply
            '*' -> {
                val bulksToRead = data.toLong()
                val bulks = arrayListOf<Any>()
                for (n in 0 until bulksToRead) {
                    bulks += readValueAsync().await()
                }
                bulks
            }
            else -> throw RedisResponseException("Unknown param type '$type'")
        }
    }
}

fun RedisClient.setAsync(key: String, value: String) = async<Unit> { commandAsync("set", key, value).await() }
fun RedisClient.getAsync(key: String) = async<Any> { commandAsync("get", key).await() }

class RedisResponseException(message: String) : RuntimeException(message)