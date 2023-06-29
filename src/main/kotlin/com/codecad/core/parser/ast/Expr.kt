package com.codecad.core.parser.ast

import com.codecad.core.Utils
import com.codecad.core.exception.InterpreterException
import com.codecad.core.scope.Scope
import java.util.function.Consumer

interface Expr {
    fun eval(scope: Scope): Any?
    fun call(scope: Scope, args: Array<Any?>): Any? {
        return call(scope, args, false)
    }

    fun call(scope: Scope, args: Array<Any?>, optional: Boolean): Any? {
        return CallExpr(this, LiteralExpr(args), optional)
            .eval(scope)
    }

    fun assign(scope: Scope, obj: Any?, define: Boolean): Any? {
        throw InterpreterException("Assign not implemented for " + javaClass.simpleName)
    }

    fun spread(scope: Scope, sink: Consumer<Any?>) {
        Utils.getIterator(eval(scope)).forEachRemaining(sink)
    }

    fun collect(scope: Scope, sink: Consumer<Any?>) {
        sink.accept(eval(scope))
    }

    fun evalBoolean(scope: Scope): Boolean {
        return true == eval(scope)
    }

    fun bind(scope: Scope, define: Boolean): Expr {
        return this
    }
}