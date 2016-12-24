package es.kotlin.async.utils

import java.net.URL

suspend fun downloadUrl(url: URL) = executeInWorker { String(url.openStream().readBytes(), Charsets.UTF_8) }