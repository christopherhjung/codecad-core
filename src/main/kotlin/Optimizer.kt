import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.math.pow
import kotlin.math.sqrt

interface Optimizer {
    fun optimize(grad: DoubleArray)
}

class ConjugateGradientOptimizer(private val params: List<Parameter>): Optimizer{
    private val values = DoubleArray(params.size)
    private val alpha = DoubleArray(params.size)
    private val residuum = DoubleArray(params.size)

    init {

    }

    override fun optimize(grad: DoubleArray) {
        for((i, param) in params.withIndex()){
            values[i] = param.value
        }

        var alpha = 0.0
        for(i in params.indices){
            alpha += residuum[i] * residuum[i]
        }
    }
}

class AdamOptimizer(private val params: List<Parameter>) : Optimizer{
    private val alpha = 0.002
    private val beta1 = 0.9
    private val beta2 = 0.999
    private val beta3 = 0.9
    private val epsilon = 10e-8
    private val m = DoubleArray(params.size)
    private val v = DoubleArray(params.size)

    private var currentBeta1 = 1.0
    private var currentBeta2 = 1.0

    override fun optimize(grad: DoubleArray) {
        currentBeta1 *= beta1
        currentBeta2 *= beta2
        for(i in grad.indices){
            m[i] = beta1 * m[i] + ( 1 - beta1 ) * grad[i]
            v[i] = beta2 * v[i] + ( 1 - beta2 ) * grad[i] * grad[i]
            val mHat = m[i] / (1 - currentBeta1)
            val vHat = v[i] / (1 - currentBeta2)
            val random =  (Math.random() - 0.5) * 2 * 1e-2
            params[i].value -= ( 1 ) * alpha * mHat / ( sqrt(vHat) + epsilon )
        }
    }

}
