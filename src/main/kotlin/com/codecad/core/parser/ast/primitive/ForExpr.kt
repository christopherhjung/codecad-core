package com.codecad.core.parser.ast.primitive

import com.codecad.core.Utils
import com.codecad.core.parser.controlflow.BreakException
import com.codecad.core.parser.controlflow.ContinueException
import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class ForExpr(
    world: World,
    private val variable: Expr,
    private val range: Expr,
    private val body: Expr,
    private val label: String? = null
) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        val iterable = Utils.getIterator(range.eval(scope))
        val nestedScope = NestedScope.mutual(scope)
        while (iterable.hasNext()) {
            try {
                val value = iterable.next()
                variable.assign(nestedScope, value, true)
                body.eval(nestedScope)
            } catch (e: ContinueException) {
                if (e.label != label) {
                    throw e
                }
            } catch (e: BreakException) {
                if (e.label == label) {
                    break
                } else {
                    throw e
                }
            }
        }
        return null
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val newVariable = variable.bind(scope, true)
        val newRange = range.bind(scope, false)
        val newBody = body.bind(scope, false)
        return ForExpr(world, newVariable, newRange, newBody, label)
    }
}