package com.codecad.core

import com.codecad.core.parser.ast.primitive.Expr
import com.codecad.core.parser.ast.primitive.ParamExpr
import com.codecad.core.sketch.Constraint
import com.codecad.core.sketch.World
import kotlin.math.abs

val minErrorChange = 1e-10
val targetError = 1e-8

class Solver(val tracker: Tracker) {

    fun solve(world: World, x: List<Set<ParamExpr>>, constraints: List<Constraint>, accuracy: Double = targetError): Boolean {
        val current = mutableListOf<ParamExpr>()
        val number = x.sumOf { it.size }

        var errorTerm: Expr = world.ZERO

        for(constraint in constraints){
            val eq = constraint.equation
            errorTerm += eq
        }

        val derivatives = mutableListOf<Expr>()
        for( params in x ){
            for (param in params) {
                current.add(param)
                derivatives.add(errorTerm.derivative(param))
            }
        }

        val result = solveImpl(current, errorTerm, derivatives, accuracy)

        if(result){
            println("${current.size} instead of $number")
            return true
        }

        return false
    }

    fun solveImpl(x: List<ParamExpr>, errorTerm: Expr, derivatives: List<Expr>, accuracy: Double = targetError): Boolean{
        var error = errorTerm.evalDouble()
        if (error < accuracy) {
            println("error: $error")
            return true
        }

        val grad = DoubleArray(x.size)

        var lastError = error
        var errorChange = 1.0
        var iter = 0

        val optimizer = AdamOptimizer(x)

        val start = System.currentTimeMillis()
        while ((errorChange > minErrorChange && error > accuracy ) && iter < 20000) {
            tracker.addEntry(x, errorTerm)

            for (j in x.indices) {
                grad[j] = derivatives[j].evalDouble()
            }

            optimizer.optimize(grad)

            error = errorTerm.evalDouble()
            errorChange = abs(error - lastError)
            lastError = error
            iter++

            if(System.currentTimeMillis() - start > 1000){
                //return false
            }

        }

        println("iterations: $iter")
        println("error: $error")

        return error < accuracy
    }
}
