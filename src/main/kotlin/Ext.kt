
fun UInt.getBits(start: Int, end: Int): UInt {
    return this shl (31 - end) shr (start + 31 - end)
}

operator fun <T> Array<T>.get(index: UInt) = this[index.toInt()]

operator fun <T> Array<T>.set(index: UInt, value: T) = this.set(index.toInt(), value)

operator fun UIntArray.get(index: UInt) = this[index.toInt()]

operator fun UIntArray.set(index: UInt, value: UInt) = this.set(index.toInt(), value)