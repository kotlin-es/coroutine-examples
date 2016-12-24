package es.kotlin.async.coroutine

import es.kotlin.async.AsyncStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

fun <T> generateAsync(routine: suspend AsyncStreamController<T>.() -> Unit): AsyncStream<T> {
	val controller = AsyncStreamController<T>()
	routine.startCoroutine(controller, completion = object : Continuation<Unit> {
		override fun resume(value: Unit) {
			controller.emitter.close()
		}

		override fun resumeWithException(exception: Throwable) {
			controller.emitter.deferred.reject(exception)
		}

	})
	return controller.stream
}

class AsyncStreamController<T> {
	internal val emitter = AsyncStream.Emitter<T>()
	val stream = emitter.stream

	fun emit(value: T) {
		emitter.emit(value)
	}
}

suspend fun <T> AsyncStream<T>.each(handler: (T) -> Unit) = awaitAsync {
	this.eachAsync(handler).await()
}
