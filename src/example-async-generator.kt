import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.*
import es.kotlin.async.utils.sleep
import es.kotlin.time.seconds

fun main(args: Array<String>) = EventLoop.mainAsync {
	data class User(val name: String, val age: Int)
	// Asynchronous Producer

	fun readUsers() = asyncGenerate {
		// This could read data results from disk or a socket
		for (n in 0 until 4) {
			sleep(0.3.seconds)
			yield(User(name = "test$n", age = n * 5))
		}
	}

	// Consumer
	for (user in readUsers()) {
		println(user)
	}
	println("----")
	val sumPromise = async { readUsers().map { it.age }.sum() }
	val sumGreatOrEqualThan10Promise = async { readUsers().filter { it.age >= 10 }.map { it.age }.sum() }
	println("Parallelized:")
	println("All ages summed: " + await(sumPromise))
	println("All ages (greater than 10) summed: " + await(sumGreatOrEqualThan10Promise))
}

