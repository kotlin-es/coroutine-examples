package es.kotlin.async.coroutine

import java.lang.reflect.Method
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineIntrinsics
import kotlin.coroutines.suspendCoroutine


// Useful for invoking methods with suspend
class ContinuationWait<T> {
	val lock = ReentrantLock()
	var completed = false
	var c_value: T? = null
	var c_exception: Throwable? = null
	var attachedContinuation: Continuation<T>? = null

	inline fun <R> locked(block: () -> R): R {
		lock.lock()
		return try { block() } finally { lock.unlock() }
	}

	val continuation = object : Continuation<T> {
		override fun resume(value: T) {
			locked {
				completed = true
				c_value = value
				attachedContinuation
			}?.resume(value)
		}

		override fun resumeWithException(exception: Throwable) {
			locked {
				completed = true
				c_exception = exception
				attachedContinuation
			}?.resumeWithException(exception)
		}

	}

	suspend fun await(): T = suspendCoroutine { c ->
		var was_completed = false
		var was_c_value: T? = null
		var was_c_exception: Throwable? = null
		locked {
			was_completed = completed
			was_c_value = c_value
			was_c_exception = c_exception
			if (!was_completed) attachedContinuation = c
		}
		if (was_completed) {
			if (was_c_exception != null) {
				c.resumeWithException(was_c_exception as Throwable)
			} else {
				c.resume(was_c_value as T)
			}
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