package teksturepako.pakku.io

fun ByteArray.toMurmur2(): Long
{
    val bytes = this.filterWhitespaceCharacter()
    return Murmur2.hash(bytes, bytes.size, 1)
}

private fun ByteArray.filterWhitespaceCharacter(): ByteArray
{
    val newArray = mutableListOf<Byte>()

    for (byte in this) {
        when (byte) {
            9.toByte(), 10.toByte(), 13.toByte(), 32.toByte() -> {}
            else -> {
                newArray.add(byte)
            }
        }
    }

    return newArray.toByteArray()
}

object Murmur2
{
    /**
     * Helps convert a byte into its unsigned value
     */
    private const val UNSIGNED_MASK: Long = 0xff

    /**
     * Helps convert integer to its unsigned value
     */
    private const val UINT_MASK: Long = 0xFFFFFFFFL

    /**
     * Compute the Murmur2 hash.
     *
     * @param data
     * the data that needs to be hashed
     *
     * @param length
     * the length of the data that needs to be hashed
     *
     * @param seed
     * the seed to use to compute the hash
     *
     * @return the computed hash value
     */
    fun hash(data: ByteArray, length: Int, seed: Long): Long {
        val m = 0x5bd1e995L
        val r = 24

        // Initialize the hash to a 'random' value
        var hash = seed xor length.toLong() and UINT_MASK

        // Mix 4 bytes at a time into the hash
        val length4 = length.ushr(2)

        for (i in 0..<length4) {
            val i4 = i shl 2

            var k = data[i4].toLong() and UNSIGNED_MASK
            k = k or ((data[i4 + 1].toLong() and UNSIGNED_MASK) shl 8)
            k = k or ((data[i4 + 2].toLong() and UNSIGNED_MASK) shl 16)
            k = k or ((data[i4 + 3].toLong() and UNSIGNED_MASK) shl 24)

            k = k * m and UINT_MASK
            k = k xor (k.ushr(r) and UINT_MASK)
            k = k * m and UINT_MASK

            hash = hash * m and UINT_MASK
            hash = hash xor k and UINT_MASK
        }

        // Handle the last few bytes of the input array
        val offset = length4 shl 2
        when (length and 3) {
            3 -> {
                hash = hash xor (data[offset + 2].toLong() shl 16 and UINT_MASK)
                hash = hash xor (data[offset + 1].toLong() shl 8 and UINT_MASK)
                hash = hash xor (data[offset].toLong() and UINT_MASK)
                hash = hash * m and UINT_MASK
            }

            2 -> {
                hash = hash xor (data[offset + 1].toLong() shl 8 and UINT_MASK)
                hash = hash xor (data[offset].toLong() and UINT_MASK)
                hash = hash * m and UINT_MASK
            }

            1 -> {
                hash = hash xor (data[offset].toLong() and UINT_MASK)
                hash = hash * m and UINT_MASK
            }
        }

        hash = hash xor (hash.ushr(13) and UINT_MASK)
        hash = hash * m and UINT_MASK
        hash = hash xor hash.ushr(15)

        return hash
    }
}