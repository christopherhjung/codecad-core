import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.math.pow
import kotlin.math.sqrt

interface Optimizer {
    fun optimize(grad: DoubleArray)
}

class AdamOptimizer(private val size: Int, private val consumer: (Int, Double) -> Unit) : Optimizer{
    private val alpha = 0.005
    private val beta1 = 0.9
    private val beta2 = 0.999
    private val epsilon = 10e-8
    private val m = DoubleArray(size)
    private val v = DoubleArray(size)

    private var currentBeta1 = 1.0
    private var currentBeta2 = 1.0

    override fun optimize(grad: DoubleArray) {
        //t++
        currentBeta1 *= beta1
        currentBeta2 *= beta2
        for(i in grad.indices){
            m[i] = beta1 * m[i] + ( 1 - beta1 ) * grad[i]
            v[i] = beta2 * v[i] + ( 1 - beta2 ) * grad[i] * grad[i]
            val mHat = m[i] / (1 - currentBeta1)
            val vHat = v[i] / (1 - currentBeta2)
            consumer(i, - alpha * mHat / ( sqrt(vHat) + epsilon ))
        }
    }

}
