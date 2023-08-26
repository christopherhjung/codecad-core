package com.codecad.core

import com.codecad.core.ast.primitive.*
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.face.entity.Workplane
import com.codecad.core.parser.Op
import com.codecad.core.scope.EmptyScope
import com.codecad.core.scope.Slot
import kotlin.math.pow

class World {
    private val sea = HashMap<Expr, Expr>()

    val MinusOne = LiteralExpr(this, -1.0)
    val Zero = LiteralExpr(this, 0.0)
    val One = LiteralExpr(this, 1.0)
    val Two = LiteralExpr(this, 2.0)

    val ZeroVec2 = vec2(Zero, Zero)
    val ZeroVec3 = vec3(Zero, Zero, Zero)

    val DirectionX = vec3(One, Zero, Zero)
    val DirectionY = vec3(Zero, One, Zero)
    val DirectionZ = vec3(Zero, Zero, One)

    val PlaneXY = Plane(DirectionZ, Zero)
    val PlaneYZ = Plane(DirectionX, Zero)
    val PlaneZX = Plane(DirectionY, Zero)

    val WorkplaneXY = Workplane(ZeroVec3, DirectionX, DirectionY)
    val WorkplaneYZ = Workplane(ZeroVec3, DirectionY, DirectionZ)
    val WorkplaneZX = Workplane(ZeroVec3, DirectionX, DirectionZ)

    private inline fun <reified T : Expr> unify(expr: T) : T {
        return sea.putIfAbsent(expr, expr) as? T ?: expr
    }

    fun negate(expr : Expr) : Expr {
        return if(expr === Zero){
            Zero
        }else if(expr is LiteralExpr){
            literal(-expr.evalDouble())
        }else{
            mul(MinusOne, expr)
            //unify(PrefixExpr(this, expr, Op.Sub))
        }
    }

    fun infix(lhs : Expr, rhs: Expr, op : Op) : Expr {
        return when(op){
            Op.Add -> add(lhs, rhs)
            Op.Sub -> sub(lhs, rhs)
            Op.Mul -> mul(lhs, rhs)
            Op.Div -> div(lhs, rhs)
            Op.AssignAdd -> infix(lhs, add(lhs, rhs), Op.Assign)
            Op.AssignSub -> infix(lhs, add(lhs, rhs), Op.Assign)
            Op.AssignMul -> infix(lhs, add(lhs, rhs), Op.Assign)
            Op.AssignDiv -> infix(lhs, add(lhs, rhs), Op.Assign)
            Op.Lt, Op.Le -> cmp(lhs, rhs, op)
            Op.Gt -> cmp(rhs, lhs, Op.Le)
            Op.Ge -> cmp(rhs, lhs, Op.Lt)
            Op.Assign -> unify(InfixExpr(this, lhs, rhs, op))
            else -> throw NotImplementedError()
        }
    }

    private fun cmp(lhs : Expr, rhs: Expr, op : Op) : Expr{
        val infix = InfixExpr(this, lhs, rhs, op)
        return if(lhs is LiteralExpr && rhs is LiteralExpr){
            literal(infix.evalDouble())
        }else{
            unify(infix)
        }
    }

    fun prefix(expr : Expr, op : Op) : Expr {
        return when(op){
            Op.Sub -> negate(expr)
            Op.Inc, Op.Dec -> unify(PrefixExpr(this, expr, op))
            else -> throw NotImplementedError()
        }
    }

    fun postfix(expr : Expr, op : Op) : Expr {
        return when(op){
            Op.Inc, Op.Dec -> unify(PostfixExpr(this, expr, op))
            else -> throw NotImplementedError()
        }
    }

