package es.kotlin.net.async

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.util.*
import java.util.stream.Stream

class AsyncSocket {
    private val sc = AsynchronousSocketChannel.open()
    private var _connected = false

    fun connectAsync(host: String, port: Int) = connectAsync(InetSocketAddress(host, port))

    fun connectAsync(remote: SocketAddress): Promise<Unit> {
        val deferred = Promise.Deferred<Unit>()
        sc.connect(remote, this, object : CompletionHandler<Void, AsyncSocket> {
            override fun completed(result: Void?, attachment: AsyncSocket): Unit = run { _connected = true; deferred.resolve(Unit) }
            override fun failed(exc: Throwable, attachment: AsyncSocket): Unit = run { _connected = false; deferred.reject(exc) }
        })
        return deferred.promise
    }

    val connected: Boolean get() = this._connected

    //fun getAsyncStream(): Stream<ByteArray> {
        //Stream.
    //}

    fun readAsync(size: Int): Promise<ByteArray> {
        val deferred = Promise.Deferred<ByteArray>()
        val out = ByteArray(size)
        val buffer = ByteBuffer.wrap(out)
        sc.read(buffer, this, object : CompletionHandler<Int, AsyncSocket> {
            override fun completed(result: Int, attachment: AsyncSocket): Unit = run {
                if (result < 0) {
                    deferred.reject(RuntimeException("EOF"))
                } else {
                    deferred.resolve(Arrays.copyOf(out, result))
                }
            }
            override fun failed(exc: Throwable, attachment: AsyncSocket): Unit = run { deferred.reject(exc) }
        })
        return deferred.promise
    }

    fun writeAsync(data: ByteArray): Promise<Unit> {
        val deferred = Promise.Deferred<Unit>()
        val buffer = ByteBuffer.wrap(data)
        sc.write(buffer, this, object : CompletionHandler<Int, AsyncSocket> {
            override fun completed(result: Int, attachment: AsyncSocket): Unit = run { deferred.resolve(Unit) }
            override fun failed(exc: Throwable, attachment: AsyncSocket): Unit = run { deferred.reject(exc) }

        })
        return deferred.promise
    }
}

fun AsyncSocket.readLineAsync(charset: Charset): Promise<String> = async<String> {
    val os = ByteArrayOutputStream()
    // @TODO: optimize this!
    while (true) {
        val ba = readAsync(1).await()
        os.write(ba[0].toInt())
        if (ba[0].toChar() == '\n') break
    }
    val out = os.toByteArray().toString(charset)
    val res = if (out.endsWith("\r\n")) {
        out.substring(0, out.length - 2)
    } else if (out.endsWith("\n")) {
        out.substring(0, out.length - 1)
    } else {
        out
    }
    res
}
