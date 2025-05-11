import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object SHA256Util {
    fun sha256Encrypt(content: String): String {
        return try {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val digest = messageDigest.digest(content.toByteArray())
            bytesToHex(digest)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("SHA-256 algorithm not found", e)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}