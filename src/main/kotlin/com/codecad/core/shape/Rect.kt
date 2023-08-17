package com.codecad.core.shape

import com.codecad.core.SketchSegment
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.part.Sketch

class Rect() : Shape() {
    lateinit var a: Vec2Expr
    lateinit var b: Vec2Expr
    lateinit var c: Vec2Expr
    lateinit var d: Vec2Expr

    lateinit var center: Vec2Expr

    lateinit var top: SketchSegment
    lateinit var right: SketchSegment
    lateinit var bottom: SketchSegment
    lateinit var left: SketchSegment

    lateinit var width: Expr
    lateinit var height: Expr

    fun names(): List<String> {
        return listOf("width", "height", "top", "bottom")
    }

    override fun build(sketch: Sketch) {
        with(sketch){
            a = point(0.0,0.0)
            b = point(1.0,0.0)
            c = point(1.0,1.0)
            d = point(0.0,1.0)

            top = line(a,b)
            right = line(b,c)
            bottom = line(c,d)
            left = line(d,a)

            width = top.length
            height = right.length
            center = (a + b + c + d) / 4.0

            eq((c-a).length(), (d - b).length())
            eq(top.length , bottom.length)
            eq(left.length , right.length)
        }
    }
}