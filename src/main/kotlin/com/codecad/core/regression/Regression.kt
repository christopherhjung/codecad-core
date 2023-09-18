package com.codecad.core.regression

import com.codecad.core.ast.vec.Matrix
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.SphericalSurface
import kotlin.math.sqrt

object Regression {
    fun of(A : Matrix, B: Matrix) : Matrix{
        /*val AT = A.transpose()
        val AP = AT.matMul(A*AT).inverse()
        return AP.matMul(B)*/

        val AT = A.transpose()
        val AP = (AT * A).matMul(AT).inverse()
        return AP.matMul(B)
    }

    fun fitCircle(points: List<Vec2>) : Pair<Vec2, Double>{
        val aArr = DoubleArray(4 * points.size)
        val bArr = DoubleArray(points.size)

        for( (idx, point) in points.withIndex() ){
            aArr[idx] = point.x
            aArr[idx + 1] = point.y
            aArr[idx + 2] = 1.0
            bArr[idx] = point.squaredLength()
        }

        val A = Matrix(points.size, 3, aArr)
        val B = Matrix(points.size, 1, bArr)

        val xHat = of(A, B)
        val a = xHat[0, 0]
        val b = xHat[0, 1]
        val c = xHat[0, 2]
        val center = Vec2(a, b) / 2.0
        val r = sqrt(4.0 * c + a * a + b * b) / 2.0

        return Pair(center, r)
    }

    fun fitSphere(points : List<Vec3>) : SphericalSurface{
        val aArr = DoubleArray(4 * points.size)
        val bArr = DoubleArray(points.size)

        for( (idx, point) in points.withIndex() ){
            aArr[idx] = point.x
            aArr[idx + 1] = point.y
            aArr[idx + 2] = point.z
            aArr[idx + 3] = 1.0
            bArr[idx] = point.squaredLength()
        }

        val A = Matrix(points.size, 4, aArr)
        val B = Matrix(points.size, 1, bArr)

        val xHat = of(A, B)
        val a = xHat[0, 0]
        val b = xHat[0, 1]
        val c = xHat[0, 2]
        val d = xHat[0, 3]
        val center = Vec3(a, b, c) / 2.0
        val r = sqrt(4.0 * d + a * a + b * b + c * c) / 2.0

        return SphericalSurface(Workplane(center, Vec3.DirectionZ, Vec3.DirectionX), r)
    }
}