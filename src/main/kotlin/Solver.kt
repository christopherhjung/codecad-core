import java.lang.Math.abs

val pertMag = 1e-6
val pertMin = 1e-10
val smallF = 1e-18
val validSolutionFine = 1e-12
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

    fun calcAlpha(f1: Double, xold: DoubleArray, grad: DoubleArray, x: List<Value>, cons: List<Constraint>) : Double{
        val alpha1 = 0.0
        //Take a step of alpha=1 as alpha2
        var alpha2 = 1.0
        for (i in x.indices) {
            x[i].value = xold[i] + alpha2 * -grad[i]//calculate the new x
        }
        var f2 = calc(cons)

        //Take a step of alpha 3 that is 2*alpha2
        var alpha3 = 2.0
        for (i in x.indices) {
            x[i].value = xold[i] + alpha3 * -grad[i]//calculate the new x
        }
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
                for (i in x.indices) {
                    x[i].value = xold[i] + alpha2 * -grad[i]//calculate the new x
                }
                f2 = calc(cons)
            } else {
                //If f2 is greater than f3 then we length alpah2 and alpha3 closer to f1
                //Effectively both are lengthened by a factor of two.
                alpha2 = alpha3
                f2 = f3
                alpha3 *= 2
                for (i in x.indices) {
                    x[i].value = xold[i] + alpha3 * -grad[i]//calculate the new x
                }
                f3 = calc(cons)
            }
        }
        // get the alpha for the minimum f of the quadratic approximation
        var alphaStar = alpha2 + ((alpha2 - alpha1) * (f1 - f3)) / (3 * (f1 - 2 * f2 + f3))

        //Guarantee that the new alphaStar is within the bracket
        if (alphaStar > alpha3 || alphaStar < alpha1) {
            alphaStar = alpha2
        }

        if (alphaStar != alphaStar) {
            alphaStar = .001//Fix nan problem
        }

        return alphaStar
    }

    fun calcGrad(f0: Double, grad: DoubleArray, x: List<Value>, cons: List<Constraint>){
        var pert = f0 * pertMag
        if (pert < pertMin) pert = pertMin
        for (j in x.indices) {
            val temp = x[j].value
            x[j].value = temp + pert
            val first = calc(cons)

            if(first < f0){
                grad[j] = (first - f0) / pert;
            }else{
                x[j].value = temp - pert
                val second = calc(cons)
                if(second < f0){
                    grad[j] = (f0 - second) / pert
                }else{
                    grad[j] = 0.0
                }
            }

            x[j].value = temp
        }
    }

    fun solve(x: List<Value>, cons: List<Constraint>, isFine: Boolean): Boolean {
        //Save the original parameters for later.
        val origSolution = DoubleArray(x.size)
        for (i in x.indices) {
            origSolution[i] = x[i].value
        }

        //Calculate Function at the starting point:
        var error = calc(cons)
        if (error < smallF){
            return true
        }

        val xold = DoubleArray(x.size) //Storage for the previous design variables
        val grad = DoubleArray(x.size) //The gradient vector (1xn)


        var lastError = error
        var errorChange = 1.0
        while (errorChange > smallF) {
            calcGrad(error,grad,x,cons)

            //copy newest values to the xold
            for (i in x.indices) {
                xold[i] = x[i].value//Copy last values to xold
            }
            //Take a step of alpha=1 as alpha2

            val alphaStar = calcAlpha(error, xold, grad, x, cons)

            /// Set the values to alphaStar
            for (i in x.indices) {
                x[i].value = xold[i] + alphaStar * -grad[i]//calculate the new x
            }

            error = calc(cons)
            errorChange = abs(error - lastError)
            lastError = error
        }

        val validSolution = if (isFine) validSolutionFine else validSoltuionRough
        return if (error < validSolution) {
            true
        } else {
            //Replace the bad numbers with the last result
            for (i in x.indices) {
                x[i].value = origSolution[i]
            }
            false
        }

    }
}
