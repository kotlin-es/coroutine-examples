package ext.lang

fun DynamicConvert(value: Any?, type: Class<*>): Any? {
    return when (type) {
        java.lang.Integer.TYPE -> "$value".toInt()
        java.lang.Long.TYPE -> "$value".toLong()
        java.lang.String::class.java -> "$value"
        else -> null
    }
}