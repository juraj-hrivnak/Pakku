package teksturepako.data

fun <T> Collection<T>.allEqual(): Boolean = this.all { it == first() }
fun <T> Collection<T>.allNotEqual(): Boolean = this.any { it != first() }
fun <T> Collection<T>.allEmpty(): Boolean = this.all { isEmpty() }

fun <K, V> Map<K, V>.keyValuesMatch(key: K) = this.filterKeys { key in this.keys }.values.allEqual()