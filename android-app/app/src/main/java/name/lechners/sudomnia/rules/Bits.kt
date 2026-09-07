package name.lechners.sudomnia.rules

/**
 * Candidate sets are 9-bit masks, not collections: bit 0 means "digit 1 is still
 * possible", bit 8 means "digit 9 is".
 *
 * This is not premature optimisation. Generating one puzzle runs the solver ~80
 * times; a `HashSet<Int>` per cell would mean six-figure allocation counts per
 * puzzle. `bitCount` and `numberOfTrailingZeros` compile to single ARM64
 * instructions (RBIT + CLZ) under ART.
 */
object Bits {

    /** All nine digits possible. */
    const val ALL = 0x1FF

    /** Mask for a single digit 1..9. */
    fun of(digit: Int): Int = 1 shl (digit - 1)

    fun contains(mask: Int, digit: Int): Boolean = (mask and of(digit)) != 0

    fun count(mask: Int): Int = Integer.bitCount(mask)

    /** The lowest digit in the mask, or 0 if the mask is empty. */
    fun lowest(mask: Int): Int =
        if (mask == 0) 0 else Integer.numberOfTrailingZeros(mask) + 1

    /** Iterates the digits in the mask in ascending order. */
    inline fun forEach(mask: Int, action: (Int) -> Unit) {
        var m = mask
        while (m != 0) {
            val bit = m and (-m)
            action(Integer.numberOfTrailingZeros(bit) + 1)
            m = m xor bit
        }
    }
}
