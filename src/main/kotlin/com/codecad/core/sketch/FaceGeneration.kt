package com.codecad.core.sketch

import com.codecad.core.LineSegment
import com.codecad.core.LineSegmentExpr
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.face.SketchEdgeLoop
import com.codecad.core.part.Sketch
import com.codecad.core.rollover
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.abs



enum class FaceType{
    Root, Surface, Hole
}


fun createFaceTree(lines: List<LineSegment>): SketchEdgeLoop {
    val pointMap = HashMap<Vec2, SketchVertex>()
    fun corner(point: Vec2) : SketchVertex {
        return pointMap.computeIfAbsent(point) { SketchVertex(it) }
    }

    val sections = cutLines(lines)
    val edges = HashSet<SketchEdge>()
    for (section in sections) {
        val left = corner(section.p0)
        val right = corner(section.p1)
        val a = SketchEdge(left, right)
        val b = SketchEdge(right, left)

        a.twin = b
        b.twin = a
        left.edges.add(a)
        right.edges.add(b)
        edges.add(a)
        edges.add(b)
    }

    finalizeCorners(pointMap.values)
    return generateFaces(edges)
}


fun finalizeCorners(vertices : Collection<SketchVertex>){
    for(corner in vertices){
        corner.edges.sortWith(RotaryComparator)

        for((top, bottom) in corner.edges.rollover()){
            assert(top.twin.target === bottom.source)
            top.twin.next = bottom
        }
    }
}

fun computeArea(start : SketchEdge) : Double{
    var curr = start
    var area = 0.0
    while(true){
        val currPos = curr.target.point
        area += curr.source.point.crossZ(currPos)
        if(curr.target === start.source) break
        curr = curr.next!!
    }

    return area / 2
}

fun generateFaces(edges: Collection<SketchEdge>) : SketchEdgeLoop {
    val visited = hashSetOf<SketchEdge>()

    val holes = arrayListOf<SketchEdgeLoop>()
    for( root in edges ){
        if(visited.contains(root)) continue

        var hole : SketchEdgeLoop? = null
        val surfaces = arrayListOf<SketchEdgeLoop>()
        val queue = LinkedList<SketchEdge>()

        queue.add(root)
        while( queue.isNotEmpty() ){
            val start = queue.pollFirst()
            if(!visited.add(start)) continue

            val area = computeArea(start)
            val sketchLoop = SketchEdgeLoop(start)
            sketchLoop.area = abs(area)

            if(area > 0){
                sketchLoop.type = FaceType.Surface
                surfaces.add(sketchLoop)
            }else if(hole == null){
                sketchLoop.type = FaceType.Hole
                hole = sketchLoop
            }else{
                throw Error("double hole!")
            }

            for( curr in start.loop() ){
                visited.add(curr)
                val twin = curr.twin
                queue.add(twin)
            }
        }

        if(hole == null) throw Error("No hole found")
        hole.children = surfaces
        holes.add(hole)
    }

    return nestHoles(holes)
}

fun nestHoles(holes : MutableList<SketchEdgeLoop>) : SketchEdgeLoop{
    holes.sortByDescending { it.area }

    val rootSurface = SketchEdgeLoop(SketchEdge.ZERO)
    rootSurface.type = FaceType.Root
    for( hole in holes ){
        nestHoles(hole, rootSurface)
    }

    return rootSurface
}

fun nestHoles(hole : SketchEdgeLoop, parentSurface: SketchEdgeLoop){
    for( rootHole in parentSurface.children){
        if(rootHole.area <= hole.area) continue

        for( surface in rootHole.children ){
            if(surface.area <= hole.area) continue

            if(isPointInPolygon(hole.root.source.point, surface.points)){
                nestHoles(hole, surface)
                return
            }
        }
    }

    parentSurface.area -= hole.area
    parentSurface.children.add(hole)
}

fun collectSurfaces(rootSurface: SketchEdgeLoop) : List<SketchEdgeLoop>{
    val surfaces = mutableListOf<SketchEdgeLoop>()
    collectSurfaces(rootSurface, surfaces)
    return surfaces
}

fun collectSurfaces(parentSurface : SketchEdgeLoop, surfaces : MutableList<SketchEdgeLoop>){
    for( hole in parentSurface.children ){
        surfaces.addAll(hole.children)
        for( surface in hole.children ){
            collectSurfaces(surface, surfaces)
        }
    }
}

fun SketchEdgeLoop.toFace(workplane: Workplane) : Face{
    val points = when (type) {
        FaceType.Root -> emptyList()
        else -> points.toList()
    }

    val surface = PlaneSurface(workplane)
    val faceBounds = arrayListOf<FaceBound>()
    val result = Face(surface, faceBounds)

    val projPoints = points.map { workplane.unproject(it) }.toList().toTypedArray()
    val bound = FaceBound(EdgeLoop.polygon(*projPoints), FaceBoundKind.OuterBound)
    faceBounds.add(bound)

    for( hole in children ){
        val projPoints = hole.points.map { workplane.unproject(it) }.toList().toTypedArray()
        val bound = FaceBound(EdgeLoop.polygon(*projPoints), FaceBoundKind.InnerBound)
        faceBounds.add(bound)
    }
    return result
}

fun isPointInPolygon(point: Vec2, polygon: Iterable<Vec2>): Boolean {
    var windingNumber = 0

    for ((p1, p2) in polygon.rollover()) {
        if (p1.y <= point.y) {
            if (p2.y > point.y && isLeft(p1, p2, point) > 0) {
                windingNumber++
            }
        } else if (p2.y <= point.y && isLeft(p1, p2, point) < 0) {
            windingNumber--
        }
    }

    return windingNumber != 0
}

private fun isLeft(p0: Vec2, p1: Vec2, p2: Vec2): Double {
    return (p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y)
}
/*
fun isPointInPolygon3d(point: Vec2, polygon: Iterable<Vec3>): Boolean {
    var windingNumber = 0

    for ((p1, p2) in polygon.rollover()) {
        if (p1.y <= point.y) {
            if (p2.y > point.y && isLeft(p1, p2, point) > 0) {
                windingNumber++
            }
        } else if (p2.y <= point.y && isLeft(p1, p2, point) < 0) {
            windingNumber--
        }
    }

    return windingNumber != 0
}

private fun isLeft3d(p0: Vec3, p1: Vec3, p2: Vec3, normal : Vec3): Double {
    return (p1 - p0).cross(p2 - p1).dot(normal)
}
*/
fun sketchToLines(sketch: Sketch, ignoreConstruction: Boolean = false) : List<LineSegment>{
    /*val unifier = Unifier<Vec2>{ lhs,rhs ->
        lhs.distance(rhs) < 0.001
    }

    val list = mutableListOf<LineSegment>()
    for(entity in sketch.entities){
        if(entity is LineSegmentExpr){
            list.add(LineSegment(entity.p0.eval(), entity.p1.eval()))
        }
    }

    for( lineSegment in list ){
        unifier.add(lineSegment.p0)
        unifier.add(lineSegment.p1)
    }

    for( lineSegment in list ){
        lineSegment.p0 = unifier.get(lineSegment.p0)
        lineSegment.p1 = unifier.get(lineSegment.p1)
    }

    return list*/

    return emptyList()
}

