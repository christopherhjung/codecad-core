package com.codecad.core.part

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.*
import com.codecad.core.sketch.isPointInPolygon
import com.codecad.core.volume.Volume

enum class CombineKind{
    Add, Subtract, Intersect
}
data class Intersection(val vertex: Vertex<Vec3>, val face: Face, val loop: Loop<Vec3>)

open class Split(val vertex: Vertex<Vec3>){
    val branches = hashMapOf<Loop<Vec3>, MutableList<Loop<Vec3>>>()
    open fun addBranch(loop: Loop<Vec3>, branch: Loop<Vec3>){
        val key = if(loop.edge.bound.end === vertex){
            loop.next
        }else{
            loop
        }

        branches.computeIfAbsent(key){ mutableListOf() }.add(branch)
    }
}


object BooleanCombine{
    private val vertexSplitMap = hashMapOf<Vertex<Vec3>, Split>()

    fun combine(kind: CombineKind, lhsVolume : Volume, rhsVolume: Volume) : Volume{
        for( lhsShell in lhsVolume.shells ){
            for( lhsFace in lhsShell.faces ){

                for( rhsShell in rhsVolume.shells ){
                    for( rhsFace in rhsShell.faces ){
                        intersectFace(lhsFace, rhsFace)
                    }
                }
            }
        }

        return lhsVolume
    }

    fun intersectFace(lhsFace: Face, rhsFace: Face){
        val interCurves = SurfaceIntersect.intersect(lhsFace.surface, rhsFace.surface)

        interCurves.forEach {
            createEdges(it, lhsFace, rhsFace)
        }
    }

    private fun addSplit(loop: Loop<Vec3>, vertex: Vertex<Vec3> ) : Split{
        val bound = loop.edge.edge.bound
        for( boundVertex in bound ){
            if(boundVertex === vertex){
                return vertexSplitMap.computeIfAbsent(boundVertex){
                    Split(it)
                }
            }
        }

        splitEdge(loop, vertex)
        val split = Split(vertex)
        vertexSplitMap[vertex] = split
        return split
    }

    private fun splitEdge(
        loop: Loop<Vec3>,
        vertex: Vertex<Vec3>,
    ) {
        val edge = loop.edge.edge
        val bound = edge.bound
        val curve = edge.curve
        val firstEdge = Edge(curve, EdgeBound(bound.start, vertex))
        val secondEdge = Edge(curve, EdgeBound(vertex, bound.end))

        val twin = loop.twin!!
        val orient = loop.edge.orientation
        val twinOrient = twin.edge.orientation

        val firstLoop = Loop.twin(firstEdge, orient, twinOrient)
        val secondLoop = Loop.twin(secondEdge, orient, twinOrient)
        val face = loop.face
        val twinFace = twin.face
        firstLoop.face = face
        secondLoop.face = face
        firstLoop.twin!!.face = twinFace
        secondLoop.twin!!.face = twinFace

        if (orient == EdgeOrientation.Forward) {
            firstLoop.followedBy(secondLoop)

            loop.prev.followedBy(firstLoop)
            secondLoop.followedBy(loop.next)
        } else {
            secondLoop.followedBy(firstLoop)

            loop.prev.followedBy(secondLoop)
            firstLoop.followedBy(loop.next)
        }

        if (twinOrient == EdgeOrientation.Forward) {
            firstLoop.twin!!.followedBy(secondLoop.twin!!)

            twin.prev.followedBy(firstLoop.twin!!)
            secondLoop.twin!!.followedBy(twin.next)
        } else {
            secondLoop.twin!!.followedBy(firstLoop.twin!!)

            twin.prev.followedBy(secondLoop.twin!!)
            firstLoop.twin!!.followedBy(twin.next)
        }

        face.bounds = face.bounds.map {
            if (it.loop === loop) {
                FaceBound(firstLoop, it.sense)
            } else {
                it
            }
        }

        twinFace.bounds = twinFace.bounds.map {
            if (it.loop === twin) {
                FaceBound(firstLoop.twin!!, it.sense)
            } else {
                it
            }
        }
    }

    fun createEdges(curve: Curve<Vec3>, lhsFace: Face, rhsFace: Face){
        val inters = findIters(curve, lhsFace, rhsFace)
        var lhsActive = false
        var rhsActive = false
        var last : Intersection? = null
        for( inter in inters ){
            if(inter.vertex === last?.vertex){
                continue
            }

            if(lhsActive && rhsActive){
                last!!
                val lastVertex = addSplit(last.loop, last.vertex)
                val interVertex = addSplit(inter.loop, inter.vertex)
                val edge = Edge(curve, EdgeBound(lastVertex.vertex, interVertex.vertex))

                val loop = Loop.twin(edge)
                lastVertex.addBranch(last.loop, loop)
                interVertex.addBranch(inter.loop, loop.twin!!)
            }

            if(lhsFace === inter.face){
                lhsActive = !lhsActive
            }else if(rhsFace === inter.face){
                rhsActive = !rhsActive
            }

            last = inter
        }
        assert(!lhsActive && !rhsActive)
    }

    private fun findIters(
        interCurve: Curve<Vec3>,
        lhsFace: Face,
        rhsFace: Face
    ): ArrayList<Intersection> {
        val intersections = arrayListOf<Intersection>()

        fun scan(face: Face) {
            for (bound in face.bounds) {
                for (loop in bound.loop) {
                    val orientedEdge = loop.edge
                    val edge = orientedEdge.edge

                    CurveEdgeIntersect.intersect(interCurve, face.surface, edge).map {
                        Intersection(it, face, loop)
                    }.forEach(intersections::add)
                }
            }
        }

        scan(lhsFace)
        scan(rhsFace)

        val line = interCurve as Line<Vec3>
        intersections.sortBy { line.direction.dot(it.vertex.point - line.origin) }
        return intersections
    }

    fun isInside(point: Vec3, rhsFace: Face) : Boolean{
        val planeSurface = rhsFace.surface as PlaneSurface

        for(bound in rhsFace.bounds){
            if((bound.sense == FaceBoundKind.OuterBound) != isInside(point, bound, planeSurface.workplane)){
                return false
            }
        }

        return true
    }

    fun isInside(point: Vec3, bound: FaceBound<Vec3>, workplane : Workplane<Vec3>) : Boolean{
        val projPoint = workplane.project2d(point)
        val points = bound.loop.map { workplane.project2d(it.edge.start.point) }
        return isPointInPolygon(projPoint, points)
    }


}

