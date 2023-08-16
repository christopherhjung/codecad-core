package com.codecad.core.part

import com.codecad.common.LineD
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.optimizer.Solver
import com.codecad.core.constraint.*
import com.codecad.core.face.entity.curve.Circle
import com.codecad.core.face.entity.curve.Conic
import java.util.*

class Sketch(val partStudio: PartStudio, val name: String) {
    val params = HashSet<ParamExpr>()
    val constraints = HashSet<Constraint>()
    val figures = ArrayList<Figure>()
    val lineType = HashMap<Figure, LineType>()
    val world: World = partStudio.world

    fun param(value: Double = 0.0): ParamExpr {
        val param = ParamExpr( world, value )
        partStudio.tracker.params.add(param)
        params.add(param)
        return param
    }


    fun createLiteral(value: Double = 0.0) : Expr {
        return world.literal(value)
    }

    fun constPoint(x: Double = 0.0, y: Double = 0.0): Vec2Expr {
        val a = createLiteral(x)
        val b = createLiteral(y)
        val point = world.vec2(a, b)
        return point
    }

    fun point(x: Expr, y: Expr): Vec2Expr {
        val point = world.vec2(x,y)
        return point
    }

    fun point(x: Double = 0.0, y: Double = 0.0): Vec2Expr {
        val a = param(x)
        val b = param(y)
        return point(a,b)
    }

    fun line(a: Vec2Expr, b: Vec2Expr, type: LineType = LineType.Normal): SketchSegment {
        val line = SketchSegment(a, b)
        lineType.putIfAbsent(line, type)
        return line
    }

    fun cline(a: Vec2Expr, b: Vec2Expr) : SketchSegment {
        return line(a,b, LineType.Construction)
    }

    fun circle(center: Vec2Expr, radius: Expr): SketchCircle {
        val circle = SketchCircle(center, radius)
        return circle
    }

    fun arc(p0: Vec2Expr, p1: Vec2Expr, radius: Expr): SketchArc {
        val arc = SketchArc(p0,p1,radius)
        return arc
    }

    fun func(function : (Expr) -> Vec2Expr) : FunctionFigure {
        val function = FunctionFigure(function)
        figures.add(function)
        return function
    }

    fun circle(): SketchCircle {
        return circle(point(), param(1.0))
    }

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): SketchSegment {
        return line(point(x, y), point(x2, y2))
    }

    fun constLine(x: Double = 0.0, y: Double = 0.0, x2: Double = 0.0, y2: Double = 0.0): SketchSegment {
        return line(constPoint(x, y), constPoint(x2, y2))
    }

    fun tangent(circle: SketchConic, line: SketchSegment) {
        addConstraint(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Vec2Expr, line: SketchSegment) {
        addConstraint(PointOnLineMidpoint(point, line))
    }

    fun pointOnLine(point: Vec2Expr, line: SketchSegment) {
        addConstraint(PointOnLine(point, line))
    }

    fun horizontal(line: SketchSegment) {
        addConstraint(Horizontal(line))
    }

    fun vertical(line: SketchSegment) {
        addConstraint(Vertical(line))
    }

    fun pointOnCircle(point: Vec2Expr, circle: SketchConic) {
        addConstraint(PointOnCircle(point, circle))
    }

    fun equalLength(line1: SketchSegment, line2: SketchSegment) {
        addConstraint(Equals(line1.length, line2.length))
    }

    fun len(line1: SketchSegment, length: Expr) {
        addConstraint(Equals(line1.length, length))
    }

    fun angle(line1: SketchSegment, line2: SketchSegment, angle: Expr) {
        addConstraint(InternalAngle(line1, line2, angle))
    }

    fun perp(line1: SketchSegment, line2: SketchSegment){
        addConstraint(Perpendicular(line1, line2))
    }

    fun parallel(line1: SketchSegment, line2: SketchSegment){
        addConstraint(Parallel(line1, line2))
    }

    fun pointOnPoint(point1: Vec2Expr, vec2Expr: Vec2Expr) {
        addConstraint(PointOnPoint(point1, vec2Expr))
    }

    fun eq(point1: Vec2Expr, vec2Expr: Vec2Expr) {
        addConstraint(PointOnPoint(point1, vec2Expr))
    }

    fun eq(value1 : Expr, value2: Expr) {
        addConstraint(Equals(value1, value2))
    }

    fun radius(circle: Circle, value: Expr) {
        addConstraint(Equals(circle.radius, value))
    }

    fun minimize(expr: Expr) {
        addConstraint(Minimize(expr))
    }

    fun addConstraint(constraint: Constraint){
        constraints.add(constraint)
    }

    fun solveImpl(accuracy: Double, params: List<Set<ParamExpr>> = listOf(this.params)) : Boolean{
        val solver = Solver(partStudio.tracker)
        val result = solver.solve(world, params.flatten(), constraints, accuracy)
        return result
    }

    fun solve(accuracy: Double, params: List<Set<ParamExpr>> = listOf(this.params)) {
        val start = System.currentTimeMillis()
        val result = solveImpl(accuracy, params)
        val end = System.currentTimeMillis()
        println("time: ${end - start}ms")
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (element in figures) {
            sb.append(element).append("\n")
        }
        return sb.toString()
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

/*
* fun Canvas.line(line: com.codecad.core.Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}
fun Canvas.circle(circle: com.codecad.core.Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}
* */
