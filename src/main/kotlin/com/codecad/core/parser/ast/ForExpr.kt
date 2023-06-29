package com.codecad.core.parser.ast

import com.codecad.core.Utils
import com.codecad.core.parser.controlflow.BreakException
import com.codecad.core.parser.controlflow.ContinueException
import com.codecad.core.scope.*

class ForExpr @JvmOverloads constructor(
    private val variable: Expr,
    private val range: Expr,
    private val body: Expr,
    private val label: String? = null
) : Expr {
    override fun eval(scope: Scope): Any? {
        var scope = scope
        val iterable = Utils.getIterator(range.eval(scope))
        scope = NestedScope.mutual(scope)
        while (iterable.hasNext()) {
            try {
                val value = iterable.next()!!
                variable.assign(scope, value, true)
                body.eval(scope)
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
        return ForExpr(newVariable, newRange, newBody, label)
    }
}