import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.asyncFun
import es.kotlin.async.utils.sleep
import es.kotlin.di.AsyncInjector
import es.kotlin.time.seconds
import es.kotlin.vertx.redis.*
import es.kotlin.vertx.route.Param
import es.kotlin.vertx.route.Route
import es.kotlin.vertx.route.registerRouter
import es.kotlin.vertx.routedHttpServerAsync
import es.kotlin.vertx.vertx
import io.vertx.core.http.HttpMethod
import io.vertx.redis.RedisClient
import io.vertx.redis.RedisOptions

object VertxExample {
	@JvmStatic fun main(args: Array<String>) {
		val redisNode = System.getenv("REDIS_CLUSTER") ?: "127.0.0.1"

		//for ((env, value) in System.getenv()) println("ENV:$env:$value")

		println("redisNode: $redisNode")

		val injector = AsyncInjector()
			.map(vertx)
			.map(vertx.fileSystem())
			.map(RedisClient.create(vertx, RedisOptions().setHost(redisNode)))

		routedHttpServerAsync {
			async {
				println("Registering routes...")
				registerRouter<SimpleRoute>(injector)
				println("Ok")

				// Static handler!
				//route("/assets/*").handler(StaticHandler.create("assets"));
			}
		}
	}
}

class SimpleRoute(
	val redis: RedisClient
) {
	val ranking = redis["ranking"]

	@Route(HttpMethod.GET, "/")
	suspend fun root() = asyncFun {
		sleep(0.5.seconds)
		"HELLO after 0.5 seconds without blocking!"
	}

	@Route(HttpMethod.GET, "/top")
	suspend fun top() = asyncFun {
		ranking.zrevrange(0L, 10L)
	}

	@Route(HttpMethod.GET, "/incr/:user/:value")
	suspend fun incr(@Param("user") user: String, @Param("value") value: Long) = asyncFun {
		ranking.zincrby(user, value.toDouble())
	}

	@Route(HttpMethod.GET, "/rank/:user")
	suspend fun rank(@Param("user") user: String) = asyncFun {
		ranking.zrevrank(user)
	}
}