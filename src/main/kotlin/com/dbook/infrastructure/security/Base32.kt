package com.dbook.infrastructure.security

/** RFC 4648 Base32 without padding: the alphabet authenticator apps read a secret in. */
internal object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val BITS_PER_CHAR = 5
    private const val BITS_PER_BYTE = 8
    private const val CHAR_MASK = 0b11111
    private const val BYTE_MASK = 0xff

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl BITS_PER_BYTE) or (byte.toInt() and BYTE_MASK)
            bits += BITS_PER_BYTE
            while (bits >= BITS_PER_CHAR) {
                out.append(ALPHABET[(buffer shr (bits - BITS_PER_CHAR)) and CHAR_MASK])
                bits -= BITS_PER_CHAR
            }
        }
        if (bits > 0) {
            out.append(ALPHABET[(buffer shl (BITS_PER_CHAR - bits)) and CHAR_MASK])
        }
        return out.toString()
    }
}
