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
                    // @BUG with git version:
                    /////////////////////////////////////////////////////////
                    // https://youtrack.jetbrains.com/issue/KT-12958
                    /////////////////////////////////////////////////////////
                    
                    //val data2 = socket.readAsync(bytesToRead).await()
                    //socket.readAsync(2).await()
                    //data2.toString(charset)



                    // @WORKS
                    val data2 = socket.readAsync(bytesToRead + 2).await()
                    val out = data2.toString(charset)
                    out.substring(0, out.length - 2)
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
        //println("out:$out")
        out
    }
}

//fun RedisClient.setAsync(key: String, value: String) = async<Unit> { commandAsync("set", key, value).await() } // @BUG with git version? Should produce error because there is no available handleResult?
fun RedisClient.setAsync(key: String, value: String) = async<Unit> { commandAsync("set", key, value).await(); Unit }
//fun RedisClient.setAsync(key: String, value: String) = commandAsync("set", key, value)


fun RedisClient.getAsync(key: String) = async<Any> { commandAsync("get", key).await() }
//fun RedisClient.getAsync(key: String) = commandAsync("get", key)

class RedisResponseException(message: String) : RuntimeException(message)