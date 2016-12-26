package es.kotlin.vertx

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import kotlin.coroutines.suspendCoroutine

fun Router.get(path: String, callback: (RoutingContext) -> Unit): Route {
	return this.get(path).handler { callback(it) }
}

fun RoutingContext.header(key: String, value: String) {
	this.response().putHeader(key, value)
}

fun Vertx.router(callback: Router.() -> Unit): Router {
	val router = Router.router(this)
	router.callback()
	return router
}

fun routedHttpServerAsync(port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080, callback: Router.() -> Promise<Unit> = { Promise.resolved(Unit) }): Promise<HttpServer> = async {
	val router = Router.router(vertx)

	println("Preparing router...")
	await(callback(router))
	println("Preparing router...Ok")

	vertx.createHttpServer()
		.requestHandler { req -> router.accept(req) }
		.listen(port) {
			if (it.failed()) {
				println("routedHttpServerAsync (${it.cause().message}):")
				it.cause().printStackTrace()
			} else {
				println("Listening at port ${it.result().actualPort()}")
			}
		}
}


fun <T : Any?> Promise.Deferred<T>.toVertxHandler(): Handler<AsyncResult<T>> {
	val deferred = this
	return Handler<AsyncResult<T>> { event ->
		val result = event.result()
		if (result != null) {
			deferred.resolve(event.result())
		} else {
			var cause = event.cause()
			if (cause == null) cause = RuntimeException("Invalid")
			deferred.reject(cause)
		}
	}
}


// Suggested by Roman Elizarov @ kotlinlang slack #coroutines 22nd december 2016:
// Roman Elizarov [JB] [3:51 PM]
// Why do you care about alloc? It an sync call. Its cost is already much higher than even a few extra objects.
// `vx { doSomeVertexOperation(args, it) }`
inline suspend fun <T> vx(crossinline callback: (Handler<AsyncResult<T>>) -> Unit) = suspendCoroutine<T> { c ->
	callback(object : Handler<AsyncResult<T>> {
		override fun handle(event: AsyncResult<T>) {
			if (event.succeeded()) {
				c.resume(event.result())
			} else {
				c.resumeWithException(event.cause())
			}
		}
	})
}