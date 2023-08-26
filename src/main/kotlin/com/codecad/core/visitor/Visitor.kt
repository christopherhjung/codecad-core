package com.codecad.core.visitor

import com.codecad.core.ast.controlflow.BlockExpr
import com.codecad.core.ast.primitive.*


abstract class Visitor {
    open fun visitInfix(expr : InfixExpr) {}
    open fun visitLiteral(expr : LiteralExpr) {}
    open fun visitPrefix(expr : PrefixExpr) {}
    open fun visitParam(expr : ParamExpr) {}
    open fun visitLet(expr : LetExpr) {}
    open fun visitBlock(expr : BlockExpr) {}
    open fun visitIdent(expr : IdentExpr) {}
    open fun visitIf(expr : IfExpr) {}
    open fun visitTuple(expr : TupleExpr) {}
    open fun visitMath(expr : MathExpr) {}
    open fun visitRef(expr : RefExpr) {}

    protected open fun visit(expr : Expr) {
        when(expr){
            is LiteralExpr -> visitLiteral(expr)
            is InfixExpr -> visitInfix(expr)
            is PrefixExpr -> visitPrefix(expr)
            is ParamExpr -> visitParam(expr)
            is LetExpr -> visitLet(expr)
            is BlockExpr -> visitBlock(expr)
            is IdentExpr -> visitIdent(expr)
            is IfExpr -> visitIf(expr)
            is TupleExpr -> visitTuple(expr)
            is MathExpr -> visitMath(expr)
            is RefExpr -> visitRef(expr)
            else -> throw NotImplementedError("${expr.javaClass} not yet implemented visitor")
        }
    }
}