package es.kotlin.async

import java.util.*

class AsyncStream<T> {
    typealias Handler = (T) -> Unit
    private val deferred = Promise.Deferred<Unit>()
    private val handlers = arrayListOf<Handler>()

    class Emitter<T> {
        val stream = AsyncStream<T>()
        val deferred = stream.deferred
        val buffer = LinkedList<T>()

        fun emit(value: T) {
            buffer += value
            if (stream.handlers.isNotEmpty()) {
                while (buffer.isNotEmpty()) {
                    val item = buffer.removeFirst()
                    for (handler in stream.handlers) handler(item)
                }
            }
        }
        fun close() {
            deferred.resolve(Unit)
        }
    }
    fun eachAsync(handler: Handler) = listenAsync(handler)
    fun listenAsync(handler: Handler): Promise<Unit> {
        handlers += handler
        return deferred.promise
    }
    fun <T2> map(map: (T) -> T2): AsyncStream<T2> {
        val emitter = AsyncStream.Emitter<T2>()
        this.listenAsync {
            emitter.emit(map(it))
        }.then {
            emitter.close()
        }
        return emitter.stream
    }
    fun filter(filter: (T) -> Boolean): AsyncStream<T> {
        val emitter = AsyncStream.Emitter<T>()
        this.listenAsync {
            if (filter(it)) emitter.emit(it)
        }.then {
            emitter.close()
        }
        return emitter.stream
    }
    fun <R> foldAsync(initial: R, fold: (R, T) -> R): Promise<R> {
        val out = Promise.Deferred<R>()
        var result = initial
        this.listenAsync {
            result = fold(result, it)
        }.then {
            out.resolve(result)
        }
        return out.promise
    }
}

//fun <T : Number> AsyncStream<T>.sumAsync(): Promise<T> {
fun AsyncStream<Int>.sumAsync(): Promise<Int> {
    return foldAsync(0) { a, b -> a + b }
}
