import java.lang.Math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val pertMag = 1e-6
val pertMin = 1e-10
val smallF = 1e-20
val validSolutionFine = 1e-40
val validSoltuionRough = 1e-4
val maxIterations = 50

fun calc(constraints: List<Constraint>): Double {
    var error = 0.0
    for (constraint in constraints) {
        error += constraint.error()
    }
    return error
}

class Solver {

    fun stepGrad(x: List<Value>, xold: DoubleArray, grad: DoubleArray, alpha: Double) {
        for (i in x.indices) {
            x[i].value = xold[i] + alpha * -grad[i]//calculate the new x
        }
    }

    fun calcAlpha(f1: Double, xold: DoubleArray, grad: DoubleArray, x: List<Value>, cons: List<Constraint>): Double {
        val alpha1 = 0.0
        var alpha2 = 0.00001
        stepGrad(x, xold, grad, alpha2)
        var f2 = calc(cons)

        var alpha3 = 2 * alpha2
        stepGrad(x, xold, grad, alpha3)
        var f3 = calc(cons)

        //Now reduce or lengthen alpha2 and alpha3 until the minimum is
        //Bracketed by the triplet f1>f2<f3
        while (f2 > f1 || f2 > f3) {
            if (f2 > f1) {
                //If f2 is greater than f1 then we shorten alpha2 and alpha3 closer to f1
                //Effectively both are shortened by a factor of two.
                alpha3 = alpha2
                f3 = f2
                alpha2 /= 2
                stepGrad(x, xold, grad, alpha2)
                f2 = calc(cons)
            } else {
                //If f2 is greater than f3 then we length alpah2 and alpha3 closer to f1
                //Effectively both are lengthened by a factor of two.
                alpha2 = alpha3
                f2 = f3
                alpha3 *= 2
                stepGrad(x, xold, grad, alpha3)
                f3 = calc(cons)
            }
        }

        val denominator = (3 * (f1 - 2 * f2 + f3))
        var alphaStar: Double
        if (denominator == 0.0) {
            //throw RuntimeException("divide by 0")
            alphaStar = 0.001
        } else {
            // get the alpha for the minimum f of the quadratic approximation
            alphaStar = alpha2 + ((alpha2 - alpha1) * (f1 - f3)) / denominator

            //Guarantee that the new alphaStar is within the bracket
            if (alphaStar > 0.01 || alphaStar < 0.0) {
                alphaStar = 0.1
            }
        }

        return alphaStar
    }

    fun calcGrad(currentError: Double, grad: DoubleArray, x: List<Value>, cons: List<Constraint>) {
        var pert = currentError * pertMag
        if (pert < pertMin) pert = pertMin
        for (j in x.indices) {
            val temp = x[j].value
            x[j].value = temp + pert
            var nextError = calc(cons)

            if (nextError < currentError) {
                grad[j] =  (nextError - currentError) / pert
            } else {
                x[j].value = temp - pert
                nextError = calc(cons)
                if (nextError < currentError) {
                    grad[j] = (currentError - nextError) / pert
                } else {
                    grad[j] = 0.0
                }
            }

            x[j].value = temp
        }
    }

    fun copyInto(target: DoubleArray, x: List<Value>) {
        for (i in x.indices) {
            target[i] = x[i].value
        }
    }

    val alpha = 0.001
    val beta1 = 0.9
    val beta2 = 0.999
    val epsilon = 10e-8

    var m = 0.0
    var v = 0.0
    var t = 0

    fun solve(x: List<Value>, cons: List<Constraint>, isFine: Boolean): Boolean {
        val original = DoubleArray(x.size)
        copyInto(original, x)

        var error = calc(cons)
        if (error < smallF) {
            return true
        }

        val xold = DoubleArray(x.size)
        val grad = DoubleArray(x.size)

        var lastError = error
        var errorChange = 1.0
        var iter = 0
        while (errorChange > smallF) {
            calcGrad(error, grad, x, cons)

            copyInto(xold, x)

            t++
            for(i in x.indices){
                m = beta1 * m + ( 1 - beta1 ) * grad[i]
                v = beta2 * v + ( 1 - beta2 ) * grad[i] * grad[i]
                val mHat = m / (1 - beta1.pow(t))
                val vHat = v / (1 - beta2.pow(t))
                x[i].value = xold[i] - alpha * mHat / ( sqrt(vHat) + epsilon )
            }

            //val alphaStar = calcAlpha(error, xold, grad, x, cons)

            //stepGrad(x, xold, grad, alphaStar)

            error = calc(cons)
            errorChange = abs(error - lastError)
            lastError = error
            iter++
        }
/*
        errorChange = 1.0
        iter = 0
        while (iter < 1000000) {
            calcGrad(error, grad, x, cons)

            copyInto(xold, x)

            val alphaStar = calcAlpha(error, xold, grad, x, cons)

            stepGrad(x, xold, grad, alphaStar)

            error = calc(cons)
            iter++
        }*/

        println(iter)

        val validSolution = if (isFine) validSolutionFine else validSoltuionRough
        return if (error < validSolution) {
            true
        } else {
            for (i in x.indices) {
                x[i].value = original[i]
            }
            false
        }

    }
}
