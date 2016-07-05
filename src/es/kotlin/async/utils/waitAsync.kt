package es.kotlin.async.utils

import es.kotlin.async.EventLoop
import es.kotlin.async.Promise
import es.kotlin.time.TimeSpan

// @TODO: this would be cool!
//suspend fun Awaitable.wait(time: TimeSpan, c: Continuation<Unit>) {
//    waitAsync()
//}

fun waitAsync(time: TimeSpan) = waitAsync(time, Unit)

fun <T> waitAsync(time: TimeSpan, result: T): Promise<T> {
	val deferred = Promise.Deferred<T>()
	val timer = EventLoop.setTimeout(time.milliseconds.toInt()) {
		if (result is Throwable) deferred.reject(result) else deferred.resolve(result)
	}
	deferred.onCancel {
		timer.dispose()
	}
	return deferred.promise
}
