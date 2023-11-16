package teksturepako

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
inline fun <T> T.debug(block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (debugMode) block(this)
    return this
}