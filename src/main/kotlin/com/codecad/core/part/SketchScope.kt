package com.codecad.core.part

import com.codecad.common.LineD
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.sketch.*
import kotlin.reflect.KClass

class PatternScope(project: Project, val count: Int, val center: Vec2) : SketchScope(project) {
    val allCrawler = mutableMapOf<Vec2, MutableList<MutableList<Vec2>>>()
    val pointLookup = mutableMapOf<Vec2, Array<Vec2?>>()

    fun all(ref: Vec2, list: MutableList<Vec2>)  {
        allCrawler.computeIfAbsent(ref){ mutableListOf()}.add(list)
    }

    var current = 0

    private fun rotatePoint(point: Vec2, angle: Expr) : Vec2 {
        val array = pointLookup.computeIfAbsent(point){Array(count - 1){null} }

        if(array[current] == null){
            array[current] = point.rotate(center, angle)
        }

        return array[current]!!
    }

    fun finish(){
        sketch.solve(1e-8)
        val rawElements = sketch.figures.toList()

        for(i in 0 until count - 1){
            val angle = literal((2 * Math.PI / count) * (i + 1))
            current = i

            for(element in rawElements){
                sketch.figures.add(
                    if(element is Vec2){
                        rotatePoint(element, angle)
                    }else if(element is Segment2){
                        Segment2(
                            rotatePoint(element.p0, angle),
                            rotatePoint(element.p1, angle)
                        )
                    }else if(element is Circle){
                        Circle(
                            rotatePoint(element.center, angle),
                            element.radius
                        )
                    }else if(element is Arc){
                        Arc(
                            rotatePoint(element.p0, angle),
                            rotatePoint(element.p1, angle),
                            element.h
                        )
                    }else continue
                )
            }
        }

        for((ref, crawler) in allCrawler.entries){
            for(list in crawler){
                list.add(ref)
                list.addAll(pointLookup[ref]!!.map { it!! })
            }
        }
    }
}

open class SketchScope(val project: Project) {
    val sketch = Sketch(project, "")

    val deg = 0
    val rad = 1

    val mm = 2
    val cm = 3

    fun Expr.isEquals(other : Expr) {
        equal(this, other)
    }

    infix fun Double.unit(other: Int): Double {
        return if (other == deg) {
            Math.toRadians(this)
        } else {
            this
        }
    }

    fun <T> create(type: KClass<T>) : T where T : Component {
        val component = type.constructors.first().call()
        component.build(sketch)
        return component
    }

    inline fun <reified T> create() : T where T : Component {
        val component = T::class.constructors.first().call()
        component.build(sketch)
        return component
    }

    fun pattern(repeat: Int, point: Vec2, init: PatternScope.(Vec2) -> Unit): Sketch {
        val builder = PatternScope(project, repeat, point)
        builder.init(point)
        builder.finish()
        sketch.figures.addAll(builder.sketch.figures)
        return builder.sketch
    }

    fun param(value: Number = 0.0): Expr {
        return sketch.param(value.toDouble())
    }

    fun <T> list() : MutableList<T>{
        return mutableListOf()
    }

    fun literal(value: Number = 0.0): Expr {
        return sketch.createLiteral(value.toDouble())
    }

    fun literalPoint(x: Number = 0.0, y: Number = 0.0): Vec2 {
        return sketch.constPoint(x.toDouble(), y.toDouble())
    }

    fun point(x: Number = 0.0, y: Number = 0.0): Vec2 {
        return sketch.point(x.toDouble(), y.toDouble())
    }

    fun line(a: Vec2, b: Vec2): Segment2 {
        return sketch.line(a, b, LineType.Normal)
    }

    fun cline(a: Vec2, b: Vec2): Segment2 {
        return sketch.line(a, b, LineType.Construction)
    }

    fun circle(center: Vec2, radius: Expr): Circle {
        return sketch.circle(center, radius)
    }

