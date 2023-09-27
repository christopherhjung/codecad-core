package com.codecad.core.part

import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.ast.vec.Vec2Expr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.constraint.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.optimizer.Solver
import com.codecad.core.sketch.collectSurfaces
import com.codecad.core.sketch.createFaceTree
import com.codecad.core.sketch.sketchToLines
import com.codecad.core.sketch.toFace
import java.util.*


class Sketch(val partStudio: PartStudio, val workplane : Workplane<Vec3>, val name: String) {
    val params = HashSet<ParamExpr>()
    val constraints = HashSet<Constraint>()
    val world = partStudio.world
    val entities = arrayListOf<SketchEntityExpr>()

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

    fun line(a: Vec2Expr, b: Vec2Expr): SketchLineExpr {
        val segment = SketchLineExpr(a, b)
        entities.add(segment)
        return segment
    }

    fun circle(center: Vec2Expr, radius: Expr): SketchCircleExpr {
        val circle = SketchCircleExpr(center, radius)
        entities.add(circle)
        return circle
    }

    fun arc(p0: Vec2Expr, p1: Vec2Expr): SketchArcExpr {
        val h = param()

        val arc = SketchArcExpr(p0,p1,h)
        entities.add(arc)
        return arc
    }

    fun circle(): SketchCircleExpr {
        return circle(point(), param())
    }

    fun line(x: Double = 0.0, y: Double = 0.0, x2: Double = 1.0, y2: Double = 1.0): SketchLineExpr {
        return line(point(x, y), point(x2, y2))
    }

    fun tangent(circle: SketchConicExpr, line : SketchLineExpr){
        addConstraint(CircleTangent(circle, line))
    }

    fun pointOnLineMidpoint(point: Vec2Expr, line: SketchLineExpr) {
        addConstraint(PointOnLineMidpoint(point, line))
    }

    fun pointOnLine(point: Vec2Expr, line: SketchLineExpr) {
        addConstraint(PointOnLine(point, line))
    }

    fun horizontal(line: SketchLineExpr) {
        addConstraint(Horizontal(line))
    }

    fun vertical(line: SketchLineExpr) {
        addConstraint(Vertical(line))
    }

    fun pointOnCircle(point: Vec2Expr, circle: SketchConicExpr) {
        addConstraint(PointOnCircle(point, circle))
    }

    fun equalLength(line1: SketchLineExpr, line2: SketchLineExpr) {
        addConstraint(Equals(line1.length, line2.length))
    }

    fun len(line1: SketchLineExpr, length: Expr) {
        addConstraint(Equals(line1.length, length))
    }

    fun angle(line1: SketchLineExpr, line2: SketchLineExpr, angle: Expr) {
        addConstraint(InternalAngle(line1, line2, angle))
    }

    fun perp(line1: SketchLineExpr, line2: SketchLineExpr){
        addConstraint(Perpendicular(line1, line2))
    }

    fun parallel(line1: SketchLineExpr, line2: SketchLineExpr){
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

    fun radius(circle: SketchCircleExpr, value: Expr) {
        addConstraint(Equals(circle.radius, value))
    }

    fun minimize(expr: Expr) {
        addConstraint(Minimize(expr))
    }

    fun addConstraint(constraint: Constraint){
        constraints.add(constraint)
    }

    fun init(entity: SketchEntityExpr, vararg values: Double){
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
                is SketchLineExpr -> {
                    val projA = workplane.unproject(figure.p0.eval())
                    val projB = workplane.unproject(figure.p1.eval())

                    val lineNew = Edge.line(Vertex(projA), Vertex(projB))
                }
                is SketchCircleExpr -> {
                    val projCenter = workplane.unproject(figure.center.eval())
                    val centerWorkplane = Workplane(projCenter, workplane.normal, workplane.x)

                    val circle = Edge(
                        Circle(centerWorkplane, figure.radius.evalDouble())
                    )
                }
                is SketchArcExpr -> {
                    val projCenter = workplane.unproject(figure.center.eval())
                    val centerWorkplane = Workplane(projCenter, workplane.normal, workplane.x)
                    val arc = Edge(
                        Circle(centerWorkplane, figure.radius.evalDouble()),
                        EdgeBound(
                            Vertex(workplane.unproject(figure.p0.eval())),
                            Vertex(workplane.unproject(figure.p1.eval()))
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
