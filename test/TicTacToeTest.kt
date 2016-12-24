import es.kotlin.async.coroutine.async
import es.kotlin.async.coroutine.sync
import es.kotlin.async.utils.sleep
import es.kotlin.net.async.AsyncClient
import es.kotlin.time.seconds
import org.junit.Test

class TicTacToeTest {
	@Test
	fun name() {
		sync {
			async { TicTacToe(0.1.seconds).server() }
			async { AsyncClient("127.0.0.1", 9090) }
			async { AsyncClient("127.0.0.1", 9090) }
			sleep(10.seconds)
		}
	}
}