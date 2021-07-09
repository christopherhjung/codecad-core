import kotlinx.coroutines.CompletableDeferred
import java.lang.Math.abs
import java.lang.Math.random
import java.util.concurrent.CompletableFuture

val minErrorChange = 1e-20
val targetError = 1e-8

class Solver {

    fun copyInto(target: DoubleArray, x: List<Value>) {
        for (i in x.indices) {
            target[i] = x[i].value
        }
    }

    fun solve(x: List<Parameter>, cons: List<Constraint>, accuracy: Double = targetError): Boolean {
        val original = DoubleArray(x.size)
        copyInto(original, x)

        val scaledError = accuracy

        var errorTerm: Value = Const.ZERO

        for(con in cons){
            val form = con.formular()
            errorTerm += form
        }

        var error = errorTerm.value
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

        val derivatives = mutableListOf<Value>()

        for (j in x.indices) {
            derivatives.add(errorTerm.derivative(x[j]))
        }

        while ((errorChange > minErrorChange && error > scaledError ) && iter < 100000) {
            for (j in x.indices) {
                grad[j] = derivatives[j].value
            }

            optimizer.optimize(grad)

            error = errorTerm.value
            errorChange = abs(error - lastError)
            lastError = error
            iter++
        }

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
