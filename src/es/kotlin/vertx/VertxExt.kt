package es.kotlin.vertx

import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.asyncFun
import es.kotlin.async.coroutine.awaitAsyncTask
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.io.Closeable
import java.io.File

val vertx by lazy {
	Vertx.vertx().apply {
		val vertx = this

		EventLoop.impl = object : EventLoop() {
			override fun queue(callback: () -> Unit) {
				vertx.runOnContext { callback() }
			}

			override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
				val timer = vertx.setPeriodic(ms.toLong(), {
					callback()
				})
				return Closeable {
					vertx.cancelTimer(timer)
				}
			}

			override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
				var done = false
				val timer = vertx.setTimer(ms.toLong()) {
					done = true
					callback()
				}
				return Closeable {
					if (!done) {
						done = true
						vertx.cancelTimer(timer)
					}
				}
			}

			override fun step() {
			}
		}
	}
}

val vertxFileSystem by lazy { vertx.fileSystem() }

suspend fun readResourceAsString(name: String) = asyncFun {
	readResource(name).toString(Charsets.UTF_8)
}

suspend fun readResource(name: String) = ClassLoader.getSystemClassLoader().readBytes(name)

suspend fun ClassLoader.readBytes(name: String) = awaitAsyncTask {
	val resource = getResourceAsStream(name) ?: throw RuntimeException("Can't find resource '$name'")
	resource.readBytes()
}


suspend fun File.readBytes() = asyncFun { readBuffer().bytes }

suspend fun File.readBuffer(): Buffer = vx { vertxFileSystem.readFile(absolutePath, it) }