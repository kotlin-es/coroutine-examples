package es.kotlin.vfs.async

import es.kotlin.async.AsyncStream
import es.kotlin.async.Promise
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await

abstract class Vfs {
	val root by lazy { VfsFile(this, "/") }

	open fun readFullyAsync(path: String): Promise<ByteArray> = async {
		val stat = statAsync(path).await()
		readChunkAsync(path, 0L, stat.size).await()
	}
	open fun writeFullyAsync(path: String, data: ByteArray): Promise<Unit> = writeChunkAsync(path, data, 0L, true)

	open fun readChunkAsync(path: String, offset: Long, size: Long): Promise<ByteArray> = throw UnsupportedOperationException()
	open fun writeChunkAsync(path: String, data: ByteArray, offset: Long, resize: Boolean): Promise<Unit> = throw UnsupportedOperationException()
	open fun setSizeAsync(path: String, size: Long): Promise<Unit> = throw UnsupportedOperationException()
	open fun statAsync(path: String): Promise<VfsStat> = throw UnsupportedOperationException()
	open fun list(path: String): AsyncStream<VfsStat> = throw UnsupportedOperationException()

	abstract class Proxy : Vfs() {
		abstract protected fun access(path: String): VfsFile
		open protected fun transformStat(stat: VfsStat): VfsStat = stat

		override fun readFullyAsync(path: String): Promise<ByteArray> = access(path).readAsync()
		override fun writeFullyAsync(path: String, data: ByteArray): Promise<Unit> = access(path).writeAsync(data)
		override fun readChunkAsync(path: String, offset: Long, size: Long): Promise<ByteArray> =  access(path).readChunkAsync(offset, size)
		override fun writeChunkAsync(path: String, data: ByteArray, offset: Long, resize: Boolean): Promise<Unit> = access(path).writeChunkAsync(data, offset, resize)
		override fun setSizeAsync(path: String, size: Long): Promise<Unit> = access(path).setSizeAsync(size)
		override fun statAsync(path: String): Promise<VfsStat> = async { transformStat(access(path).statAsync().await()) }
		override fun list(path: String): AsyncStream<VfsStat> = access(path).list().map { transformStat(it) }
	}
}