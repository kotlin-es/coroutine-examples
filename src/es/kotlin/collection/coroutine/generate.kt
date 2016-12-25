package es.kotlin.collection.coroutine

import kotlin.coroutines.Continuation
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.suspendCoroutine

// From: https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/generate.kt

@RestrictsSuspension
interface Generator<in T> {
	suspend fun yield(value: T)
}

fun <T> generate(block: suspend Generator<T>.() -> Unit): Iterable<T> = object : Iterable<T> {
	override fun iterator(): Iterator<T> {
		val iterator = GeneratorIterator<T>()
		iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
		return iterator
	}
}

private class GeneratorIterator<T>: AbstractIterator<T>(), Generator<T>, Continuation<Unit> {
	lateinit var nextStep: Continuation<Unit>

	// AbstractIterator implementation
	override fun computeNext() { nextStep.resume(Unit) }

	// Completion continuation implementation
	override fun resume(value: Unit) { done() }
	override fun resumeWithException(exception: Throwable) { throw exception }

	// Generator implementation
	override suspend fun yield(value: T) {
		setNext(value)
		return suspendCoroutine { c -> nextStep = c }
	}
}

/*
fun <T> generate(routine: suspend GeneratorController<T>.() -> Unit): Iterable<T> {
	return object : Iterable<T> {
		override fun iterator(): Iterator<T> {
			return object : Iterator<T> {
				val controller = GeneratorController<T>()

				init {
					controller.lastContinuation = routine.createCoroutine(
						controller,
						completion = object : Continuation<Unit> {
							override fun resume(value: Unit) = run { controller.done = true }
							override fun resumeWithException(exception: Throwable) = run { throw exception }
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
*/
