package com.codecad.core.sketch

import com.codecad.core.SketchLine
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.face.SketchEdgeLoop
import com.codecad.core.part.Sketch
import com.codecad.core.rollover
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.math.abs


enum class FaceType{
    Root, Surface, Hole
}


data class VertexHelper(val point: Vertex<Vec2>){
    val loops = mutableListOf<Loop<Vec2>>()

    fun addLoop(loop: Loop<Vec2>){
        if(loop.edge.start != point){
            throw RuntimeException("ss")
        }

        loops.add(loop)
    }

    fun finalizeCCW(){
        loops.sortWith(Comparator.comparing({it.edge}, RotaryEdgeComparator))
        for((top, bottom) in loops.rollover()){
            top.twin!!.followedBy(bottom)
        }
    }

    fun finalizeMirrored(){
        val forwards = loops.filter { it.edge.orientation == EdgeOrientation.Forward }
            .sortedBy { System.identityHashCode(it.edge.edge.curve) }

        val backwards = loops.filter { it.edge.orientation == EdgeOrientation.Backward }
            .sortedByDescending { System.identityHashCode(it.edge.edge.curve) }

        for((fwd, bwd) in forwards.zip(backwards)){
            bwd.twin!!.followedBy(fwd)
        }
    }
}

fun createFaceTree(edges: List<Edge<Vec2>>): SketchFace {
    val cutEdges = cutLines(edges)
    val loops = connectVerticesCCW(cutEdges)
    return generateFaces(loops)
}

fun collectEdges(edges: List<Edge<Vec2>>) : Collection<VertexHelper>{
    val helperMap = HashMap<Vertex<Vec2>, VertexHelper>()
    fun createHelper(point: Vertex<Vec2>) : VertexHelper {
        return helperMap.computeIfAbsent(point) { VertexHelper(it) }
    }

    for (cutEdge in edges) {
        val bound = cutEdge.bound ?: continue
        val left = createHelper(bound.start)
        val right = createHelper(bound.end)
        val forwardEdge = OrientedEdge(cutEdge, EdgeOrientation.Forward)
        val backwardEdge = OrientedEdge(cutEdge, EdgeOrientation.Backward)

        val forwardLoop = Loop(forwardEdge)
        val backwardLoop = Loop(backwardEdge)

        forwardLoop.twin = backwardLoop
        backwardLoop.twin = forwardLoop
        left.addLoop(forwardLoop)
        right.addLoop(backwardLoop)
    }

    return helperMap.values
}

fun connectVerticesCCW(edges: List<Edge<Vec2>>) : List<Loop<Vec2>>{
    val helpers = collectEdges(edges)
    helpers.forEach { it.finalizeCCW() }
    return helpers.flatMap { it.loops }
}

fun connectVerticesMirrored(edges: List<Edge<Vec2>>) : List<Loop<Vec2>>{
    val helpers = collectEdges(edges)
    helpers.forEach { it.finalizeMirrored() }
    return helpers.flatMap { it.loops }.filter { it.edge.orientation == EdgeOrientation.Forward }
}


