package es.kotlin.collection.lazy

import es.kotlin.collection.coroutine.generate

fun <T> Iterable<T>.lazyFilter(filter: (value: T) -> Boolean): Iterable<T> = generate<T> { for (v in this@lazyFilter) if (filter(v)) yield(v) }
fun <T, R> Iterable<T>.lazyMap(map: (value: T) -> R): Iterable<R> = generate<R> { for (v in this@lazyMap) yield(map(v)) }
