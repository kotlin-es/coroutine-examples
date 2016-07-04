package es.kotlin.collection.coroutine

import kotlin.properties.Delegates

fun <T> generate(coroutine routine: GeneratorController<T>.() -> Continuation<Unit>): Iterable<T> {
    return object :Iterable<T> {
        override fun iterator(): Iterator<T> {
            return object : Iterator<T> {
                val controller = GeneratorController<T>()

                init {
                    controller.lastContinuation = routine(controller)
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
    var lastValue: T? = null
    var hasValue = false
    lateinit var lastContinuation: Continuation<Unit>
    var done: Boolean = false

    suspend fun yield(value:T, x: Continuation<Unit>) {
        lastValue = value
        hasValue = true
        lastContinuation = x
    }
    operator fun handleResult(x: Unit, y: Continuation<Nothing>) {
        done = true
    }
}
