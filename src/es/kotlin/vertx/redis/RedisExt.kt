package es.kotlin.vertx.redis

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.asyncFun
import es.kotlin.async.coroutine.await
import es.kotlin.vertx.vx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.redis.RedisClient
import io.vertx.redis.op.RangeOptions

operator fun RedisClient.get(key: String) = RedisKey(this, key)

class RedisKey(val client: RedisClient, val key: String)

suspend fun RedisKey.hget(member: String): String = vx { client.hget(key, member, it) }

suspend fun RedisKey.hgetall() = asyncFun {
	val parts: JsonObject = vx { client.hgetall(key, it) }
	parts.map { it.key to it.value as String }.toMap()
}

suspend fun RedisKey.hincrby(member: String, increment: Long): Long = vx { client.hincrby(key, member, increment, it) }

suspend fun RedisKey.zaddMany(scores: Map<String, Double>): Long = vx { client.zaddMany(key, scores, it) }
suspend fun RedisKey.zadd(member: String, score: Double): Long = vx { client.zadd(key, score, member, it) }
suspend fun RedisKey.zincrby(member: String, score: Double): String = vx { client.zincrby(key, score, member, it) }
suspend fun RedisKey.zcard(): Long = vx { client.zcard(key, it) }
suspend fun RedisKey.zrevrank(member: String): Long = vx { client.zrevrank(key, member, it) }

suspend fun RedisKey.zrevrange(start: Long, stop: Long) = asyncFun {
	val result: JsonArray = vx { client.zrevrange(key, start, stop, RangeOptions.WITHSCORES, it) }
	(0 until result.size() / 2).map { result.getString(it * 2 + 0) to result.getString(it * 2 + 1).toDouble() }
}

suspend fun RedisKey.del(): Long = vx { client.del(key, it) }
