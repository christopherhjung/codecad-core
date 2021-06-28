import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.math.pow
import kotlin.math.sqrt

interface Optimizer {
    fun optimize(grad: DoubleArray)
}

class AdamOptimizer(val consumer: BiConsumer<Int, Double>) : Optimizer{
    val alpha = 0.001
    val beta1 = 0.9
    val beta2 = 0.999
    val epsilon = 10e-8
    var m = 0.0
    var v = 0.0
    var t = 0

    override fun optimize(grad: DoubleArray) {
        t++
        for(i in grad.indices){
            m = beta1 * m + ( 1 - beta1 ) * grad[i]
            v = beta2 * v + ( 1 - beta2 ) * grad[i] * grad[i]
            val mHat = m / (1 - beta1.pow(t))
            val vHat = v / (1 - beta2.pow(t))
            consumer.accept(i, - alpha * mHat / ( sqrt(vHat) + epsilon ))
        }
    }

}
