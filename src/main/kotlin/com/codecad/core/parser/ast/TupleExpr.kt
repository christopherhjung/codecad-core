package com.codecad.core.parser.ast

import com.codecad.core.Utils
import com.codecad.core.exception.InterpreterException
import com.codecad.core.scope.*
import java.util.function.Consumer

class TupleExpr(private val elems: Array<Expr>) : Expr {
    override fun eval(scope: Scope): Any? {
        val list = ArrayList<Any?>()
        for (elem in elems) {
            elem.collect(scope) { e: Any? -> list.add(e) }
        }
        return list.toArray()
    }

    override fun assign(scope: Scope, obj: Any?, define: Boolean): Any? {
        val iter = Utils.getIterator(obj)
        for (elem in elems) {
            if (iter.hasNext()) {
                elem.assign(scope, iter.next(), true)
            } else {
                throw InterpreterException("Expected " + elems.size)
            }
        }
        if (iter.hasNext()) {
            throw InterpreterException("Too many arguments to spread")
        }
        return null
    }

    override fun spread(scope: Scope, sink: Consumer<Any?>) {
        for (elem in elems) {
            elem.collect(scope, sink)
        }
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newElems = Array(elems.size){
            elems[it].bind(scope, define)
        }
        return TupleExpr(newElems)
    }

    companion object {
        fun asTuple(expr: Expr): TupleExpr {
            return if (expr is TupleExpr) {
                expr
            } else {
                TupleExpr(arrayOf(expr))
            }
        }
    }
}