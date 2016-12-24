package es.kotlin.async.utils

import es.kotlin.async.EventLoop
import es.kotlin.time.TimeSpan
import kotlin.coroutines.suspendCoroutine

suspend fun sleep(time: TimeSpan) = suspendCoroutine<Unit> { c ->
	EventLoop.setTimeout(time.milliseconds.toInt()) {
		c.resume(Unit)
	}
}
