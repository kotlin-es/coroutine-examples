package es.kotlin.collection.coroutine

fun <T> generate(coroutine routine: GeneratorController<T>.() -> Continuation<Unit>): Iterable<T> {
    return object :Iterable<T> {
        override fun iterator(): Iterator<T> {
            return object : Iterator<T> {
                val controller = GeneratorController<T>()
                val continuation = routine(controller)

                private fun prepare() {
                    if (controller.lastValue == null) {
                        continuation.resume(Unit)
                    }
                }

                override fun hasNext(): Boolean {
                    prepare()
                    return !controller.done
                }

                override fun next(): T {
                    prepare()
                    val v = controller.lastValue
                    controller.lastValue = null
                    return v!!
                }
            }
        }
    }
}

class GeneratorController<T> {
    var lastValue: T? = null
    var done: Boolean = false

    suspend fun yield(value:T, x: Continuation<Unit>) {
        lastValue = value
    }
    operator fun handleResult(x: Unit, y: Continuation<Nothing>) {
        done = true
    }
}
