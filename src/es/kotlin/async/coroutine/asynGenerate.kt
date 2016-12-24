package es.kotlin.async.coroutine

import kotlin.coroutines.Continuation
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.suspendCoroutine

// Copy from here: https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/asyncGenerate.kt
// Added extensions methods from: https://github.com/Kotlin/kotlinx.coroutines/issues/18

interface AsyncGenerator<in T> {
	suspend fun yield(value: T)
}

interface AsyncSequence<out T> {
	operator fun iterator(): AsyncIterator<T>
}

interface AsyncIterator<out T> {
	suspend operator fun hasNext(): Boolean
	suspend operator fun next(): T
}

fun <T> asyncGenerate(block: suspend AsyncGenerator<T>.() -> Unit): AsyncSequence<T> = object : AsyncSequence<T> {
	override fun iterator(): AsyncIterator<T> {
		val iterator = AsyncGeneratorIterator<T>()
		iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
		return iterator
	}
}

class AsyncGeneratorIterator<T>: AsyncIterator<T>, AsyncGenerator<T>, Continuation<Unit> {
	var computedNext = false
	var nextValue: T? = null
	var nextStep: Continuation<Unit>? = null

	// if (computesNext) computeContinuation is Continuation<T>
	// if (!computesNext) computeContinuation is Continuation<Boolean>
	var computesNext = false
	var computeContinuation: Continuation<*>? = null

	suspend fun computeHasNext(): Boolean = suspendCoroutine { c ->
		computesNext = false
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	suspend fun computeNext(): T = suspendCoroutine { c ->
		computesNext = true
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	@Suppress("UNCHECKED_CAST")
	fun resumeIterator(exception: Throwable?) {
		if (exception != null) {
			done()
			computeContinuation!!.resumeWithException(exception)
			return
		}
		if (computesNext) {
			computedNext = false
			(computeContinuation as Continuation<T>).resume(nextValue as T)
		} else {
			(computeContinuation as Continuation<Boolean>).resume(nextStep != null)
		}
	}

	override suspend fun hasNext(): Boolean {
		if (!computedNext) return computeHasNext()
		return nextStep != null
	}

	override suspend fun next(): T {
		if (!computedNext) return computeNext()
		computedNext = false
		return nextValue as T
	}

	private fun done() {
		computedNext = true
		nextStep = null
	}

	// Completion continuation implementation
	override fun resume(value: Unit) {
		done()
		resumeIterator(null)
	}

	override fun resumeWithException(exception: Throwable) {
		done()
		resumeIterator(exception)
	}

	// Generator implementation
	override suspend fun yield(value: T): Unit = suspendCoroutine { c ->
		computedNext = true
		nextValue = value
		nextStep = c
		resumeIterator(null)
	}
}

inline suspend fun <T, T2> AsyncSequence<T>.map(crossinline transform: (T) -> T2) = asyncGenerate<T2> {
	for (e in this@map) {
		yield(transform(e))
	}
}

inline suspend fun <T> AsyncSequence<T>.filter(crossinline filter: (T) -> Boolean) = asyncGenerate<T> {
	for (e in this@filter) {
		if (filter(e)) yield(e)
	}
}

suspend fun <T> AsyncSequence<T>.chunks(count: Int) = asyncGenerate<List<T>> {
	val chunk = arrayListOf<T>()

	for (e in this@chunks) {
		chunk += e
		if (chunk.size > count) {
			yield(chunk.toList())
			chunk.clear()
		}
	}

	if (chunk.size > 0) {
		yield(chunk.toList())
	}
}

suspend fun <T> AsyncSequence<T>.toList(): List<T> = asyncFun {
	val out = arrayListOf<T>()
	for (e in this@toList) out += e
	out
}

inline suspend fun <T, TR> AsyncSequence<T>.fold(initial: TR, crossinline folder: (T, TR) -> TR): TR = asyncFun {
	var result: TR = initial
	for (e in this) result = folder(e, result)
	result
}

suspend fun AsyncSequence<Int>.sum(): Int = this.fold(0) { a, b -> a + b }
