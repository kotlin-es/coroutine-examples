package es.kotlin.async.coroutine

import es.kotlin.async.AsyncStream
import es.kotlin.async.Promise

fun <T> async(coroutine routine: AwaitAsyncController<T>.() -> Continuation<Unit>): Promise<T> {
    val controller = AwaitAsyncController<T>()
    val c = routine(controller)
    c.resume(Unit)
    return controller.promise
}

class AwaitAsyncController<T> {
    private val deferred = Promise.Deferred<T>()
    val promise = deferred.promise

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

    suspend fun <T> AsyncStream<T>.each(handler: (T) -> Unit, c: Continuation<Unit>) {
        this.eachAsync(handler).await(c)
    }

    operator fun handleResult(v: T, c: Continuation<Nothing>) {
        deferred.resolve(v)
    }

    operator fun handleException(t: Throwable, c: Continuation<Nothing>) {
        deferred.reject(t)
    }
}
