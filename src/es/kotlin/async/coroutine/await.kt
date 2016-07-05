package es.kotlin.async.coroutine

import es.kotlin.async.EventLoop
import es.kotlin.async.Promise
import es.kotlin.time.TimeSpan
import java.util.concurrent.TimeoutException

interface Awaitable {
	suspend fun <T> Promise<T>.await(c: Continuation<T>)
	suspend fun <T> Promise<T>.awaitWithTimeout(timeout: TimeSpan, c: Continuation<T>)

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

		override suspend fun <T> Promise<T>.awaitWithTimeout(timeout: TimeSpan, c: Continuation<T>) {
			val timer = EventLoop.setTimeout(timeout.milliseconds.toInt()) {
				this@awaitWithTimeout.cancel(TimeoutException())
			}
			this.then(
				resolved = {
					timer.dispose()
					c.resume(it)
				},
				rejected = {
					timer.dispose()
					c.resumeWithException(it)
				}
			)
		}
	}
}