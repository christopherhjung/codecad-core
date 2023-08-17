package com.codecad.core.ast.controlflow

import com.codecad.core.World
import com.codecad.core.ast.controlflow.exception.BreakException
import com.codecad.core.ast.controlflow.exception.ContinueException
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.Scope

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