package es.kotlin.vfs.async

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.asyncGenerate
import es.kotlin.async.utils.executeInWorker
import java.io.File

fun LocalVfs(base: File): VfsFile {
	class Impl : Vfs() {
		suspend override fun readChunk(path: String, offset: Long, size: Long) = super.readChunk(path, offset, size)
		suspend override fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean) = super.writeChunk(path, data, offset, resize)
		suspend override fun setSize(path: String, size: Long): Unit = super.setSize(path, size)
		suspend override fun stat(path: String): VfsStat = super.stat(path)

		suspend override fun list(path: String) = asyncGenerate {
			val enumBase = File("${base.absolutePath}/$path").absoluteFile
			val enumBaseLength = enumBase.absolutePath.length
			val files = executeInWorker { enumBase.listFiles() ?: arrayOf() }
			for (file in files) {
				yield(VfsStat(
					file = VfsFile(this@Impl, path + "/" + file.absolutePath.substring(enumBaseLength + 1)),
					exists = file.exists(),
					isDirectory = file.isDirectory,
					size = file.length()
				))
			}
		}

		override fun toString(): String = "LocalVfs(${base.absolutePath})"
	}
	return Impl().root
}

