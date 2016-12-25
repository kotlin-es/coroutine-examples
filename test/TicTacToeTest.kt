import es.kotlin.async.coroutine.AsyncIterator
import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.asyncGenerate
import es.kotlin.async.coroutine.sync
import es.kotlin.async.utils.sleep
import es.kotlin.collection.coroutine.generate
import es.kotlin.net.async.AsyncClient
import es.kotlin.time.seconds
import org.junit.Test

class TicTacToeTest {
	@Test
	fun name() {
		sync {
			val port = 9091
			async { TicTacToe(moveTimeout = 0.1.seconds, port = port).server() }
			async { AsyncClient("127.0.0.1", port) }
			async { AsyncClient("127.0.0.1", port) }
			sleep(10.seconds)
		}
	}

	/*
	class X {
		fun s(): AsyncIterator<Int> = asyncGenerate<Int> { t().next() }.iterator()
		fun t(): AsyncIterator<Int> = asyncGenerate<Int> { s().next() }.iterator()
	}

	@Test
	fun asyncGenerateBug() {
		sync { X().s().next() }
	}
	*/

	//class X {
	//	fun s(): Iterator<Int> = generate<Int> { t().next() }.iterator()
	//	fun t(): Iterator<Int> = generate<Int> { s().next() }.iterator()
	//}
//
	//@Test
	//fun syncGenerateBug() {
	//	sync { X().s().next() }
	//}
}