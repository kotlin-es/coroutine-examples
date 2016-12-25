package es.kotlin.async

import es.kotlin.async.coroutine.async
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedDeque

open class EventLoop {
	data class TimerHandler(val time: Long, val handler: () -> Unit)

	companion object {
		var impl = EventLoop()

		fun mainAsync(routine: suspend () -> Unit): Unit = impl.mainAsync(routine)
		fun main(entry: (() -> Unit)? = null) = impl.main(entry)

		fun queue(handler: () -> Unit) = impl.queue(handler)
		fun step() = impl.step()
		fun setImmediate(handler: () -> Unit) = impl.queue(handler)
		fun setTimeout(ms: Int, callback: () -> Unit): Closeable = impl.setTimeout(ms, callback)
		fun setInterval(ms: Int, callback: () -> Unit): Closeable = impl.setInterval(ms, callback)
	}

	val handlers = ConcurrentLinkedDeque<() -> Unit>()
	var timerHandlers = ConcurrentLinkedDeque<TimerHandler>()
	var timerHandlersBack = ConcurrentLinkedDeque<TimerHandler>()

	fun mainAsync(routine: suspend () -> Unit): Unit {
		main {
			async(routine)
		}
	}

	fun main(entry: (() -> Unit)? = null) {
		entry?.invoke()

		while (handlers.isNotEmpty() || timerHandlers.isNotEmpty() || Thread.activeCount() > 1) {
			step()
			Thread.sleep(1L)
		}
	}

	open fun step() {
		while (handlers.isNotEmpty()) {
			val handler = handlers.removeFirst()
			handler?.invoke()
		}
		val now = System.currentTimeMillis()
		while (timerHandlers.isNotEmpty()) {
			val handler = timerHandlers.removeFirst()
			if (now >= handler.time) {
				handler.handler()
			} else {
				timerHandlersBack.add(handler)
			}
		}
		val temp = timerHandlersBack
		timerHandlersBack = timerHandlers
		timerHandlers = temp
	}

	open fun queue(handler: () -> Unit) {
		handlers += handler
	}

	fun setImmediate(handler: () -> Unit) = queue(handler)

	open fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		val handler = TimerHandler(System.currentTimeMillis() + ms, callback)
		timerHandlers.add(handler)
		return object : Closeable {
			override fun close(): Unit = run { timerHandlers.remove(handler) }
		}
	}

	open fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		var ccallback: (() -> Unit)? = null
		var disposable: Closeable? = null

		ccallback = {
			callback()
			disposable = setTimeout(ms, ccallback!!)
		}

		//ccallback()
		disposable = setTimeout(ms, ccallback!!)

		return object : Closeable {
			override fun close(): Unit = run { disposable?.close() }
		}
	}
}
