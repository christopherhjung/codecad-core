package com.codecad.core.rewrite

import com.codecad.core.World
import com.codecad.core.ast.controlflow.BlockExpr
import com.codecad.core.ast.primitive.*
import com.codecad.core.parser.Op
import com.codecad.core.scope.Slot

abstract class Rewriter {
    open fun rewriteInfix(expr : InfixExpr) : Expr = expr
    open fun rewriteLiteral(expr : LiteralExpr) : Expr = expr
    open fun rewritePrefix(expr : PrefixExpr) : Expr = expr
    open fun rewriteParam(expr : ParamExpr) : Expr = expr
    open fun rewriteLet(expr : LetExpr) : Expr = expr
    open fun rewriteBlock(expr : BlockExpr) : Expr = expr
    open fun rewriteIdent(expr : IdentExpr) : Expr = expr
    open fun rewriteIf(expr : IfExpr) : Expr = expr
    open fun rewriteTuple(expr : TupleExpr) : Expr = expr
    open fun rewriteMath(expr : MathExpr) : Expr = expr

    protected fun rewriteImpl(expr : Expr) : Expr{
        return when(expr){
            is LiteralExpr -> rewriteLiteral(expr)
            is InfixExpr -> rewriteInfix(expr)
            is PrefixExpr -> rewritePrefix(expr)
            is ParamExpr -> rewriteParam(expr)
            is LetExpr -> rewriteLet(expr)
            is BlockExpr -> rewriteBlock(expr)
            is IdentExpr -> rewriteIdent(expr)
            is IfExpr -> rewriteIf(expr)
            is TupleExpr -> rewriteTuple(expr)
            is MathExpr -> rewriteMath(expr)
            else -> throw NotImplementedError("$expr not yet implemented visitor")
        }
    }
}

class MultiUseScanner(){
    private val slots = HashMap<Expr, Int>()

    private fun inc(expr: Expr) : Boolean{
        return slots.merge(expr, 1){ a, b -> a + b}!! > 1
    }

    fun scan(expr : Expr) : Map<Expr, Int>{
        scanImpl(expr)
        return slots.filter { it.value > 1 }
    }

    private fun scanImpl(expr : Expr){
        if(expr is LiteralExpr || expr is ParamExpr || expr is IdentExpr){
            return
        }

        if(inc(expr)){
            return
        }

        if(expr is InfixExpr){
            scanImpl(expr.lhs)
            scanImpl(expr.rhs)
        }else if(expr is PrefixExpr){
            scanImpl(expr.expr)
        }else if(expr is MathExpr){
            scanImpl(expr.arg())
        }else if(expr is IfExpr){
            scanImpl(expr.condition)
            scanImpl(expr.trueBranch)
            expr.falseBranch?.let { scanImpl(it) }
        }else if(expr is TupleExpr){
            for(arg in expr.elems){
                scanImpl(arg)
            }
        }else{
            throw NotImplementedError("Unknown ")
        }
    }
}

class ShareRewriter(val world: World) : Rewriter(){
    data class Node(var ref: Expr, var visited : Boolean = false)
    private val nodes : MutableMap<Expr, Node> = HashMap()
    private val exprs = ArrayList<Expr>()

    fun rewrite(expr: Expr) : Expr{
        val scanner = MultiUseScanner()
        val scanExprs = scanner.scan(expr)
        if(scanExprs.isEmpty()) return expr

        nodes.clear()
        exprs.clear()
        scanExprs.forEach { nodes[it.key] = Node(world.ref(Slot(0.0))) }
        val result = rewriteImpl(expr)
        exprs.add(result)
        return BlockExpr(world, exprs.toTypedArray())
    }

    override fun rewriteInfix(expr: InfixExpr): Expr {
        val new = world.infix(rewriteImpl(expr.lhs), rewriteImpl(expr.rhs), expr.op)
        return old2new(expr, new)
    }

    override fun rewritePrefix(expr: PrefixExpr): Expr {
        val new = world.prefix(rewriteImpl(expr.expr), expr.op)
        return old2new(expr, new)
    }

    override fun rewriteIf(expr: IfExpr): Expr {
        val new = world.ifExpr(rewriteImpl(expr.condition), rewriteImpl(expr.trueBranch), expr.falseBranch?.also { rewriteImpl(it) }!!)
        return old2new(expr, new)
    }

    override fun rewriteTuple(expr: TupleExpr): Expr {
        val new = world.tuple(*expr.elems.map { rewriteImpl(it) }.toTypedArray())
        return old2new(expr, new)
    }

    override fun rewriteMath(expr: MathExpr): Expr {
        return old2new(expr, when(expr){
            is PowExpr -> world.pow(rewriteImpl(expr.base), rewriteImpl(expr.exp))
            is AbsExpr -> world.abs(rewriteImpl(expr.arg))
            is SinExpr -> world.sin(rewriteImpl(expr.arg))
            is CosExpr -> world.cos(rewriteImpl(expr.arg))
            is AsinExpr -> world.asin(rewriteImpl(expr.arg))
            is ExpExpr -> world.exp(rewriteImpl(expr.arg))
            is LogExpr -> world.log(rewriteImpl(expr.arg))
            else -> throw NotImplementedError("missing")
        })
    }

    private fun old2new(old : Expr, new : Expr) : Expr{
        val node = nodes[old]
        return if(node != null){
            val ref = node.ref
            if(!node.visited){
                exprs.add(world.infix(ref, new, Op.Assign))
                node.visited = true
            }
            ref
        }else{
            new
        }
    }
}

