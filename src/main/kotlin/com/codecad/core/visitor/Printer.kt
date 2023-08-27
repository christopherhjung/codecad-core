package com.codecad.core.visitor

import com.codecad.core.ast.controlflow.BlockExpr
import com.codecad.core.ast.primitive.*
import com.codecad.core.scope.Slot


class Printer() : Visitor(){
    var indent = 0
    val sb = StringBuilder()

    private fun indent(){
        sb.append("    ".repeat(indent))
    }

    private fun nl(){
        sb.append("\n").append("    ".repeat(indent))
    }

    val paramNames = hashMapOf<ParamExpr, String>()
    var paramIdx = 0

    private fun paramName( expr: ParamExpr ) : String{
        return paramNames.computeIfAbsent(expr){
            "param" + paramIdx++
        }
    }

    val slotNames = hashMapOf<Slot, String>()
    var slotIdx = 0

    private fun refName( slot: Slot ) : String{
        return slotNames.computeIfAbsent(slot){
            "slot" + slotIdx++
        }
    }

    fun print(expr: Expr) : String{
        visit(expr)
        return sb.toString()
    }

    override fun visitLiteral(expr: LiteralExpr) {
        sb.append(expr.value)
    }

    override fun visitInfix(expr: InfixExpr){
        sb.append("(")
        visit(expr.lhs)
        sb.append(" ").append(expr.op.sign).append(" ")
        visit(expr.rhs)
        sb.append(")")
    }

    override fun visitPrefix(expr: PrefixExpr){
        sb.append(expr.op.sign)
        visit(expr.expr)
    }

    override fun visitIf(expr: IfExpr) {
        sb.append("if ")
        visit(expr.condition)
        sb.append(" ")
        visit(expr.trueBranch)
        if(expr.falseBranch != null){
            sb.append("else ")
        }else{
            nl()
        }
    }

    override fun visitBlock(expr: BlockExpr) {
        sb.append("{")
        indent++
        for( item in expr.exprs ){
            nl()
            visit(item)
        }

        indent--;
        sb.append("}")
    }

    override fun visitTuple(expr: TupleExpr){
        sb.append("(")
        var first = true
        for( elem in expr.elems ){
            if(!first){
                sb.append(", ")
            }
            first = false
            visit(elem)
        }
        sb.append(")")
    }

    override fun visitMath(expr: MathExpr){
        val name = when(expr){
            is AbsExpr -> "abs"
            is PowExpr -> "pow"
            is SinExpr -> "sin"
            is CosExpr -> "cos"
            is ASinExpr -> "asin"
            is ACosExpr -> "acos"
            else -> throw Error("xx")
        }

        sb.append(name).append("(")
        visit(expr.arg())
        sb.append(")")
    }

    override fun visitParam(expr: ParamExpr) {
        sb.append(paramName(expr))
    }

    override fun visitRef(expr: RefExpr) {
        sb.append(refName(expr.slot))
    }
}