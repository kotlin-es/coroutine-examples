package es.kotlin.crypto

import java.security.MessageDigest

class Hash(private val digest: java.security.MessageDigest) {
	companion object {
		val SHA1 = Hash(MessageDigest.getInstance("SHA1"))
		val MD5 = Hash(MessageDigest.getInstance("MD5"))
	}

	fun hash(data: ByteArray): ByteArray {
		digest.reset()
		return digest.digest(data)
	}
}