package es.kotlin.db.async.redis

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.asyncFun
import es.kotlin.async.coroutine.await
import es.kotlin.vfs.async.Vfs
import es.kotlin.vfs.async.VfsFile
import es.kotlin.vfs.async.VfsStat

fun RedisVfs(client: RedisClient): VfsFile {
	class Impl : Vfs() {
		suspend override fun readFully(path: String) = asyncFun {
			("" + client.get(path)).toByteArray(Charsets.UTF_8)
		}

		suspend override fun writeFully(path: String, data: ByteArray): Unit = asyncFun {
			client.set(path, data.toString(Charsets.UTF_8))
			Unit
		}

		suspend override fun stat(path: String): VfsStat = asyncFun {
			val exists = client.exists(path)
			VfsStat(file = VfsFile(this@Impl, path), exists = exists, isDirectory = false, size = -1L)
		}
	}

	return Impl().root
}