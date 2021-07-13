import kotlin.math.abs

val minErrorChange = 1e-18
val targetError = 1e-8

class Solver(val tracker: Tracker) {

    fun copyInto(target: DoubleArray, x: List<Value>) {
        for (i in x.indices) {
            target[i] = x[i].value
        }
    }

    fun solve(x: List<Set<Parameter>>, constraints: List<Constraint>, accuracy: Double = targetError): Boolean {
        val current = mutableListOf<Parameter>()
        val number = x.sumOf { it.size }

        var errorTerm: Value = Const.ZERO

        for(constraint in constraints){
            errorTerm += constraint.equation
        }

        val derivatives = mutableListOf<Value>()
        for( params in x ){
            for (param in params) {
                current.add(param)
                derivatives.add(errorTerm.derivative(param))
            }
        }

        val result = solveImpl(current, errorTerm, derivatives, accuracy)

        if(result){
            println("${current.size} instead of ${number}")
            return true
        }

        return false
    }

    fun solveImpl(x: List<Parameter>, errorTerm: Value, derivatives: List<Value>, accuracy: Double = targetError): Boolean{
        var error = errorTerm.value
        if (error < accuracy) {
            return true
        }

        val grad = DoubleArray(x.size)

        var lastError = error
        var errorChange = 1.0
        var iter = 0

        val optimizer = AdamOptimizer(x)

        val start = System.currentTimeMillis()
        while ((errorChange > minErrorChange && error > accuracy ) && iter < 100000) {
            tracker.addEntry(x, errorTerm)

            for (j in x.indices) {
                grad[j] = derivatives[j].value
            }

            optimizer.optimize(grad)

            error = errorTerm.value
            errorChange = abs(error - lastError)
            lastError = error
            iter++

            if(System.currentTimeMillis() - start > 1000){
                return false
            }
        }

        println(iter)

        return error < accuracy
    }
}
