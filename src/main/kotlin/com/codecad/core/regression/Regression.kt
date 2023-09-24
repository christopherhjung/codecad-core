package com.codecad.core.regression

import com.codecad.core.ast.vec.Matrix
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

data class CircleFit(val center : Vec2, val radius: Double)
data class SphereFit(val center : Vec3, val radius: Double)
data class EllipseFit(val center : Vec2, val direction : Vec2, val major: Double, val minor: Double)

object Regression {
    fun solve(A : Matrix, B: Matrix) : Matrix?{
        val AT = A.transpose()
        val ATA = AT.matMul(A)
        if(abs(ATA.det()) < 1e-8) return null
        val AP = ATA.inverse().matMul(AT)
        return AP.matMul(B)
    }

    fun fitCircle(points: List<Vec2>) : CircleFit?{
        val aArr = DoubleArray(3 * points.size)
        val bArr = DoubleArray(points.size)

        for( (idx, point) in points.withIndex() ){
            aArr[3 * idx] = point.x
            aArr[3 * idx + 1] = point.y
            aArr[3 * idx + 2] = 1.0
            bArr[idx] = point.squaredLength()
        }

        val A = Matrix(points.size, 3, aArr)
        val B = Matrix(points.size, 1, bArr)

        val xHat = solve(A, B) ?: return null
        val a = xHat[0, 0]
        val b = xHat[0, 1]
        val c = xHat[0, 2]
        val center = Vec2(a, b) / 2.0
        val r = sqrt(4.0 * c + a * a + b * b) / 2.0

        return CircleFit(center, r)
    }

    fun fitEllipse(points: List<Vec2>) : EllipseFit?{
        val aArr = DoubleArray(5 * points.size)
        val bArr = DoubleArray(points.size)

        for( (idx, point) in points.withIndex() ){
            aArr[5 * idx] = point.x * point.y
            aArr[5 * idx + 1] = point.y.pow(2.0)
            aArr[5 * idx + 2] = point.x
            aArr[5 * idx + 3] = point.y
            aArr[5 * idx + 4] = 1.0
            bArr[idx] = -point.x.pow(2.0)
        }

        val matA = Matrix(points.size, 5, aArr)
        val matB = Matrix(points.size, 1, bArr)

        val xHat = solve(matA, matB) ?: return null
        val A = 1.0
        val B = xHat[0, 0]
        val C = xHat[0, 1]
        val D = xHat[0, 2]
        val E = xHat[0, 3]
        val F = xHat[0, 4]

        val B24AC = B*B-4*A*C
        if(B24AC >= 0.0) return null
        val inv = 1/B24AC
        val factor = 2*(A*E*E + C*D*D - B*D*E + B24AC*F)
        val sqrtAC2B2 = sqrt((A - C).pow(2.0) + B*B)

        val a = -sqrt(factor * (A+C + sqrtAC2B2)) * inv
        val b = -sqrt(factor * (A+C - sqrtAC2B2)) * inv
        val x0 = (2*C*D-B*E) * inv
        val y0 = (2*A*E-B*D) * inv
        val center = Vec2(x0, y0)
        val direction = if(abs(B) < 1e-8){
            if(A < C){
                Vec2(1.0, 0.0)
            }else{
                Vec2(0.0, 1.0)
            }
        }else{
            Vec2(1.0, (C-A- sqrtAC2B2) / B).normalized()
        }

        return EllipseFit(center, direction, a, b)
    }

    fun matchEllipse(points: List<Vec2>, error: Double) : EllipseFit?{
        val ellipse = fitEllipse(points) ?: return null
        val errorSq = error.pow(2)
        points.forEach{
            val offset = it - ellipse.center
            val pointOnEllipse = offset.skew(ellipse.direction)

            val r = (pointOnEllipse.x / ellipse.major).pow(2) +
                    (pointOnEllipse.y / ellipse.minor).pow(2)

            if(abs(r - 1.0) >= errorSq){
                return null
            }
        }

        return ellipse
    }

    fun acot(x : Double) : Double{
        return (Math.PI / 2.0) - atan(x)
    }

    fun fitSphere(points : List<Vec3>) : SphereFit?{
        val aArr = DoubleArray(4 * points.size)
        val bArr = DoubleArray(points.size)

        for( (idx, point) in points.withIndex() ){
            aArr[4 * idx] = point.x
            aArr[4 * idx + 1] = point.y
            aArr[4 * idx + 2] = point.z
            aArr[4 * idx + 3] = 1.0
            bArr[idx] = point.squaredLength()
        }

        val A = Matrix(points.size, 4, aArr)
        val B = Matrix(points.size, 1, bArr)

        val xHat = solve(A, B) ?: return null
        val a = xHat[0, 0]
        val b = xHat[0, 1]
        val c = xHat[0, 2]
        val d = xHat[0, 3]
        val center = Vec3(a, b, c) / 2.0
        val radius = sqrt(4.0 * d + a * a + b * b + c * c) / 2.0
        return SphereFit(center, radius)
    }
}