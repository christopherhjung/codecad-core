package com.codecad.core.rewrite

import com.codecad.core.ast.controlflow.BlockExpr
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.InfixExpr
import com.codecad.core.ast.primitive.LetExpr
import com.codecad.core.ast.primitive.LiteralExpr

abstract class Visitor {
    protected fun visitBlock(expr : BlockExpr) : Expr = expr
    protected fun visitInfix(expr : InfixExpr) : Expr = expr
    protected fun visitLet(expr : LetExpr) : Expr = expr
    protected fun visitLiteral(expr : LiteralExpr) : Expr = expr

    protected fun visit(expr : Expr) : Expr{
        return if(expr is LiteralExpr){
            visitLiteral(expr)
        }else if(expr is InfixExpr){
            visitInfix(expr)
        }else if(expr is LetExpr){
            visitLet(expr)
        }else if(expr is BlockExpr){
            visitBlock(expr)
        }else{
            throw NotImplementedError("$expr not yet implemented visitor")
        }
    }
}