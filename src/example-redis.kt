import es.kotlin.async.EventLoop
import es.kotlin.db.async.redis.RedisClient
import es.kotlin.db.async.redis.RedisVfs
import es.kotlin.db.async.redis.get
import es.kotlin.db.async.redis.set

fun main(args: Array<String>) = EventLoop.mainAsync {
	val redis = RedisClient("127.0.0.1", 6379)
	redis["test123"] = "from kotlin!"
	println("done!")
	println(redis["test123"])

	val vfs = RedisVfs(redis)

	println("vfs:" + vfs["test123"].readString())
}