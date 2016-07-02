import es.kotlin.async.EventLoop
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.generateAsync
import es.kotlin.async.sumAsync
import es.kotlin.async.utils.waitAsync
import es.kotlin.time.seconds

fun main(args: Array<String>) = EventLoop.main {
    data class User(val name:String, val age:Int)
    // Asynchronous Producer

    fun readUsers() = generateAsync<User> {
        // This could read data results from disk or a socket
        for (n in 0 until 4) {
            waitAsync(0.3.seconds).await()
            emit(User(name = "test$n", age = n * 5))
        }
    }

    async<Unit> {
        // Consumer
        readUsers().eachAsync { user ->
            println(user)
        }.await()
        println("----")
        // Consumer (eachAsync+await alias just inside async block)
        readUsers().each {
            println(it)
        }
        println("----")
        val sumPromise = readUsers().map { it.age }.sumAsync()
        val sumGreatOrEqualThan10Promise = readUsers().filter { it.age >= 10 }.map { it.age }.sumAsync()
        println("Parallelized:")
        println("All ages summed: " + sumPromise.await())
        println("All ages (greater than 10) summed: " + sumGreatOrEqualThan10Promise.await())
    }
}

