import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.each
import es.kotlin.vfs.async.LocalVfs
import java.io.File

fun main(args: Array<String>) = EventLoop.mainAsync {
	val vfs = LocalVfs(File("."))

	vfs.listRecursive().each {
		println(it)
	}
}