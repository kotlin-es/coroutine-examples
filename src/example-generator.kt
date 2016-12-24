import es.kotlin.collection.coroutine.generate
import es.kotlin.collection.lazy.lazyFilter
import es.kotlin.collection.lazy.lazyMap

fun main(args: Array<String>) {
	//val infiniteList = generate<Int> { for (n in 0 .. 3) yield(n) }
	val infiniteList = generate {
		var n = 0
		while (true) yield(n++)
	}

	for (i in infiniteList.lazyFilter { it % 2 == 0 }.lazyMap { -it }) {
		//for (i in infiniteList) {
		println(i)
		if (i < -10000000) break
	}
}

