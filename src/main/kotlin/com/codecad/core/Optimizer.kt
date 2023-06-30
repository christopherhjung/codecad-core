package com.codecad.core

import com.codecad.core.parser.ast.IdentExpr
import com.codecad.core.parser.ast.ParamExpr
import kotlin.math.sqrt

interface Optimizer {
    fun optimize(grad: DoubleArray)
}

class AdamOptimizer(private val params: List<ParamExpr>) : Optimizer {
    private val alpha = 0.01
    private val beta1 = 0.9
    private val beta2 = 0.999
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
            params[i].value -= alpha * mHat / ( sqrt(vHat) + epsilon )
        }
    }
}
