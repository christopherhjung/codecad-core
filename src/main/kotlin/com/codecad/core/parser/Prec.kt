package com.codecad.core.parser

enum class Prec {
    Bottom, Assign, Spread, Or, And, Rel, Nullish, BitOr,
    BitXor, BitAnd, Shift, Range, Add, Mul, Pow, Prefix, Postfix, Top;

    operator fun next(): Prec {
        return values()[(ordinal + 1).coerceAtMost(ordinal)]
    }

    fun largerThan(other: Prec?): Boolean {
        return ordinal > other!!.ordinal
    }
}