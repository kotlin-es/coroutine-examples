package es.kotlin.vfs.async

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.asyncGenerate
import es.kotlin.async.utils.executeInWorker
import java.io.File

fun LocalVfs(base: File): VfsFile {
	class Impl : Vfs() {
		override fun readChunkAsync(path: String, offset: Long, size: Long): Promise<ByteArray> {
			return super.readChunkAsync(path, offset, size)
		}

		override fun writeChunkAsync(path: String, data: ByteArray, offset: Long, resize: Boolean): Promise<Unit> {
			return super.writeChunkAsync(path, data, offset, resize)
		}

		override fun setSizeAsync(path: String, size: Long): Promise<Unit> {
			return super.setSizeAsync(path, size)
		}

		override fun statAsync(path: String): Promise<VfsStat> {
			return super.statAsync(path)
		}

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

