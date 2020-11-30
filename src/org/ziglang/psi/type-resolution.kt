package org.ziglang.psi

/** Something that can be resolved to a Zig type. */
interface ZigTypeResolvable {
	fun resolveToType():Any? = null
}

/** Degenerate [ZigTypeResolvable] that does not resolve into anything. */
val NULL_ZIG_TYPE_RESOLVABLE:ZigTypeResolvable = object : ZigTypeResolvable {}