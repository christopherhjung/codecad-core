package com.codecad.core.shape

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.part.Sketch

class RoundRect : Shape(){
    lateinit var center: Vec2Expr
    lateinit var width: Expr
    lateinit var height: Expr

    override fun build(sketch: Sketch) {
        with(sketch){
            val topLine = line(point(0.0, 1.0),point(1.0,1.0))
            val bottomLine = line(point(0.0,0.0),point(1.0,0.0))

            val vertLine = cline(topLine.p0, bottomLine.p0)
            val vertLine2 = cline(topLine.p1, bottomLine.p1)

            val leftArc = arc(vertLine.p0, vertLine.p1, param(0.2))
            val rightArc = arc(vertLine2.p1, vertLine2.p0, param(0.2))

            val centerLine = cline(leftArc.center, rightArc.center)

            eq(topLine.length, bottomLine.length)

            perp(topLine, vertLine)

            eq(leftArc.radius, rightArc.radius)
            eq(vertLine.length, vertLine2.length)

            eq(topLine.p0.y, topLine.p1.y)

            eq(leftArc.center, vertLine.midPoint )
            eq(rightArc.center, vertLine2.midPoint )

            center = centerLine.midPoint
            width = centerLine.length
            height = vertLine.length
        }
    }

}
