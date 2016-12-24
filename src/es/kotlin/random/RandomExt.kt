package es.kotlin.random

import java.util.*

operator fun <T> List<T>.get(random :Random) = this[random.nextInt(this.size)]