    fun add(lhs : Expr, rhs: Expr) : Expr {
        return if(lhs === Zero){
            rhs
        }else if(rhs === Zero){
            lhs
        }else if(lhs === rhs){
            mul(Two, rhs)
        }else if(rhs is LiteralExpr && lhs is LiteralExpr){
            literal(lhs.evalDouble() + rhs.evalDouble())
        }else if(lhs is PowExpr && lhs.exp === Two && rhs is PowExpr && rhs.exp === Two){
            val lhsBase = lhs.base
            val rhsBase = rhs.base
            if(lhsBase is SinExpr && rhsBase is CosExpr && lhsBase.arg === rhsBase.arg ){
                One
            }else if(lhsBase is CosExpr && rhsBase is SinExpr && lhsBase.arg === rhsBase.arg ){
                One
            }else{
                reassociate(lhs, rhs, Op.Add)
            }
        }else{
            reassociate(lhs, rhs, Op.Add)
        }
    }

    fun sub(lhs : Expr, rhs: Expr) : Expr {
        return if (lhs === Zero) {
            negate(rhs)
        } else if (rhs === Zero) {
            lhs
        } else if (lhs === rhs) {
            Zero
        }else if(rhs is PrefixExpr && rhs.op == Op.Sub){
            add(lhs, rhs.expr)
        }else if(rhs is LiteralExpr){
            if(lhs is LiteralExpr){
                literal(lhs.evalDouble() - rhs.evalDouble())
            }else{
                add(lhs, literal(-rhs.evalDouble()))
            }
        }else {
            reassociate(lhs, rhs, Op.Sub)
        }
    }

    fun mul(lhs : Expr, rhs: Expr) : Expr {
        return if(lhs === Zero || rhs === Zero){
            Zero
        }else if(lhs === One){
            rhs
        }else if(rhs === One){
            lhs
        }else if(lhs === rhs){
            pow(lhs, Two)
        }else if(lhs is PrefixExpr && rhs is PrefixExpr && lhs.op == Op.Sub && rhs.op == Op.Sub){
            mul(lhs.expr, rhs.expr)
        }else if(lhs is LiteralExpr && rhs is LiteralExpr){
            literal(lhs.evalDouble() * rhs.evalDouble())
        }else if(lhs is SinExpr && rhs is CosExpr && lhs.arg === rhs.arg){
            mul(literal(0.5), sin(mul(Two, lhs.arg)))
        }else if(lhs is CosExpr && rhs is SinExpr && lhs.arg === rhs.arg){
            mul(literal(0.5), sin(mul(Two, lhs.arg)))
        }else{
            reassociate(lhs, rhs, Op.Mul)
        }
    }

    /// (1)     la    op (lz op w) -> (la op lz) op w
    /// (2) (lx op y) op (lz op w) -> (lx op lz) op (y op w)
    /// (3)      a    op (lz op w) ->  lz op (a op w)
    /// (4) (lx op y) op      b    ->  lx op (y op b)
    fun reassociate(a : Expr, b: Expr, op: Op ) : Expr{
        if( op.isAssociative ){
            var lx : LiteralExpr? = null
            val y = if(a is InfixExpr && a.op == op){
                lx = a.lhs as? LiteralExpr
                a.rhs
            }else null

            var lz : LiteralExpr? = null
            val w = if(b is InfixExpr && b.op == op){
                lz = b.lhs as? LiteralExpr
                b.rhs
            }else null

            return when{
                a is LiteralExpr && lz != null -> infix(infix(a, lz, op), w!!, op)            // (1)
                lx != null && lz != null -> infix(infix(lx, lz, op), infix(y!!, w!!, op), op) // (2)
                lz != null -> infix(lz, infix(a, w!!, op), op)                                // (3)
                lx != null -> infix(lx, infix(y!!, b, op), op)                                // (4)
                b is LiteralExpr -> infix(b, a, op)
                else -> unify(InfixExpr(this, a, b, op))
            }
        }else{
            return unify(InfixExpr(this, a, b, op))
        }
    }

    fun div(lhs : Expr, rhs: Expr) : Expr {
        return if (lhs === Zero) {
            Zero
        }else if (rhs === One){
            lhs
        }else if (lhs === rhs){
            One
        }else if(rhs is InfixExpr && rhs.op == Op.Div){
            div(mul(lhs, rhs.rhs), rhs.lhs)
        }else if (lhs is LiteralExpr) {
            if(rhs is LiteralExpr){
                literal(lhs.evalDouble() / rhs.evalDouble())
            }else{
                mul(literal(1.0 / rhs.evalDouble()), lhs)
            }
        }else if(rhs is LiteralExpr){
            mul(literal(1.0 / rhs.evalDouble()), lhs)
        }else {
            reassociate(lhs, rhs, Op.Div)
        }
    }

