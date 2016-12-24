package es.kotlin.collection.coroutine

import kotlin.coroutines.Continuation
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.suspendCoroutine

fun <T> generate(routine: suspend GeneratorController<T>.() -> Unit): Iterable<T> {
	return object : Iterable<T> {
		override fun iterator(): Iterator<T> {
			return object : Iterator<T> {
				val controller = GeneratorController<T>()

				init {
					controller.lastContinuation = routine.createCoroutine(
						controller,
						completion = object : Continuation<Unit> {
							override fun resume(value: Unit) {
								controller.done = true
							}

							override fun resumeWithException(exception: Throwable) {
								throw exception
							}
						}
					)
				}

				private fun prepare() {
					if (!controller.hasValue) {
						controller.lastContinuation.resume(Unit)
					}
				}

				override fun hasNext(): Boolean {
					prepare()
					return !controller.done
				}

				override fun next(): T {
					prepare()
					val v = controller.lastValue
					controller.hasValue = false
					return v as T
				}
			}
		}
	}
}

class GeneratorController<T> {
	internal var lastValue: T? = null
	internal var hasValue = false
	internal lateinit var lastContinuation: Continuation<Unit>
	internal var done: Boolean = false

	suspend fun yield(value: T) = suspendCoroutine<Unit> { x ->
		lastValue = value
		hasValue = true
		lastContinuation = x
	}
}
