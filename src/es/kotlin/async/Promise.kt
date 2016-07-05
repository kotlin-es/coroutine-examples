package es.kotlin.async

import java.util.*
import java.util.concurrent.TimeoutException

class Promise<T> {
    typealias ResolvedHandler = (T) -> Unit
    typealias RejectedHandler = (Throwable) -> Unit

    class Deferred<T> {
        val promise = Promise<T>()

        fun resolve(value: T) = promise.complete(value, null)
        fun reject(error: Throwable) = promise.complete(null, error)

        fun onCancel(handler: (Throwable) -> Unit) {
            promise.cancelHandlers += handler
        }
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
    private val cancelHandlers = LinkedList<RejectedHandler>()

    private fun flush() {
        if (!done) return
        if (error != null) {
            while (rejectedHandlers.isNotEmpty()) {
                val handler = rejectedHandlers.removeFirst()
                EventLoop.setImmediate { handler(error ?: RuntimeException()) }
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
            if (value == null && error == null) {
                throw RuntimeException("Invalid completion!")
            }

            this.value = value
            this.error = error
            this.done = true

            if (error != null && this.rejectedHandlers.isEmpty()) {
                error.printStackTrace()
            }

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

    fun cancel(reason: Throwable) {
        for (handler in cancelHandlers) handler(reason)
        complete(null, reason)
    }
}