    fun lt(lhs : Expr, rhs: Expr) : Expr {
        return cmp(lhs, rhs, Op.Lt)
    }

    fun ifExpr(condition: Expr, lhs: Expr, rhs: Expr) : Expr {
        return if(condition is LiteralExpr){
            if(condition.evalBoolean(EmptyScope)){
                lhs
            }else{
                rhs
            }
        }else if(lhs is LiteralExpr && rhs is LiteralExpr && lhs === rhs){
            lhs
        }else{
            unify(IfExpr(this, condition, lhs, rhs))
        }
    }

    fun pow(base : Expr, exp: Expr) : Expr {
        return if(exp === Zero){
            One
        }else if(exp === One){
            base
        }else if(base === Zero){
            Zero
        }else if(base is PowExpr){
            pow(base.base, mul(base.exp, exp))
        }else if (exp is LiteralExpr) {
            if(base is LiteralExpr){
                literal(base.evalDouble().pow(exp.evalDouble()))
            }else{
                val expValue = exp.evalDouble()
                if(expValue < 0.0){
                    div(One, pow(base, literal(-expValue)))
                }else{
                    unify(PowExpr(this, base, exp))
                }
            }
        }else{
            unify(PowExpr(this, base, exp))
        }
    }

    fun cos(expr : Expr) : Expr {
        val cosExpr = CosExpr(this, expr)
        return if(expr is LiteralExpr){
            cosExpr.evalLiteral()
        }else{
            unify(cosExpr)
        }
    }

    fun sin(expr : Expr) : Expr {
        val sinExpr = SinExpr(this, expr)
        return if(expr is LiteralExpr){
            sinExpr.evalLiteral()
        }else{
            unify(sinExpr)
        }
    }

    fun asin(expr : Expr) : Expr {
        val asinExpr = AsinExpr(this, expr)
        return if(expr is LiteralExpr){
            asinExpr.evalLiteral()
        }else{
            unify(asinExpr)
        }
    }

    fun log(expr : Expr) : Expr {
        val log = LogExpr(this, expr)
        return when(expr){
            is LiteralExpr -> log.evalLiteral()
            is ExpExpr -> expr.arg
            else -> unify(log)
        }
    }

    fun exp(expr : Expr) : Expr {
        val exp = ExpExpr(this, expr)
        return when(expr){
            is LiteralExpr -> exp.evalLiteral()
            is LogExpr -> expr.arg
            else -> unify(exp)
        }
    }

    fun abs(expr: Expr) : Expr {
        val abs = AbsExpr(this, expr)
        return when(expr){
            is LiteralExpr -> abs.evalLiteral()
            else -> unify(abs)
        }
    }

    fun sign(expr: Expr) : Expr {
        val sign = SignExpr(this, expr)
        return when(expr){
            is LiteralExpr -> sign.evalLiteral()
            else -> unify(sign)
        }
    }

    fun literal(value: Any?) : Expr {
        return when(value){
            0.0 -> Zero
            1.0 -> One
            2.0 -> Two
            -1.0 -> MinusOne
            else -> unify(LiteralExpr(this, value))
        }
    }

    fun param(value: Double) : Expr {
        return ParamExpr(this, value)
    }

    fun tuple(vararg expr: Expr) : Expr{
        return unify(TupleExpr(this, expr))
    }

    fun ref(slot : Slot) : Expr{
        return unify(RefExpr(this, slot))
    }

    fun vec2(x: Expr, y: Expr) : Vec2Expr {
        return unify(Vec2Expr(this, x, y))
    }

    fun vec3(x: Expr, y: Expr, z: Expr) : Vec3Expr {
        return unify(Vec3Expr(this, x, y, z))
    }
}