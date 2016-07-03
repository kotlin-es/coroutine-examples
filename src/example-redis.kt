import es.kotlin.async.EventLoop
import es.kotlin.db.async.redis.RedisClient
import es.kotlin.db.async.redis.RedisVfs
import es.kotlin.db.async.redis.getAsync
import es.kotlin.db.async.redis.setAsync

fun main(args: Array<String>) = EventLoop.mainAsync {
	val redis = RedisClient("127.0.0.1", 6379)
	redis.setAsync("test123", "from kotlin!").await()
	println("done!")
	println(redis.getAsync("test123").await())

	val vfs = RedisVfs(redis)

	println("vfs:" + vfs["test123"].readStringAsync().await())

}