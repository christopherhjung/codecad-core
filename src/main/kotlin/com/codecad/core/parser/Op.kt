package com.codecad.core.parser

enum class Op {
    Assign, AssignAdd, AssignSub, AssignMul, AssignDiv, Eq, Ne, Lt, Le, Gt, Ge, Range, Add, Sub, Mul, Div, Not, And, Or, BitAnd, BitOr, BitXor, Dot, Chain, Inc, Dec, Nullish, Pow, Spread, LeftParen;

    fun prec(): Prec {
        return when (this) {
            Eq, Ne, Lt, Le, Gt, Ge -> Prec.Rel
            Add, Sub -> Prec.Add
            Range -> Prec.Range
            Mul, Div -> Prec.Mul
            And -> Prec.And
            Or -> Prec.Or
            Assign, AssignAdd, AssignSub, AssignMul, AssignDiv -> Prec.Assign
            BitAnd -> Prec.BitAnd
            BitOr -> Prec.BitOr
            BitXor -> Prec.BitXor
            Spread -> Prec.Spread
            Not -> Prec.Prefix
            Dec, Inc -> Prec.Postfix
            else -> Prec.Bottom
        }
    }

    val isInfix: Boolean
        get() = when (this) {
            Eq, Ne, Lt, Le, Gt, Ge, Add, Sub, Mul, Div, And, Or, Assign, AssignAdd, AssignSub, AssignMul, AssignDiv, Nullish, Chain, BitAnd, BitOr, BitXor, Pow, Dot, Range -> true
            else -> false
        }
    val isPrefix: Boolean
        get() = when (this) {
            Add, Sub, Not, Spread -> true
            else -> false
        }
    val isPostfix: Boolean
        get() = when (this) {
            LeftParen, Inc, Dec -> true
            else -> false
        }
}