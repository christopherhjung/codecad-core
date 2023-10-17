package com.codecad.core.ast.vec

import com.codecad.core.EPSILON
import com.codecad.core.Utils
import com.codecad.core.World
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.Scope
import kotlin.math.*

interface Vec<T : Vec<T>>{
    fun dot(other: T) : Double

    operator fun times(value: Double) : T

    operator fun div(value: Int) : T{
        return div(value.toDouble())
    }

    operator fun div(right: Double) : T

    operator fun plus(right: T) : T

    operator fun minus(right: T) : T

    operator fun minus(right: Double) : T

    fun squaredLength(): Double

    fun length(): Double

    fun squaredDistance(other: T): Double

    fun distanceTo(other: T): Double

    fun negate() : T

    fun scaleTo(factor : Double) : T

    fun normalized() : T

    fun projectOn(rhs: T) : T
}

data class Vec2(val x: Double, val y: Double) : Vec<Vec2>{
    init {
        assert(!x.isNaN())
        assert(!y.isNaN())
    }

    companion object{
        val Zero = Vec2(0.0, 0.0)
        val DirX = Vec2(1.0, 0.0)
        val DirY = Vec2(0.0, 1.0)

        //projects lhs onto rhs
        fun project(lhs: Vec2, rhs: Vec2) : Vec2{
            return rhs * ( lhs.dot(rhs) / rhs.squaredLength() )
        }

        private fun halfSector(cross : Double) : Boolean{
            return cross < 0.0
        }

        private fun sector(halfSector: Boolean, dot : Double) : Int{
            return if(halfSector){
                if(dot < 0.0){
                    3
                }else{
                    4
                }
            }else{
                if(dot > 0.0){
                    1
                }else{
                    2
                }
            }
        }

        fun rotaryCmp(reference: Vec2, lhs: Vec2, rhs: Vec2) : Int{
            val lhsCross = reference.crossZ(lhs)
            val rhsCross = reference.crossZ(rhs)

            val lhsHalfSector = halfSector(lhsCross)
            val rhsHalfSector = halfSector(rhsCross)
            val halfSectorCmp = lhsHalfSector.compareTo(rhsHalfSector)

            if(halfSectorCmp != 0){
                return halfSectorCmp
            }

            val lhsDot = reference.dot(lhs)
            val rhsDot = reference.dot(rhs)

            val lhsSector = sector(lhsHalfSector, lhsDot)
            val rhsSector = sector(rhsHalfSector, rhsDot)
            val sectorCmp = lhsSector.compareTo(rhsSector)

            if(sectorCmp != 0){
                return sectorCmp
            }

            val cross = lhs.crossZ(rhs)
            if(abs(cross) < EPSILON){
                return 0
            }

            return 0.0.compareTo(cross)
        }
    }

    override fun projectOn(rhs: Vec2): Vec2 {
        return project(this, rhs)
    }

    fun near(other: Vec2, distance : Double) : Boolean{
        return squaredDistance(other) < distance.pow(2)
    }

    fun skew(newDir : Vec2) : Vec2{
        val x = dot(newDir)
        val y = sqrt(squaredLength() - x.pow(2))
        return Vec2(x, y)
    }

    fun rightTurn(vec: Vec2) : Vec2{
        return Vec2(x + vec.y, y - vec.x)
    }

    fun leftTurn(vec: Vec2) : Vec2{
        return Vec2(x - vec.y, y + vec.x)
    }

    fun rotateCCW() : Vec2{
        return Vec2(-y, x)
    }

    fun rotateCW() : Vec2{
        return Vec2(y, -x)
    }

    fun atan2() : Double{
        return atan2(y, x)
    }

    fun absoluteAngle(center : Vec2): Double {
        return (this - center).absoluteAngle()
    }

    fun absoluteAngle(): Double {
        val angle = atan2(y, x)
        return if(angle < 0.0){
            2 * Math.PI + angle
        }else{
            angle
        }
    }

    fun angleTo(other : Vec2) : Double{
        return Utils.normalizeAngle(atan2(other.y, other.x) - atan2(y, x))
    }

    override fun negate() : Vec2{
        return Vec2(-x, -y)
    }

    fun negateX() : Vec2{
        return Vec2(-x, y)
    }

    fun negateY() : Vec2{
        return Vec2(x, -y)
    }

    fun rotate(center: Vec2, angle: Double) : Vec2 {
        return rotate(this - center, angle) + center
    }

