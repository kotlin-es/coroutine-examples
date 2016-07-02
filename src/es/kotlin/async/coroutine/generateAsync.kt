package es.kotlin.async.coroutine

import es.kotlin.async.AsyncStream
import es.kotlin.async.Promise

fun <T> generateAsync(coroutine routine: AsyncStreamController<T>.() -> Continuation<Unit>): AsyncStream<T> {
    val controller = AsyncStreamController<T>()
    val c = routine(controller)
    c.resume(Unit)
    return controller.stream
}

class AsyncStreamController<T> {
    private val emitter = AsyncStream.Emitter<T>()
    val stream = emitter.stream

    fun emit(value: T) {
        emitter.emit(value)
    }

    suspend fun <T> Promise<T>.await(c: Continuation<T>) {
        this.then(
                resolved = {
                    c.resume(it)
                },
                rejected = {
                    c.resumeWithException(it)
                }
        )
    }

    suspend fun AsyncStream<T>.each(handler: (T) -> Unit, c: Continuation<Unit>) {
        this.eachAsync(handler).await(c)
    }

    operator fun handleResult(v: Unit, c: Continuation<Nothing>) {
        emitter.close()
    }

    operator fun handleException(t: Throwable, c: Continuation<Nothing>) {
        emitter.deferred.reject(t)
    }
}
