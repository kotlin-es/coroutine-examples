import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.await
import es.kotlin.async.utils.downloadUrlAsync
import es.kotlin.async.utils.waitAsync
import es.kotlin.time.seconds
import java.net.URL

fun main(args: Array<String>) {
	EventLoop.main {
		async<String> {
			val secondsToWait = 3
			println("Started!")
			println("Waiting $secondsToWait seconds...")
			for (n in 0 until secondsToWait) {
				waitAsync(1.seconds).await()
				println("One second elapsed!")
			}

			println("Downloading url...")
			val result = MyNetTasks.downloadGoogleAsStringAsync().await()
			println("Downloaded")
			result
		}.then { result ->
			println(result)

			async<Unit> {
				try {
					waitAsync(3.seconds, RuntimeException("Error")).await()
					println("Unexpected!")
				} catch (t: Throwable) {
					println("Exception catched! : $t")
				} finally {
					println("Finally!")
				}

				try {
					println("Downloaded url: ${MyNetTasks.downloadUnexistantUrlAsync().await()}")
				} catch (e: Throwable) {
					println("Error downloading url: $e")
				}
			}
		}
	}
}

object MyNetTasks {
	fun downloadGoogleAsStringAsync() = async<String> {
		downloadUrlAsync(URL("http://google.com/")).await()
	}

	fun downloadUnexistantUrlAsync() = async<String> {
		downloadUrlAsync(URL("http://google.com/adasda/asdasd")).await()
	}
}
