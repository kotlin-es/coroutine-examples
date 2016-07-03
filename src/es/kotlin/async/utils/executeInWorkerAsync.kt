package es.kotlin.async.utils

import es.kotlin.async.Promise

fun <T> executeInWorkerAsync(task: () -> T): Promise<T> {
	val deferred = Promise.Deferred<T>()
	Thread {
		try {
			val result = task()
			deferred.resolve(result)
		} catch (e: Throwable) {
			deferred.reject(e)
		}
	}.run()
	return deferred.promise
}