package es.kotlin.vertx.route

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import es.kotlin.async.toContinuation
import es.kotlin.di.AsyncInjector
import ext.lang.DynamicConvert
import io.netty.handler.codec.http.QueryStringDecoder
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineIntrinsics

annotation class Route(val method: HttpMethod, val path: String)

annotation class Param(val name: String, val limit: Int = -1)
annotation class Post(val name: String, val limit: Int = -1)

suspend inline fun <reified T : Any> Router.registerRouter(injector: AsyncInjector) = await(this.registerRouterAsync(injector, T::class.java))
suspend fun Router.registerRouter(injector: AsyncInjector, clazz: Class<*>) = await(this.registerRouterAsync(injector, clazz))

fun Router.registerRouterAsync(injector: AsyncInjector, clazz: Class<*>): Promise<Unit> = async {
	val router = this@registerRouterAsync
	val instance = await(injector.getAsync(clazz))

	for (method in clazz.declaredMethods) {
		val route = method.getAnnotation(Route::class.java)
		if (route != null) {
			router.route(route.method, route.path).handler { rreq ->
				val res = rreq.response()

				val req = rreq.request()
				val contentType = req.headers().get("Content-Type")

				val bodyHandler = Promise.Deferred<Map<String, List<String>>>()

				req.bodyHandler { buf ->
					if ("application/x-www-form-urlencoded" == contentType) {
						val qsd = QueryStringDecoder(buf.toString(), false)
						val params = qsd.parameters()
						bodyHandler.resolve(params)
					}
				}

				async {
					try {
						var deferred: Promise.Deferred<Any>? = null
						val args = arrayListOf<Any?>()
						for ((paramType, annotations) in method.parameterTypes.zip(method.parameterAnnotations)) {
							val get = annotations.filterIsInstance<Param>().firstOrNull()
							val post = annotations.filterIsInstance<Post>().firstOrNull()
							if (get != null) {
								args += DynamicConvert(rreq.pathParam(get.name), paramType)
							} else if (post != null) {
								val postParams = await(bodyHandler.promise)
								val result = postParams[post.name]?.firstOrNull()
								args += DynamicConvert(result, paramType)
							} else if (Continuation::class.java.isAssignableFrom(paramType)) {
								deferred = Promise.Deferred<Any>()
								args += deferred.toContinuation()
							} else {
								throw RuntimeException("Expected @Get annotation")
							}
						}

						val result = method.invoke(instance, *args.toTypedArray())

						if (result != CoroutineIntrinsics.SUSPENDED && deferred != null) {
							deferred.resolve(result)
						}

						val finalResult = if (deferred != null) {
							await(deferred.promise)
						} else {
							if (result is Promise<*>) {
								await(result)
							} else {
								result
							}
						}

						when (finalResult) {
							is String -> res.end("$finalResult")
							else -> res.end(Json.encode(finalResult))
						}
					} catch (t: Throwable) {
						println("Router.registerRouterAsync (${t.message}):")
						t.printStackTrace()
						res.statusCode = 500
						val t2 = when (t) {
							is InvocationTargetException -> t.cause ?: t
							else -> t
						}
						res.end("${t2.message}")
					}
				}
			}
		}
	}
}