    fun rotate(angle: Double) : Vec2 {
        val s = sin(angle)
        val c = cos(angle)

        return Vec2(
            c * x - s * y,
            s * x + c * y
        )
    }

    override fun dot(other: Vec2) : Double {
        return x * other.x + y * other.y
    }

    fun crossZ(right: Vec2) : Double {
        return x * right.y - y * right.x
    }

    override operator fun times(value: Double) : Vec2 {
        return Vec2(x * value, y * value)
    }

    override operator fun div(right: Double) : Vec2 {
        return Vec2(x / right, y / right)
    }

    override operator fun plus(right: Vec2) : Vec2 {
        return Vec2(x + right.x, y + right.y)
    }

    override operator fun minus(right: Vec2) : Vec2 {
        return Vec2(x - right.x, y - right.y)
    }

    override operator fun minus(right: Double) : Vec2 {
        return Vec2(x - right, y - right)
    }

    override fun squaredLength(): Double {
        return x.pow(2) + y.pow(2)
    }

    override fun length(): Double {
        return sqrt(squaredLength())
    }

    override fun squaredDistance(other: Vec2): Double {
        return ( x - other.x ).pow(2) + ( y - other.y ).pow(2)
    }

    override fun distanceTo(other: Vec2): Double {
        return sqrt(squaredDistance(other))
    }

    override fun scaleTo(factor: Double): Vec2 {
        return this * (factor / length())
    }

    override fun normalized() : Vec2 {
        return this / length()
    }

    fun toExpr(world: World) : Vec2Expr{
        return world.vec2(world.literal(x), world.literal(y))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }
}

class Vec2Expr(world : World, val x: Expr, val y: Expr) : Expr(world) {

    fun absoluteAngle() : Expr{
        return atan2(y, x)
    }

    fun normalized() : Vec2Expr {
        return when (this.opt) {
            NormalizedVec -> this
            else -> {
                val result = this / length()
                result.opt = NormalizedVec
                result
            }
        }
    }

    fun rotate(center: Vec2Expr, angle: Expr) : Vec2Expr {
        return rotate(this - center, angle) + center
    }

    fun rotate(angle: Expr) : Vec2Expr {
        val s = sin(angle)
        val c = cos(angle)

        return world.vec2(
            c * x - s * y,
            s * x + c * y
        )
    }

    override fun eval(scope: Scope): Vec2 {
        return Vec2(x.evalDouble(scope), y.evalDouble(scope))
    }

    fun dot(right: Vec2Expr) : Expr {
        return x * right.x + y * right.y
    }

    fun crossZ(right: Vec2Expr) : Expr {
        return x * right.y - y * right.x
    }

    operator fun times(right: Vec2Expr) : Expr {
        return x * right.y - y * right.x
    }

    override operator fun times(right: Expr) : Vec2Expr {
        return world.vec2(x * right, y * right)
    }

    override operator fun times(right: Double) : Vec2Expr {
        val value = x.world.literal(right)
        return world.vec2(x * value, y * value)
    }

    override operator fun div(right: Expr) : Vec2Expr {
        return world.vec2(x / right, y / right)
    }

    override operator fun div(right: Double) : Vec2Expr {
        val value = x.world.literal(right)
        return world.vec2(x / value, y / value)
    }

    operator fun plus(right: Vec2Expr) : Vec2Expr {
        return world.vec2(x + right.x, y + right.y)
    }

    operator fun minus(right: Vec2Expr) : Vec2Expr {
        return world.vec2(x - right.x, y - right.y)
    }

    override operator fun minus(right: Expr) : Vec2Expr {
        return world.vec2(x - right, y - right)
    }

    fun squaredLength(): Expr {
        return when (opt) {
            NormalizedVec -> world.One
            else -> x.pow(2) + y.pow(2)
        }
    }

    fun length(): Expr {
        return when (opt) {
            NormalizedVec -> world.One
            else -> squaredLength().sqrt()
        }
    }

    fun squaredDistance(right: Vec2Expr): Expr {
        return ( x - right.x ).pow(2) + ( y - right.y ).pow(2)
    }

    fun distance(right: Vec2Expr): Expr {
        return squaredDistance(right).sqrt()
    }

    override fun equals(right: Any?): Boolean {
        return this === right ||
                right is Vec2Expr &&
                x == right.x &&
                y == right.y
    }

    override fun hashCode(): Int {
        var hash = 31 * x.hashCode()
        hash = 31 * y.hashCode() + 11 * hash
        return hash
    }

    override fun toString(): String {
        return "Vec2(x=$x, y=$y)"
    }
}