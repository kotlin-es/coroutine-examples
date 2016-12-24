package es.kotlin.vfs.async

import es.kotlin.async.AsyncStream
import es.kotlin.async.Promise
import es.kotlin.async.coroutine.*
import java.nio.charset.Charset
import java.util.*

// @TODO: Give feedback about this!
// @NOTE: Would be great if I could extend VfsFile inside the AsyncStreamController/Awaitable interface
// so I could provide here a 'stat' method (and the other methods) as an alias for statAsync().await() as an extension instead
// of having to provide it in the controller.

// Examples:
//suspend fun Awaitable.stat(file: VfsFile, c: Continuation<VfsStat>) { // Modifier suspend is not aplicable to top level function
//	file.statAsync().then(resolved = { c.resume(it) }, rejected = { c.resumeWithException(it) })
//}
//
//suspend fun Awaitable::VfsFile.stat(c: Continuation<VfsStat>) {
//	this.statAsync().then(resolved = { c.resume(it) }, rejected = { c.resumeWithException(it) })
//}

class VfsFile(
	val vfs: Vfs,
	path: String
) {
	val path: String = normalize(path)

	companion object {
		fun normalize(path: String): String {
			var path2 = path
			while (path2.startsWith("/")) path2 = path2.substring(1)
			val out = LinkedList<String>()
			for (part in path2.split("/")) {
				when (part) {
					"", "." -> Unit
					".." -> if (out.isNotEmpty()) out.removeLast()
					else -> out += part
				}
			}
			return out.joinToString("/")
		}

		fun combine(base: String, access: String): String = normalize(base + "/" + access)
	}

	operator fun get(path: String): VfsFile = VfsFile(vfs, combine(this.path, path))

	fun readAsync(): Promise<ByteArray> = vfs.readFullyAsync(path)
	fun writeAsync(data: ByteArray): Promise<Unit> = vfs.writeFullyAsync(path, data)

	fun readStringAsync(charset: Charset = Charsets.UTF_8): Promise<String> = async { vfs.readFullyAsync(path).await().toString(charset) }
	fun writeStringAsync(data: String, charset: Charset = Charsets.UTF_8): Promise<Unit> = async { vfs.writeFullyAsync(path, data.toByteArray(charset)).await() }

	fun readChunkAsync(offset: Long, size: Long): Promise<ByteArray> = vfs.readChunkAsync(path, offset, size)
	fun writeChunkAsync(data: ByteArray, offset: Long, resize: Boolean = false): Promise<Unit> = vfs.writeChunkAsync(path, data, offset, resize)

	fun statAsync(): Promise<VfsStat> = vfs.statAsync(path)
	fun sizeAsync(): Promise<Long> = async { vfs.statAsync(path).await().size }
	fun existsAsync(): Promise<Boolean> = async {
		try {
			vfs.statAsync(path).await().exists
		} catch (e: Throwable) {
			false
		}
	}

	fun setSizeAsync(size: Long): Promise<Unit> = vfs.setSizeAsync(path, size)

	fun jail(): VfsFile = JailVfs(this).root

	fun list(): AsyncStream<VfsStat> = vfs.list(path)

	//fun listRecursive(): AsyncStream<VfsStat> = generateAsync {
	// @TODO: Report ERROR: java.lang.IllegalAccessError: tried to access field es.kotlin.vfs.async.VfsFile$listRecursive$1.controller from class es.kotlin.vfs.async.VfsFile$listRecursive$1$1
	// @TODO: This was a runtime error, if not supported this should be a compile-time error
	//
	//	this@VfsFile.list().eachAsync {
	//		emit(it)
	//	}
	//}

	fun listRecursive(): AsyncStream<VfsStat> = generateAsync {
		// @TODO: This is not lazy at all! (at least per directory). Find a way to flatMap lazily this
		val files = this@VfsFile.list().toListAsync().await()
		for (file in files) {
			emit(file)
			if (file.isDirectory) {
				for (file in file.file.listRecursive().toListAsync().await()) {
					emit(file)
				}
			}
		}
	}

	override fun toString(): String = "VfsFile($vfs, $path)"
}