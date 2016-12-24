import es.kotlin.async.EventLoop
import es.kotlin.vfs.async.LocalVfs
import java.io.File

fun main(args: Array<String>) = EventLoop.mainAsync {
	val vfs = LocalVfs(File("."))

	for (it in vfs.listRecursive()) { // lazy and asynchronous!
		println(it)
	}
}