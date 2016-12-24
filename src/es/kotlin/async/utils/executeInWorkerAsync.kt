package es.kotlin.async.utils

import es.kotlin.async.Promise
import es.kotlin.async.coroutine.await

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

suspend fun <T> executeInWorker(task: () -> T) = executeInWorkerAsync(task).await()