package teksturepako.data

fun <T> Collection<T>.allEqual(): Boolean = this.all { it == first() }
fun <T, V> Collection<T>.allEqualTo(value: V): Boolean = this.all { it == value }

fun <T> Collection<T>.allNotEqual(): Boolean = this.any { it != first() }
fun <T, V> Collection<T>.allNotEqualTo(value: V): Boolean = this.any { it != value }

fun <T> Collection<T>.allEmpty(): Boolean = this.all { isEmpty() }

fun <K, V> Map<K, V>.keyValuesMatch(key: K) = this.filterKeys { key in this.keys }.values.allEqual()