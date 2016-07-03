import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.async
import es.kotlin.db.async.RedisClient
import es.kotlin.db.async.getAsync
import es.kotlin.db.async.setAsync
import es.kotlin.net.async.AsyncSocket

fun main(args: Array<String>) = EventLoop.main {
    async<Unit> {
        val redis = RedisClient("127.0.0.1", 6379)
        redis.setAsync("test123", "from kotlin!").await()
        println("done!")
        println(redis.getAsync("test123").await())
    }
}