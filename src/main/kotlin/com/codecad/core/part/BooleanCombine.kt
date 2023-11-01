package com.codecad.core.part

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.*
import com.codecad.core.sketch.EPSILON
import com.codecad.core.sketch.VertexHelper
import com.codecad.core.sketch.isPointInPolygon
import com.codecad.core.volume.Volume

enum class CombineKind{
    Add, Subtract, Intersect
}
data class Intersection(val point: Vec3, val face: Face, val loop: Loop<Vec3>)

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
    private val edgeSplitMap = hashMapOf<Edge<Vec3>, MutableList<Split>>()
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

        connect()

        return lhsVolume
    }

    private fun connect(){
        for((edge, edgeSplits) in edgeSplitMap.entries){
            val line = edge.curve as Line
            edgeSplits.sortBy { line.direction.dot(it.vertex.point - line.origin) }

            val bound = edge.bound
            var lastVertex = bound.start
            var lastLoop : Loop<Vec3>? = null
            for(edgeSplit in edgeSplits){
                val currentVertex = edgeSplit.vertex
                val segEdge = Edge(line, EdgeBound(lastVertex, currentVertex))
                val segLoop = Loop.twin(segEdge)
                lastLoop?.followedBy(segLoop)

                for((loop, branch) in edgeSplit.branches.entries){
                    if(loop.edge.orientation == EdgeOrientation.Forward){

                    }else{

                    }
                }

                lastVertex = currentVertex
                lastLoop = segLoop
            }

            val segEdge = Edge(line, EdgeBound(lastVertex, bound.end))
            val segLoop = Loop.twin(segEdge)
            lastLoop?.followedBy(segLoop)
        }
    }


    fun intersectFace(lhsFace: Face, rhsFace: Face){
        val interCurves = SurfaceIntersect.intersect(lhsFace.surface, rhsFace.surface)

        interCurves.forEach {
            createEdges(it, lhsFace, rhsFace)
        }
    }

    private fun addSplit(loop: Loop<Vec3>, pos : Vec3 ) : Split{
        val edge = loop.edge.edge
        val bound = edge.bound

        for( boundVertex in bound ){
            if(boundVertex.point.near(pos, EPSILON)){
                return vertexSplitMap.computeIfAbsent(boundVertex){
                    Split(boundVertex)
                }
            }
        }

        val list = edgeSplitMap.computeIfAbsent(edge){ mutableListOf() }
        list.forEach {
            val vertex = it.vertex
            if(vertex.point.near(pos, EPSILON)){
                return it
            }
        }

        val split = Split(Vertex(pos))
        list.add(split)
        return split
    }

    fun createEdges(curve: Curve<Vec3>, lhsFace: Face, rhsFace: Face){
        val inters = findIters(curve, lhsFace, rhsFace)
        var lhsActive = false
        var rhsActive = false
        var last : Intersection? = null
        for( inter in inters ){
            if(lhsActive && rhsActive){
                last!!
                val lastVertex = addSplit(last.loop, last.point)
                val interVertex = addSplit(inter.loop, inter.point)
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
        intersections.sortBy { line.direction.dot(it.point - line.origin) }
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

