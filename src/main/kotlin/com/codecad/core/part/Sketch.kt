package com.codecad.core.part

import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.constraint.*
import com.codecad.core.face.entity.*
import com.codecad.core.face.entity.curve.Circle
import com.codecad.core.optimizer.Solver
import java.util.*


class Sketch(val partStudio: PartStudio, val workplane : Workplane, val name: String) {
    val params = HashSet<ParamExpr>()
    val constraints = HashSet<Constraint>()
    val world = partStudio.world
    val figures = arrayListOf<Figure>()

    fun generate(){
        for( figure in figures ){
            when(figure){
                is SketchSegment -> {
                    val projA = workplane.projectTo(figure.p0)
                    val projB = workplane.projectTo(figure.p1)

                    val lineNew = Edge(
                        Line(projA, projB - projA),
                        EdgeBound(Vertex(projA), Vertex(projB))
                    )
                }
                is SketchCircle -> {
                    val projCenter = workplane.projectTo(figure.center)
                    val centerWorkplane = Workplane(projCenter, workplane.axisA, workplane.axisB )

                    val circle = Edge(
                        Circle(centerWorkplane, figure.radius)
                    )
                }
                is SketchArc -> {
                    val projCenter = workplane.projectTo(figure.center)
                    val centerWorkplane = Workplane(projCenter, workplane.axisA, workplane.axisB )
                    val arc = Edge(
                        Circle(centerWorkplane, figure.radius),
                        EdgeBound(
                            Vertex(workplane.projectTo(figure.p0)),
                            Vertex(workplane.projectTo(figure.p1))
                        )
                    )
                }
            }
        }
    }

    fun param(value: Double = 0.0): ParamExpr {
        val param = ParamExpr( world, value )
        params.add(param)
        return param
    }

    fun literal(value: Double = 0.0) : Expr {
        return world.literal(value)
    }

    fun constPoint(x: Double = 0.0, y: Double = 0.0): Vec2Expr {
        val a = literal(x)
        val b = literal(y)
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

    fun line(a: Vec2Expr, b: Vec2Expr): SketchSegment {
        val segment = SketchSegment(a, b)
        figures.add(segment)
        return segment
    }

    fun circle(center: Vec2Expr, radius: Expr): SketchCircle {
        val circle = SketchCircle(center, radius)
        figures.add(circle)
        return circle
    }

    fun arc(p0: Vec2Expr, p1: Vec2Expr): SketchArc {
        val h = param(1.0)

        val arc = SketchArc(p0,p1,h)
        figures.add(arc)
        return arc
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

    fun tangent(circle: SketchConic, line : SketchSegment){
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

    fun solveImpl(accuracy: Double, params: Set<ParamExpr> = this.params) : Boolean{
        val result = Solver().solve(world, params, constraints, accuracy)
        return result
    }

    fun solve(accuracy: Double, params: Set<ParamExpr> = this.params) {
        val start = System.currentTimeMillis()
        val result = solveImpl(accuracy, params)
        val end = System.currentTimeMillis()
        println("time: ${end - start}ms")
    }
/*
    override fun toString(): String {
        val sb = StringBuilder()
        for (element in figures) {
            sb.append(element).append("\n")
        }
        return sb.toString()
    }*/
}
/*

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
}*/

/*
* fun Canvas.line(line: com.codecad.core.Line){
    line(line.a.x.value,line.a.y.value, line.b.x.value,line.b.y.value)
}
fun Canvas.circle(circle: com.codecad.core.Circle){
    circle(circle.center.x.value,circle.center.y.value, circle.rad.value)
}
* */
