package es.kotlin.lang

fun String.tryInt(): Int? = try {
	this.toInt()
} catch (t: Throwable) {
	null
}