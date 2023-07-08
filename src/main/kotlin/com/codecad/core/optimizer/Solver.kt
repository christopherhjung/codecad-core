package com.codecad.core.optimizer

import com.codecad.core.Tracker
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.constraint.Constraint
import com.codecad.core.World
import com.codecad.core.ast.primitive.evalDoubleArray
import com.codecad.core.rewrite.ShareRewriter
import com.codecad.core.visitor.Printer
import kotlin.math.abs

val minErrorChange = 1e-12
val targetError = 1e-10

class Solver(val tracker: Tracker) {

    fun solve(world: World, params: List<ParamExpr>, constraints: Collection<Constraint>, accuracy: Double = targetError): Boolean {
        val errorTerm = constraints.map { it.equation }
            .reduceOrNull{a,b -> a + b} ?: return true
        val gradients = params.map { errorTerm.derivative(it) }
        var tangent = world.tuple(*gradients.toTypedArray(), errorTerm)

        val rewriter = ShareRewriter(world)
        tangent = rewriter.rewrite(tangent)
        return solveImpl(params, tangent, accuracy)
    }

    fun solveImpl(x: List<ParamExpr>, tangent: Expr, accuracy: Double = targetError): Boolean{
        var error = Double.MAX_VALUE
        var lastError = Double.MAX_VALUE
        var errorChange = 1.0
        var iter = 0

        val optimizer = AdamOptimizer(x)
        while ((errorChange > minErrorChange && error > accuracy ) && iter < 20000) {
            val tangentValues = tangent.evalDoubleArray()
            error = tangentValues.last()
            optimizer.optimize(tangentValues)

            errorChange = abs(error - lastError)
            lastError = error
            iter++
        }

        println("iterations: $iter")
        println("error: $error")
        return error < accuracy
    }
}
