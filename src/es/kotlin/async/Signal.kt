package es.kotlin.async

class Signal<T> {
	private val handlers = arrayListOf<(T) -> Unit>()

	fun add(handler: (T) -> Unit) {
		handlers += handler
	}

	operator fun invoke(value: T) {
		for (handler in handlers) handler.invoke(value)
	}

	operator fun invoke(value: (T) -> Unit) = add(value)
}