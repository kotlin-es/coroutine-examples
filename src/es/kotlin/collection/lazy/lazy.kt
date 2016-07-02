package es.kotlin.collection.lazy

import es.kotlin.collection.coroutine.generate

fun <T> Iterable<T>.lazyFilter(filter: (value: T) -> Boolean): Iterable<T> {
    val it = this
    return generate<T> {
        for (v in it) if (filter(v)) yield(v)
    }
}

fun <T, R> Iterable<T>.lazyMap(map: (value: T) -> R): Iterable<R> {
    val it = this
    return generate<R> {
        for (v in it) yield(map(v))
    }
}
