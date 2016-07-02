package es.kotlin.async.utils

import es.kotlin.async.EventLoop
import es.kotlin.async.Promise
import es.kotlin.time.TimeSpan

fun waitAsync(time: TimeSpan) = waitAsync(time, Unit)

fun <T> waitAsync(time: TimeSpan, result: T): Promise<T> {
    val deferred = Promise.Deferred<T>()
    EventLoop.setTimeout(time.milliseconds.toInt()) {
        if (result is Throwable) deferred.reject(result) else deferred.resolve(result)
    }
    return deferred.promise
}
