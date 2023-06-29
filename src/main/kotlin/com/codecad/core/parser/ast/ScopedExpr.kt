package com.codecad.core.parser.ast

import com.codecad.core.scope.MutualScope
import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope

class ScopedExpr(private val scope: Scope, private val expr: Expr) : Expr {
    override fun eval(scope: Scope): Any? {
        val nestedScope: Scope = NestedScope.nest(scope, this.scope)
        return expr.eval(nestedScope)
    }

    override fun call(scope: Scope, args: Array<Any?>): Any? {
        val nestedScope: Scope = NestedScope.nest(scope, this.scope)
        return expr.call(nestedScope, args)
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        var scope = scope
        scope = NestedScope.nest(scope, this.scope)
        scope = NestedScope.readonly(scope)
        val collector = MutualScope()
        scope = NestedScope.nest(scope, collector)
        for (value in this.scope.values()) {
            val content = value.value
            if (content is FunctionExpr) {
                content.bindFunction(scope)
            }
        }
        for (value in this.scope.values()) {
            val content = value.value
            if (content is FunctionExpr) {
                content.bind(scope, false)
            }
        }
        val newScope = collector.toStatic()
        val newExpr = expr.bind(newScope, define)
        return ScopedExpr(newScope, newExpr)
    }
}