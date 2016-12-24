package es.kotlin.async.coroutine

import es.kotlin.async.EventLoop
import es.kotlin.async.Promise
import es.kotlin.time.TimeSpan
import java.util.concurrent.TimeoutException
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

inline suspend fun <T> awaitAsync(routine: suspend () -> T) = asyncFun(routine)

// No need for promises here at all!
inline suspend fun <T> asyncFun(routine: suspend () -> T): T = suspendCoroutine<T> { routine.startCoroutine(it) }

fun <T> async(routine: suspend () -> T): Promise<T> {
	val deferred = Promise.Deferred<T>()
	routine.startCoroutine(completion = object : Continuation<T> {
		override fun resume(value: T) = Unit.apply { deferred.resolve(value) }
		override fun resumeWithException(exception: Throwable) = Unit.apply { deferred.reject(exception) }

	})
	return deferred.promise
}

suspend fun <T> await(p: Promise<T>) = suspendCoroutine<T>(p::then)

suspend fun <T> Promise<T> .awaitWithTimeout(timeout: TimeSpan) = suspendCoroutine<T> { c ->
	val p = this@awaitWithTimeout
	val timer = EventLoop.setTimeout(timeout.milliseconds.toInt()) {
		p.cancel(TimeoutException())
	}
	p.then(
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
