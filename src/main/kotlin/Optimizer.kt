import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.math.pow
import kotlin.math.sqrt

interface Optimizer {
    fun optimize(grad: DoubleArray)
}

class AdamOptimizer(private val consumer: (Int, Double) -> Unit) : Optimizer{
    private val alpha = 0.001
    private val beta1 = 0.9
    private val beta2 = 0.999
    private val epsilon = 10e-8
    private var m = 0.0
    private var v = 0.0
    private var t = 0

    override fun optimize(grad: DoubleArray) {
        t++
        for(i in grad.indices){
            m = beta1 * m + ( 1 - beta1 ) * grad[i]
            v = beta2 * v + ( 1 - beta2 ) * grad[i] * grad[i]
            val mHat = m / (1 - beta1.pow(t))
            val vHat = v / (1 - beta2.pow(t))
            consumer(i, - alpha * mHat / ( sqrt(vHat) + epsilon ))
        }
    }

}
