import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.await
import es.kotlin.db.async.redis.RedisClient
import es.kotlin.db.async.redis.RedisVfs
import es.kotlin.db.async.redis.getAsync
import es.kotlin.db.async.redis.setAsync

fun main(args: Array<String>) = EventLoop.mainAsync {
	val redis = RedisClient("127.0.0.1", 6379)
	await(redis.setAsync("test123", "from kotlin!"))
	println("done!")
	println(await(redis.getAsync("test123")))

	val vfs = RedisVfs(redis)

	println("vfs:" + await(vfs["test123"].readStringAsync()))

}