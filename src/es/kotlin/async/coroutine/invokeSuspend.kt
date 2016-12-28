package es.kotlin.async.coroutine

import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineIntrinsics
import kotlin.coroutines.suspendCoroutine


// Useful for invoking methods with suspend
class ContinuationWait<T> {
	var completed = false
	var c_value: T? = null
	var c_exception: Throwable? = null
	var attachedContinuation: Continuation<T>? = null

	val continuation = object : Continuation<T> {
		override fun resume(value: T) {
			completed = true
			c_value = value
			attachedContinuation?.resume(value)
		}

		override fun resumeWithException(exception: Throwable) {
			completed = true
			c_exception = exception
			attachedContinuation?.resumeWithException(exception)
		}

	}

	suspend fun await(): T = suspendCoroutine { c ->
		if (completed) {
			if (c_exception != null) {
				c.resumeWithException(c_exception as Throwable)
			} else {
				c.resume(c_value as T)
			}
		} else {
			attachedContinuation = c
		}
	}
}

suspend fun Method.invokeSuspend(obj: Any?, args: List<Any?>): Any? = asyncFun {
	val method = this

	val lastParam = method.parameters.lastOrNull()
	val margs = java.util.ArrayList(args)
	var cont: ContinuationWait<*>? = null

	if (lastParam != null && lastParam.type.isAssignableFrom(Continuation::class.java)) {
		cont = ContinuationWait<Any>()
		margs += cont.continuation
	}
	val result = method.invoke(obj, *margs.toTypedArray())
	if (result == CoroutineIntrinsics.SUSPENDED) {
		cont?.await()
	} else {
		result
	}
}