    fun arc(p0: Vec2, p1: Vec2, radius: Expr): Arc {
        return sketch.arc(p0,p1,radius)
    }

    fun func(block: (Expr) -> Vec2): FunctionFigure {
        return sketch.func(block)
    }

    fun polygon(vararg points: Vec2) : List<Segment2> {
        val lines = mutableListOf<Segment2>()
        for ((left, right) in points.toList().rollover()) {
            lines.add(line(left, right))
        }

        return lines
    }

    fun line(x: Number = 0.0, y: Number = 0.0, x2: Number = 0.0, y2: Number = 0.0): Segment2 {
        return sketch.line(x.toDouble(), y.toDouble(), x2.toDouble(), y2.toDouble())
    }

    fun tangent(circle: CircleLike, line: Segment2) {
        addConstraintImpl(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Vec2, line: Segment2) {
        addConstraintImpl(PointOnLineMidpoint(point, line))
    }

    fun pointOnLine(point: Vec2, line: Segment2) {
        addConstraintImpl(PointOnLine(point, line))
    }

    fun horizontal(line: Segment2) {
        addConstraintImpl(Horizontal(line))
    }

    fun vertical(line: Segment2) {
        addConstraintImpl(Vertical(line))
    }

    fun pointOnCircle(point: Vec2, circle: Circle) {
        addConstraintImpl(PointOnCircle(point, circle))
    }

    fun equalLength(line1: Segment2, line2: Segment2) {
        addConstraintImpl(Equals(line1.length, line2.length))
    }

    fun length(line1: Segment2, length: Expr) {
        addConstraintImpl(Equals(line1.length, length))
    }

    fun angle(line1: Segment2, line2: Segment2, angle: Expr) {
        addConstraintImpl(InternalAngle(line1, line2, angle))
    }

    fun addConstraint(constraint: Constraint) {
        addConstraintImpl(constraint)
    }

    fun perpendicular(line1: Segment2, line2: Segment2){
        addConstraintImpl(Perpendicular(line1, line2))
    }

    fun parallel(line1: Segment2, line2: Segment2){
        addConstraintImpl(Parallel(line1, line2))
    }

    fun pointOnPoint(point1: Vec2, vec2: Vec2) {
        addConstraintImpl(PointOnPoint(point1, vec2))
    }

    fun equal(point1: Vec2, vec2: Vec2) {
        addConstraintImpl(PointOnPoint(point1, vec2))
    }

    fun equal(value1 : Expr, value2: Expr) {
        addConstraintImpl(Equals(value1, value2))
    }

    fun radius(circle: Circle, value: Expr) {
        addConstraintImpl(Equals(circle.radius, value))
    }

    fun origin() : Vec2 {
        return sketch.world.ORIGIN
    }

    fun axisX() : Segment2 {
        return sketch.world.AXIS_X
    }

    fun axisY() : Segment2 {
        return sketch.world.AXIS_Y
    }

    private fun addConstraintImpl(constraint: Constraint){
        //val callersLineNumber = Thread.currentThread().stackTrace[3].lineNumber
        sketch.addConstraint(constraint)
    }

    fun add(component: Component){
        component.build(sketch)
    }
}


fun sketchToLines(sketch: Sketch, ignoreConstruction: Boolean = false) : List<LineD>{
    val list = mutableListOf<LineD>()
    for(figure in sketch.figures){
        if(ignoreConstruction && sketch.lineType[figure] == LineType.Construction){
            continue
        }

        val plotter = figure.plotter()

        var last = plotter.next()
        while( plotter.hasNext() ){
            val next = plotter.next()
            list.add(LineD(last.fixed(), next.fixed()))
            last = next
        }
    }
    return list
}

fun figureToPoints(figure: Figure) : List<PointD>{
    val list = mutableListOf<PointD>()

    val plotter = figure.plotter()
    while( plotter.hasNext() ){
        list.add(plotter.next().fixed())
    }

    return list
}

