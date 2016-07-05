package es.kotlin.async

class Signal<T> {
	typealias Handler = (T) -> Unit

	private val handlers = arrayListOf<Handler>()

	fun add(handler: Handler) {
		handlers += handler
	}

	operator fun invoke(value: T) {
		for (handler in handlers) handler.invoke(value)
	}
}