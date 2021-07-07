import java.lang.Math.abs
import kotlin.math.pow
import kotlin.math.sqrt

val minErrorChange = 1e-12
val targetError = 1e-8

fun calc(constraints: List<Constraint>): Double {
    var error = 0.0
    for ((i, constraint) in constraints.withIndex()) {
        val constError = constraint.error()
        error += constError
    }
    return error
}

class Solver {

    fun calcGrad(currentError: Double, grad: DoubleArray, x: List<Value>, cons: List<Constraint>) {
        val pert = 10e-8
        for (j in x.indices) {
            val temp = x[j].value
            x[j].value = temp + pert
            var rightError = calc(cons)

            x[j].value = temp - pert
            val leftError = calc(cons)

            val avgGrad = 0.5 * ( rightError - leftError ) / pert

            grad[j] = if(kotlin.math.abs(avgGrad) > 10e-8){
                avgGrad
            }else if (rightError < currentError) {
                (rightError - currentError) / pert
            } else if (leftError < currentError) {
                (currentError - leftError) / pert
            } else {
                0.0
            }


            x[j].value = temp
        }
    }

    fun copyInto(target: DoubleArray, x: List<Value>) {
        for (i in x.indices) {
            target[i] = x[i].value
        }
    }

    fun solve(x: List<Value>, cons: List<Constraint>, accuracy: Double = targetError): Boolean {
        val original = DoubleArray(x.size)
        copyInto(original, x)

        val scaledError = accuracy * x.size

        var error = calc(cons)
        if (error < scaledError) {
            return true
        }

        val grad = DoubleArray(x.size)

        var lastError = error
        var errorChange = 1.0
        var iter = 0

        val optimizer = AdamOptimizer(x.size){ i, diff ->
            x[i].value += diff
        }


        while ((errorChange > minErrorChange && error > scaledError ) && iter < 1000000) {
            calcGrad(error, grad, x, cons)

            optimizer.optimize(grad)

            error = calc(cons)
            errorChange = abs(error - lastError)
            lastError = error
            iter++
        }

        println(iter)
        //println(error)

        return if (error < scaledError) {
            true
        } else {
            for (i in x.indices) {
                //x[i].value = original[i]
            }
            false
        }

    }
}
