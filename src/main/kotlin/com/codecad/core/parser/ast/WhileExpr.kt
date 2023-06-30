package com.codecad.core.parser.ast

import com.codecad.core.parser.controlflow.BreakException
import com.codecad.core.parser.controlflow.ContinueException
import com.codecad.core.scope.Scope
import com.codecad.core.sketch.World

class WhileExpr(
    world: World,
                private val condition: Expr,
                private val body: Expr,
                private val label: String? = null
) : Expr(world) {
    override fun eval(scope: Scope): Any? {
        while (condition.evalBoolean(scope)) {
            try {
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
        val newCondition = condition.bind(scope, false)
        val newBody = body.bind(scope, false)
        return WhileExpr(world, newCondition, newBody, label)
    }
}