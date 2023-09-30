package com.codecad.core.sketch

import com.codecad.core.SketchLine
import com.codecad.core.ast.vec.Vec2
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.face.SketchEdgeLoop
import com.codecad.core.part.Sketch
import com.codecad.core.rollover
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap


enum class FaceType{
    Root, Surface, Hole
}


data class VertexHelper(val point: Vertex<Vec2>){
    val loops = mutableListOf<Loop<Vec2>>()

    fun addLoop(edge: Loop<Vec2>){
        if(edge.edge.start !== point){
            throw RuntimeException("ss")
        }

        loops.add(edge)
    }
}

fun createFaceTree(edges: List<Edge<Vec2>>): SketchFace {
    val cutEdges = cutLines(edges)
    val helpers = arrayListOf<VertexHelper>()
    val helperMap = HashMap<Vertex<Vec2>, VertexHelper>()
    fun createHelper(point: Vertex<Vec2>) : VertexHelper {
        return helperMap.computeIfAbsent(point) { VertexHelper(it) }
    }

    for (cutEdge in cutEdges) {
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
        helpers.add(left)
        helpers.add(right)
    }

    finalizeCorners(helperMap.values)
    return generateFaces(helperMap.values.flatMap { it.loops })
}


fun finalizeCorners(helpers : Collection<VertexHelper>){
    for(helper in helpers){
        helper.loops.sortWith(Comparator.comparing({it.edge}, RotaryEdgeComparator))
        for((top, bottom) in helper.loops.rollover()){
            //assert(top.twin.target === bottom.source)
            top.twin!!.let {
                it.next = bottom
                bottom.prev = it
            }
        }
    }
}
/*
fun computeArea(start : VertexHelper) : Double{
    var curr = start
    var area = 0.0
    while(true){
        val currPos = curr.target.point
        area += curr.source.point.crossZ(currPos)
        if(curr.target === start.source) break
        curr = curr.next!!
    }

    return area / 2
}*/

fun generateFaces(loops: Collection<Loop<Vec2>>) : SketchFace {
    val faceBounds = arrayListOf<FaceBound<Vec2>>()
    val queue = LinkedList(loops)
    val loopMarker = Marker()

    while( queue.isNotEmpty() ){
        val start = queue.pollFirst()
        if(start.marker === loopMarker) continue
        start.marker = loopMarker

        var endLoop = start.prev
        if(endLoop.prev === start){
            endLoop.marker = loopMarker
            continue
        }

        val edgeMarker = Marker()
        var currentLoop = start
        /*var endLoop : Loop<Vec2> = null
        while(true){
            var prevLoop = currentLoop.prev
            endLoop = prevLoop
            if(prevLoop.prev === currentLoop){
                endLoop.marker = loopMarker
                endLoop = null
                break
            }

            val currentEdge = currentLoop.edge.edge
            val prevEdge = prevLoop.edge.edge

            if(currentEdge === prevEdge){
                val nextLoop = currentLoop.next
                val beforePrev = prevLoop.prev

                nextLoop.prev = beforePrev
                beforePrev.next = nextLoop
            }
        }

        if(endLoop == null) break*/

        while(true){
            var nextLoop = currentLoop.next
            val currentEdge = currentLoop.edge.edge
            println(currentLoop.edge)
            System.out.flush()
            System.out.flush()

            if(currentEdge.marker === edgeMarker){
                if(nextLoop.marker === loopMarker){
                    val beforeCurrent = currentLoop.prev
                    val afterNext = nextLoop.next
                    beforeCurrent.next = afterNext
                    afterNext.prev = beforeCurrent
                    faceBounds.add(FaceBound(beforeCurrent, FaceBoundKind.OuterBound))
                    break
                }

                val twin = currentLoop.twin!!
                val twinPrev = twin.prev
                twinPrev.next = nextLoop
                nextLoop.prev = twinPrev

                if(twin.next !== currentLoop){
                    twin.prev = currentLoop
                    currentLoop.next = twin
                    faceBounds.add(FaceBound(currentLoop, FaceBoundKind.OuterBound))
                }


                //println("------------------------")
                //println("cut out ${currentLoop.edge}")
                //println(nextLoop.count())
                //println(nextLoop)
                //System.out.flush()
                endLoop = twinPrev
            }else if(currentLoop === endLoop){
                if(currentLoop !== currentLoop.next){
                    faceBounds.add(FaceBound(currentLoop, FaceBoundKind.OuterBound))
                }

                break
            }

            currentEdge.marker = edgeMarker
            nextLoop.marker = loopMarker
            currentLoop = nextLoop
        }

        println(faceBounds)
    }

    val face = SketchFace(faceBounds)
    return face//nestHoles(holes)
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

