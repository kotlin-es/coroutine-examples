package es.kotlin.async

import java.util.*

class Promise<T> {
    typealias ResolvedHandler = (T) -> Unit
    typealias RejectedHandler = (Throwable) -> Unit

    class Deferred<T> {
        val promise = Promise<T>()

        fun resolve(value: T) = promise.complete(value, null)
        fun reject(error: Throwable) = promise.complete(null, error)
    }

    companion object {
        fun <T> resolved(value: T) = Promise<T>().complete(value, null)
        fun <T> rejected(error: Throwable) = Promise<T>().complete(null, error)
    }

    private var value: T? = null
    private var error: Throwable? = null
    private var done: Boolean = false
    private val resolvedHandlers = LinkedList<ResolvedHandler>()
    private val rejectedHandlers = LinkedList<RejectedHandler>()

    private fun flush() {
        if (!done) return
        if (error != null) {
            while (rejectedHandlers.isNotEmpty()) {
                val handler = rejectedHandlers.removeFirst()
                EventLoop.setImmediate { handler(error!!) }
            }
        } else {
            while (resolvedHandlers.isNotEmpty()) {
                val handler = resolvedHandlers.removeFirst()
                EventLoop.setImmediate { handler(value!!) }
            }
        }
    }

    internal fun complete(value: T?, error: Throwable?): Promise<T> {
        if (!this.done) {
            this.value = value
            this.error = error
            this.done = true
            flush()
        }
        return this
    }

    fun then(resolved: ResolvedHandler) {
        resolvedHandlers += resolved
        flush()
    }

    fun then(resolved: ResolvedHandler, rejected: RejectedHandler) {
        resolvedHandlers += resolved
        rejectedHandlers += rejected
        flush()
    }
}
