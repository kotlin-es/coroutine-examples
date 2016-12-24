import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.asyncFun
import es.kotlin.async.utils.downloadUrl
import es.kotlin.async.utils.sleep
import es.kotlin.time.seconds
import java.net.URL

fun main(args: Array<String>) {
	EventLoop.mainAsync {
		val secondsToWait = 3
		println("Started!")
		println("Waiting $secondsToWait seconds...")
		for (n in 0 until secondsToWait) {
			sleep(1.seconds)
			println("One second elapsed!")
		}

		println("Downloading url...")
		val result = MyNetTasks.downloadGoogleAsString()
		println("Downloaded")
		println(result)

		try {
			sleep(3.seconds)
			println("Unexpected!")
		} catch (t: Throwable) {
			println("Exception catched! : $t")
		} finally {
			println("Finally!")
		}

		try {
			println("Downloaded url: ${MyNetTasks.downloadUnexistantUrl()}")
		} catch (e: Throwable) {
			println("Error downloading url: $e")
		}
	}
}

object MyNetTasks {
	suspend fun downloadGoogleAsString() = asyncFun {
		downloadUrl(URL("http://google.com/"))
	}

	suspend fun downloadUnexistantUrl() = asyncFun {
		downloadUrl(URL("http://google.com/adasda/asdasd"))
	}
}