fun generateFaces(loops: Collection<Loop<Vec2>>) : SketchFace {
    val faceBounds = arrayListOf<FaceBound<Vec2>>()

    fun createBound(loop: Loop<Vec2>){
        faceBounds.add(FaceBound(loop, FaceBoundKind.OuterBound))
    }

    val queue = LinkedList(loops)
    val initMarker = Marker()
    for( loop in loops ){
        loop.marker = initMarker
    }

    while( queue.isNotEmpty() ){
        var currentLoop = queue.pollFirst()
        if(currentLoop.marker !== initMarker) continue

        val loopMarker = Marker()
        currentLoop.marker = loopMarker
        while(true){
            val twin = currentLoop.twin!!
            val nextLoop = currentLoop.next

            if(twin.marker === loopMarker){
                val beforeCurrent = currentLoop.prev
                if(beforeCurrent !== twin){
                    val afterTwin = twin.next
                    beforeCurrent.followedBy(afterTwin)
                    twin.followedBy(currentLoop)
                    createBound(beforeCurrent)
                }

                val beforeTwin = twin.prev
                beforeTwin.followedBy(nextLoop)
            }

            if(nextLoop.marker === loopMarker){
                if(nextLoop.next !== nextLoop.twin){
                    createBound(nextLoop)
                }
                break
            }

            nextLoop.marker = loopMarker
            currentLoop = nextLoop
        }
    }

    return SketchFace(faceBounds)
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

fun SketchFace.toFace(workplane: Workplane<Vec3>) : Face{
    val surface = PlaneSurface(workplane)
    val faceBounds = bounds.map { faceBound ->
        val loop = faceBound.loop.project {
            workplane.unproject(it)
        }

        FaceBound(loop, faceBound.sense)
    }

    return Face(surface, faceBounds)
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
    return (p1 - p0).crossZ(p2 - p0)
}


fun Loop<Vec2>.isInside(point: Vec2): Boolean {
    var windingNumber = 0

    if(isClosed()){
        val circle = edge.edge.curve as Circle
        return (point - circle.workplane.origin).length() <= circle.radius
    }

    for (loop in this) {
        val edge = loop.edge.edge
        val curve = edge.curve

        val bound = edge.bound!!

        when(curve){
            is Line -> {
                val start = bound.start.point
                val end = bound.end.point

                if (start.y <= point.y) {
                    if (end.y > point.y && isLeft(start, end, point) > 0) {
                        windingNumber++
                    }
                } else if (end.y <= point.y && isLeft(start, end, point) < 0) {
                    windingNumber++
                }
            }
            is Circle -> {
                windingNumber += countBarriers(curve, bound, point)
            }
        }
    }

    return windingNumber % 2 == 1
}

private fun countBarriers(
    curve: Circle<Vec2>,
    bound: EdgeBound<Vec2>,
    point: Vec2,
) : Int {

    val start = bound.start.point
    val end = bound.end.point

    var windingNumber = 0
    val radius = curve.radius
    val center = curve.workplane.origin

    if (abs(center.y - point.y) > radius) {
        return 0
    }

    val isCenterLeft = point.x < center.x
    val isInside = center.distanceTo(point) <= radius

    if (isCenterLeft && !isInside) {
        return 0
    }

    if(bound.sense == Sense.Same){
        if (start.x < center.x || start.y > center.y) {
            if (center.x < end.x) {
                if (start.y >= point.y) {
                    windingNumber++
                }

                if (end.y > point.y && !isInside) {
                    windingNumber++
                }
            } else {
                if (start.y >= point.y && point.y > end.y) {
                    windingNumber++
                }
            }
        } else {
            if (end.x < center.x) {
                if (start.y <= point.y && !isInside) {
                    windingNumber++
                }

                if (end.y < point.y) {
                    windingNumber++
                }
            } else if (!isInside) {
                if (end.y > point.y && point.y >= start.y) {
                    windingNumber++
                }
            }
        }
    }else{
        if (start.x < center.x || start.y > center.y) {
            if (center.x < end.x) {
                if (start.y <= point.y) {
                    windingNumber++
                }

                if (end.y < point.y && !isInside) {
                    windingNumber++
                }
            } else {
                if (end.y > point.y && point.y >= start.y) {
                    windingNumber++
                }
            }
        } else {
            if (end.x < center.x) {
                if (start.y >= point.y && !isInside) {
                    windingNumber++
                }

                if (end.y > point.y) {
                    windingNumber++
                }
            } else if (!isInside) {
                if (start.y >= point.y && point.y > end.y) {
                    windingNumber++
                }
            }
        }
    }

    return windingNumber
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
fun sketchToLines(sketch: Sketch, ignoreConstruction: Boolean = false) : List<SketchLine>{
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

