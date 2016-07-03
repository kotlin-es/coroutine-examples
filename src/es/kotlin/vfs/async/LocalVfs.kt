package es.kotlin.vfs.async

import es.kotlin.async.AsyncStream
import es.kotlin.async.Promise
import es.kotlin.async.coroutine.generateAsync
import es.kotlin.async.utils.executeInWorkerAsync
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

		override fun list(path: String): AsyncStream<VfsStat> = generateAsync {
			val enumBase = File("${base.absolutePath}/$path").absoluteFile
			val enumBaseLength = enumBase.absolutePath.length
			val files = executeInWorkerAsync { enumBase.listFiles() ?: arrayOf() }.await()
			for (file in files) {
				emit(VfsStat(
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

