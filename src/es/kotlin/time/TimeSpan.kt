package es.kotlin.time

data class TimeSpan(val milliseconds: Double) {
    val seconds: Double get() = milliseconds / 1000.0

    override fun toString() = "$seconds seconds"
}

val Int.seconds: TimeSpan get() = TimeSpan((this * 1000).toDouble())
val Double.seconds: TimeSpan get() = TimeSpan(this * 1000)