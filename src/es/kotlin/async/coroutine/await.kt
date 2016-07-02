package es.kotlin.async.coroutine

import es.kotlin.async.Promise

interface Awaitable {
    suspend fun <T> Promise<T>.await(c: Continuation<T>)

    open class Mixin : Awaitable {
        override suspend fun <T> Promise<T>.await(c: Continuation<T>) {
            this.then(
                    resolved = {
                        c.resume(it)
                    },
                    rejected = {
                        c.resumeWithException(it)
                    }
            )
        }
    }
}