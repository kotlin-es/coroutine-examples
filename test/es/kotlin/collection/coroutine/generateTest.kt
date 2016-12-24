package es.kotlin.collection.coroutine

import es.kotlin.async.EventLoop
import es.kotlin.async.utils.sleep
import es.kotlin.time.seconds
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerateTest {
	@Test
	fun testGenerateNumbers() {
		fun gen() = generate { for (n in 3..7) yield(n) }
		assertEquals("3,4,5,6,7", gen().joinToString(","))
	}

	@Test
	fun testGenerateNulls() {
		fun gen() = generate {
			for (n in 3..7) {
				yield(n);
				yield(null)
			}
		}
		assertEquals("3,null,4,null,5,null,6,null,7,null", gen().joinToString(","))
	}

	@Test
	fun testAsyncAwait1() {
		var out = ""
		EventLoop.mainAsync {
			out += "a"
			sleep(0.1.seconds)
			out += "b"
		}
		Assert.assertEquals("ab", out)
	}

	@Test
	fun testAsyncAwait2() {
		var out = ""
		EventLoop.mainAsync {
			out += "a"
			sleep(0.1.seconds)
			out += "b"
			Unit
		}
		Assert.assertEquals("ab", out)
	}
}