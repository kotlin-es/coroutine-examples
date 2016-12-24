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

suspend fun <T> limitedInTime(timeout: TimeSpan, callback: suspend () -> T) = suspendCoroutine<T> { c ->
	EventLoop.setTimeout(timeout.milliseconds.toInt()) {
		c.resumeWithException(TimeoutException())
	}

	callback.startCoroutine(object : Continuation<T> {
		override fun resume(value: T) {
			c.resume(value)
		}

		override fun resumeWithException(exception: Throwable) {
			c.resumeWithException(exception)
		}
	})
}
