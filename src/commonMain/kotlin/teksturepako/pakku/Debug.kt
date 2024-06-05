package teksturepako.pakku

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Enables debug mode.
 */
var debugMode = false

/**
 * Debug scope function; Use anywhere to add code called only in debug mode.
 */
@OptIn(ExperimentalContracts::class)
inline fun debug(block: () -> Unit)
{
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (debugMode) block()
}

/**
 * Debug scope function; Use anywhere to add code called only in debug mode.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> T.debug(block: (T) -> Unit): T
{
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (debugMode) block(this)
    return this
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Collection<T>.debugIf(
    predicate: (Collection<T>) -> Boolean, block: (Collection<T>) -> Unit
): Collection<T>
{
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (debugMode && predicate(this)) block(this)
    return this
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Collection<T>.debugIfEmpty(block: (Collection<T>) -> Unit): Collection<T>
{
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (debugMode && this.isEmpty()) block(this)
    return this
}

fun Any.toPrettyString(): String
{
    var indentLevel = 0
    val indentWidth = 4

    fun padding() = "".padStart(indentLevel * indentWidth)

    val toString = toString()
    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    while (i < toString.length)
    {
        when (val char = toString[i])
        {
            '(', '[', '{' ->
            {
                indentLevel++
                stringBuilder.appendLine(char).append(padding())
            }

            ')', ']', '}' ->
            {
                indentLevel--
                stringBuilder.appendLine().append(padding()).append(char)
            }

            ','           ->
            {
                stringBuilder.appendLine(char).append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }

            else          -> stringBuilder.append(char)
        }
        i++
    }
    return stringBuilder.toString()
}