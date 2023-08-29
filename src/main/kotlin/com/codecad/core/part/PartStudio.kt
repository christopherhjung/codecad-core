package com.codecad.core.part

import com.codecad.core.*
import com.codecad.core.brep.Face
import com.codecad.core.volume.Volume

class Context{
    val volumes = arrayListOf<Volume>()
}

class PartStudio(val world : World = World()){
    val sketches: MutableList<Sketch> = mutableListOf()
    val faces = arrayListOf<Face>()
    val volumes = arrayListOf<Volume>()

    fun addFace(face: Face){
        //faces.add(face)

        val volume = Extruder()
            .extrude(face, world.DirectionZ, world.literal(4.0))
        addVolume(volume)
    }

    fun addVolume(volume: Volume){
        volumes.add(volume)
    }

/*
    val volumes = mutableListOf<Volume>()
    fun extrude(sketch: Sketch, height: Expr, plane: DirectedPlane = DirectedPlane.XY) {
        val lines = sketchToLines(sketch, ignoreConstruction = true)
        val rootHole = createFaceTree(lines)
        val surfaces = collectSurfaces(rootHole)

        val rootFaces = surfaces.first()
        volumes.add(Extrude(rootFaces, plane,  height).extrude())
    }

    fun extrudePos(sketch: Sketch, pos : Vec2Expr, height: Expr, plane: DirectedPlane = DirectedPlane.XY) {
        val lines = sketchToLines(sketch, ignoreConstruction = true)
        val rootHole = createFaceTree(lines)
        val surfaces = collectSurfaces(rootHole)
        val faceFinder = FaceFinder(surfaces)
        faceFinder.find(pos.eval())?.let {
            volumes.add(Extrude(it, plane,  height).extrude())
        }
    }*/

    /*
    fun vertex(){
        val vertex = Vertex(point())
    }

    fun param(value: Double = 0.0): ParamExpr {
        val param = ParamExpr( world, value )
        params.add(param)
        return param
    }

    fun literal(value: Double = 0.0) : Expr {
        return world.literal(value)
    }

    fun constPoint(x: Double = 0.0, y: Double = 0.0): Vec3Expr {
        val a = literal(x)
        val b = literal(y)
        val point = world.vec2(a, b)
        return point
    }

    fun point(x: Expr, y: Expr): Vec3Expr {
        val point = world.vec2(x,y)
        return point
    }

    fun point(x: Double = 0.0, y: Double = 0.0): Vec3Expr {
        val a = param(x)
        val b = param(y)
        return point(a,b)
    }

    fun line(a: Vec2Expr, b: Vec2Expr, type: LineType = LineType.Normal): SketchSegment {
        val line = SketchSegment(a, b)
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
        val result = Solver().solve(world, params.flatten(), constraints, accuracy)
        return result
    }

    fun solve(accuracy: Double, params: List<Set<ParamExpr>> = listOf(this.params)) {
        val start = System.currentTimeMillis()
        val result = solveImpl(accuracy, params)
        val end = System.currentTimeMillis()
        println("time: ${end - start}ms")
    }
*/
}
