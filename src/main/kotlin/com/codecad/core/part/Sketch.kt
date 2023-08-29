package com.codecad.core.part

import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.brep.Edge
import com.codecad.core.brep.EdgeBound
import com.codecad.core.brep.Vertex
import com.codecad.core.brep.WorkplaneExpr
import com.codecad.core.constraint.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.optimizer.Solver
import com.codecad.core.sketch.collectSurfaces
import com.codecad.core.sketch.createFaceTree
import com.codecad.core.sketch.sketchToLines
import com.codecad.core.sketch.toFace
import java.util.*


class Sketch(val partStudio: PartStudio, val workplane : WorkplaneExpr, val name: String) {
    val params = HashSet<ParamExpr>()
    val constraints = HashSet<Constraint>()
    val world = partStudio.world
    val entities = arrayListOf<Entity>()

    fun param(value : Double = 0.0): ParamExpr {
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

    fun line(a: Vec2Expr, b: Vec2Expr): LineSegmentExpr {
        val segment = LineSegmentExpr(a, b)
        entities.add(segment)
        return segment
    }

    fun circle(center: Vec2Expr, radius: Expr): SketchCircle {
        val circle = SketchCircle(center, radius)
        entities.add(circle)
        return circle
    }

    fun arc(p0: Vec2Expr, p1: Vec2Expr): SketchArc {
        val h = param()

        val arc = SketchArc(p0,p1,h)
        entities.add(arc)
        return arc
    }

    fun circle(): SketchCircle {
        return circle(point(), param())
    }

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 1.0, y2: Double = 1.0): LineSegmentExpr {
        return line(point(x, y), point(x2, y2))
    }

    fun tangent(circle: SketchConic, line : LineSegmentExpr){
        addConstraint(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Vec2Expr, line: LineSegmentExpr) {
        addConstraint(PointOnLineMidpoint(point, line))
    }

    fun pointOnLine(point: Vec2Expr, line: LineSegmentExpr) {
        addConstraint(PointOnLine(point, line))
    }

    fun horizontal(line: LineSegmentExpr) {
        addConstraint(Horizontal(line))
    }

    fun vertical(line: LineSegmentExpr) {
        addConstraint(Vertical(line))
    }

    fun pointOnCircle(point: Vec2Expr, circle: SketchConic) {
        addConstraint(PointOnCircle(point, circle))
    }

    fun equalLength(line1: LineSegmentExpr, line2: LineSegmentExpr) {
        addConstraint(Equals(line1.length, line2.length))
    }

    fun len(line1: LineSegmentExpr, length: Expr) {
        addConstraint(Equals(line1.length, length))
    }

    fun angle(line1: LineSegmentExpr, line2: LineSegmentExpr, angle: Expr) {
        addConstraint(InternalAngle(line1, line2, angle))
    }

    fun perp(line1: LineSegmentExpr, line2: LineSegmentExpr){
        addConstraint(Perpendicular(line1, line2))
    }

    fun parallel(line1: LineSegmentExpr, line2: LineSegmentExpr){
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

    fun init(entity: Entity, vararg values: Double){
        for((index, param) in entity.params.withIndex()){
            if(param is ParamExpr){
                param.value = values[index]
            }
        }
    }

    fun solveImpl(accuracy: Double) : Boolean{
        val result = Solver().solve(world, this.params, constraints, accuracy)
        return result
    }

    fun solve(accuracy: Double) {
        val start = System.currentTimeMillis()
        val result = solveImpl(accuracy)
        val end = System.currentTimeMillis()
        println("time: ${end - start}ms")

        val lines = sketchToLines(this, ignoreConstruction = true)
        val rootHole = createFaceTree(lines)
        val surfaces = collectSurfaces(rootHole)
        for(surface in surfaces){
            val face = surface.toFace(workplane)
            partStudio.addFace(face)
        }
    }

    fun generate(){
        for( figure in entities ){
            when(figure){
                is LineSegmentExpr -> {
                    val projA = workplane.unproject(figure.p0)
                    val projB = workplane.unproject(figure.p1)

                    val lineNew = Edge.line(Vertex(projA), Vertex(projB))
                }
                is SketchCircle -> {
                    val projCenter = workplane.unproject(figure.center)
                    val centerWorkplane = WorkplaneExpr(projCenter, workplane.xAxis, workplane.yAxis )

                    val circle = Edge(
                        Circle(centerWorkplane, figure.radius)
                    )
                }
                is SketchArc -> {
                    val projCenter = workplane.unproject(figure.center)
                    val centerWorkplane = WorkplaneExpr(projCenter, workplane.xAxis, workplane.yAxis )
                    val arc = Edge(
                        Circle(centerWorkplane, figure.radius),
                        EdgeBound(
                            Vertex(workplane.unproject(figure.p0)),
                            Vertex(workplane.unproject(figure.p1))
                        )
                    )
                }
            }
        }
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
