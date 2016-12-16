package es.kotlin.async

import java.util.*

class AsyncStream<T> {
    typealias Handler = (T) -> Unit
    private val deferred = Promise.Deferred<Unit>()
    private val handlers = arrayListOf<Handler>()
    private val handlersCopy = arrayListOf<Handler>()

    class Emitter<T> {
        val stream = AsyncStream<T>()
        val deferred = stream.deferred
        val buffer = LinkedList<T>()

        fun emit(value: T) {
            buffer += value
            if (stream.handlers.isNotEmpty()) {
                while (buffer.isNotEmpty()) {
                    val item = buffer.removeFirst()
                    stream.handlersCopy.addAll(stream.handlers)
                    for (handler in stream.handlersCopy) handler(item)
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
    fun readOneAsync(): Promise<T> {
        val out = Promise.Deferred<T>()
        var handler: Handler? = null
        handler = { value: T ->
            handlers.remove(handler)
            out.resolve(value)
            Unit
        }
        handlers += handler
        return out.promise
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
    fun chunks(chunkSize: Int): AsyncStream<List<T>> {
        val emitter = AsyncStream.Emitter<List<T>>()

        var buffer = arrayListOf<T>()

        fun flush() {
            if (buffer.isNotEmpty()) {
                emitter.emit(buffer)
                buffer = arrayListOf<T>()
            }
        }
        this.listenAsync {
            buffer.add(it)
            if (buffer.size >= chunkSize) flush()
        }.then {
            flush()
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

    fun toListAsync(): Promise<List<T>> {
        val out = Promise.Deferred<List<T>>()
        val result = arrayListOf<T>()
        this.listenAsync {
            result += it
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
