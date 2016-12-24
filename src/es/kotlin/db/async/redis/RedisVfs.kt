package es.kotlin.db.async.redis

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import es.kotlin.vfs.async.Vfs
import es.kotlin.vfs.async.VfsFile
import es.kotlin.vfs.async.VfsStat

fun RedisVfs(client: RedisClient): VfsFile {
	class Impl : Vfs() {
		override fun readFullyAsync(path: String): Promise<ByteArray> = async {
			("" + await(client.getAsync(path))).toByteArray(Charsets.UTF_8)
		}

		override fun writeFullyAsync(path: String, data: ByteArray): Promise<Unit> = async {
			await(client.setAsync(path, data.toString(Charsets.UTF_8)))
			Unit
		}

		override fun statAsync(path: String): Promise<VfsStat> = async {
			val exists = await(client.existsAsync(path))
			VfsStat(file = VfsFile(this@Impl, path), exists = exists, isDirectory = false, size = -1L)
		}
	}

	return Impl().root
}