import kotlin.math.sqrt


val pertMag = 1e-6
val pertMin = 1e-10
val XconvergenceRough = 1e-8
val XconvergenceFine = 1e-10
val smallF = 1e-20
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

    fun calcAlpha(f1: Double, xold: DoubleArray, s: DoubleArray, x: List<Value>, cons: List<Constraint>) : Double{

        var alpha1 = 0.0
        //Take a step of alpha=1 as alpha2
        var alpha2 = 1.0
        for (i in x.indices) {
            x[i].value = xold[i] + alpha2 * s[i]//calculate the new x
        }
        var f2 = calc(cons)

        //Take a step of alpha 3 that is 2*alpha2
        var alpha3 = 2.0
        for (i in x.indices) {
            x[i].value = xold[i] + alpha3 * s[i]//calculate the new x
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
                    x[i].value = xold[i] + alpha2 * s[i]//calculate the new x
                }
                f2 = calc(cons)
            } else if (f2 > f3) {
                //If f2 is greater than f3 then we length alpah2 and alpha3 closer to f1
                //Effectively both are lengthened by a factor of two.
                alpha2 = alpha3
                f2 = f3
                alpha3 *= 2
                for (i in x.indices) {
                    x[i].value = xold[i] + alpha3 * s[i]//calculate the new x
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

    fun solve(x: List<Value>, cons: List<Constraint>, isFine: Boolean): Boolean {
        val convergence: Double
        //Save the original parameters for later.
        val origSolution = DoubleArray(x.size)
        for (i in x.indices) {
            origSolution[i] = x[i].value
        }

        if (isFine) convergence = XconvergenceFine
        else convergence = XconvergenceRough
        //integer to keep track of how many times calc is called
        var ftimes = 0
        //Calculate Function at the starting point:
        var f0 = calc(cons)
        if (f0 < smallF) return true

        //Calculate the gradient at the starting point:

        //Calculate the gradient
        //gradF=x
        val grad = DoubleArray(x.size) //The gradient vector (1xn)
        var norm = 0.0
        var pert = f0 * pertMag
        for (j in x.indices) {
            val temper = x[j].value
            x[j].value = temper - pert
            val first = calc(cons)
            x[j].value = temper + pert
            val second = calc(cons)
            grad[j] = .5 * (second - first) / pert

            x[j].value = temper
            norm += (grad[j] * grad[j])
        }
        norm = sqrt(norm)
        //Estimate the norm of N

        //Initialize N and calculate s
        val s = DoubleArray(x.size) //The current search direction
        val N = Array(x.size) { DoubleArray(x.size) }

        for (i in x.indices) {
            for (j in x.indices) {
                if (i == j) {
                    N[i][j] = 1.0
                    s[i] = -grad[i] //Calculate the initial search vector

                } else N[i][j] = 0.0
            }
        }


        val xold = DoubleArray(x.size) //Storage for the previous design variables
        for (i in x.indices) {
            xold[i] = x[i].value//Copy last values to xold
        }

        ///////////////////////////////////////////////////////
        /// Start of line search
        ///////////////////////////////////////////////////////

        //Make the initial position alpha1
        val alphaStar = calcAlpha(f0, xold, s, x, cons)

        /// Set the values to alphaStar
        for (i in x.indices) {
            x[i].value = xold[i] + alphaStar * s[i]//calculate the new x
        }
        var fnew = calc(cons)

        var fold = fnew


        /////////////////////////////////////
        ///end of line search
        /////////////////////////////////////


        val deltaX = DoubleArray(x.size)
        val gradnew = DoubleArray(x.size)
        val gamma = DoubleArray(x.size)
        val gammatDotN = DoubleArray(x.size)
        var gammatDotNDotGamma = 0.0
        var firstTerm = 0.0
        val firstSecond = Array(x.size) { DoubleArray(x.size) }
        val deltaXDotGammatDotN = Array(x.size) { DoubleArray(x.size) }
        val gammatDotDeltaXt = Array(x.size) { DoubleArray(x.size) }
        val nDotGammaDotDeltaXt = Array(x.size) { DoubleArray(x.size) }

        var deltaXnorm = 1.0

        var iterations = 1

        ///Calculate deltaX
        for (i in x.indices) {
            deltaX[i] = x[i].value - xold[i]//Calculate the difference in x for the Hessian update
        }

        var maxIterNumber = maxIterations * x.size
        while (deltaXnorm > convergence && fnew > smallF && iterations < maxIterNumber) {
            //////////////////////////////////////////////////////////////////////
            ///Start of main loop!!!!
            //////////////////////////////////////////////////////////////////////
            var bottom = 0.0
            var deltaXtDotGamma = 0.0
            var pert = fnew * pertMag
            if (pert < pertMin) pert = pertMin
            for (i in x.indices) {
                //Calculate the new gradient vector

                val temper = x[i].value
                x[i].value = temper - pert
                val first = calc(cons)
                x[i].value = temper + pert
                val second = calc(cons)
                gradnew[i] = .5 * (second - first) / pert

                x[i].value = temper


                //Calculate the change in the gradient
                gamma[i] = gradnew[i] - grad[i]
                bottom += deltaX[i] * gamma[i]

                deltaXtDotGamma += deltaX[i] * gamma[i]

            }

            //make sure that bottom is never 0
            if (bottom == 0.0) bottom = .0000000001

            //calculate all (1xn).(nxn)

            for (i in x.indices) {
                gammatDotN[i] = 0.0
                for (j in x.indices) {
                    gammatDotN[i] += gamma[j] * N[i][j]//This is gammatDotN transpose
                }

            }
            //calculate all (1xn).(nx1)

            gammatDotNDotGamma = 0.0
            for (i in x.indices) {
                gammatDotNDotGamma += gammatDotN[i] * gamma[i]
            }

            //Calculate the first term

            firstTerm = 1 + gammatDotNDotGamma / bottom

            //Calculate all (nx1).(1xn) matrices
            for (i in x.indices) {
                for (j in x.indices) {
                    firstSecond[i][j] = ((deltaX[j] * deltaX[i]) / bottom) * firstTerm
                    deltaXDotGammatDotN[i][j] = deltaX[i] * gammatDotN[j]
                    gammatDotDeltaXt[i][j] = gamma[i] * deltaX[j]
                }
            }

            //Calculate all (nxn).(nxn) matrices
            for (i in x.indices) {
                for (j in x.indices) {
                    nDotGammaDotDeltaXt[i][j] = 0.0
                    for (k in x.indices) {
                        nDotGammaDotDeltaXt[i][j] += N[i][k] * gammatDotDeltaXt[k][j]
                    }
                }
            }
            //Now calculate the BFGS update on N
            for (i in x.indices) {
                for (j in x.indices) {
                    N[i][j] =
                        N[i][j] + firstSecond[i][j] - (deltaXDotGammatDotN[i][j] + nDotGammaDotDeltaXt[i][j]) / bottom
                }
            }

            //Calculate s
            for (i in x.indices) {
                s[i] = 0.0
                for (j in x.indices) {
                    s[i] += -N[i][j] * gradnew[j]
                }
            }

            //copy newest values to the xold
            for (i in x.indices) {
                xold[i] = x[i].value//Copy last values to xold
            }
            //Take a step of alpha=1 as alpha2

            val alphaStar = calcAlpha(fnew, xold, s, x, cons)

            /// Set the values to alphaStar
            for (i in x.indices) {
                x[i].value = xold[i] + alphaStar * s[i]//calculate the new x
            }
            fnew = calc(cons)

            /////////////////////////////////////
            ///end of line search
            ////////////////////////////////////

            deltaXnorm = 0.0
            for (i in x.indices) {
                deltaX[i] = x[i].value - xold[i]//Calculate the difference in x for the hessian update
                deltaXnorm += deltaX[i] * deltaX[i]
                grad[i] = gradnew[i]
            }
            deltaXnorm = sqrt(deltaXnorm)
            iterations++
            /////////////////////////////////////////////////////////////
            ///End of Main loop
            /////////////////////////////////////////////////////////////
        }
        ////Debug

        ///End of function
        val validSolution = if (isFine) validSolutionFine else validSoltuionRough
        return if (fnew < validSolution) {
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
