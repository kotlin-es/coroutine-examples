package es.kotlin.async.utils

import es.kotlin.async.Promise
import java.net.URL

// @TODO: this should use asynchronous I/O instead of a thread per request
fun downloadUrlAsync(url: URL): Promise<String> {
    val deferred = Promise.Deferred<String>()
    Thread({
        try {
            deferred.resolve(String(url.openStream().readBytes(), Charsets.UTF_8))
        } catch (t:Throwable) {
            deferred.reject(t)
        }
    }).run()
    return deferred.promise
}
