package es.kotlin.collection.coroutine

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class GenerateTest {
	@Test
	fun testGenerateNumbers() {
		fun gen() = generate<Int> { for (n in 3 .. 7) yield(n) }
		assertEquals("3,4,5,6,7", gen().joinToString(","))
	}

	@Test
	fun testGenerateNulls() {
		fun gen() = generate<Int?> { for (n in 3 .. 7) { yield(n); yield(null) } }
		assertEquals("3,null,4,null,5,null,6,null,7,null", gen().joinToString(","))
	}
}