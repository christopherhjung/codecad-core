package com.codecad.core.part

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Curve
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.*
import com.codecad.core.sketch.EPSILON
import com.codecad.core.sketch.isPointInPolygon
import com.codecad.core.volume.Volume

enum class CombineKind{
    Add, Subtract, Intersect
}

class Intersection(var lhsFace : Face, var rhsFace : Face, var curve : Curve<Vec3>){
}

object BooleanCombine{
    val sectionMap = HashMap<Edge<Vec3>, MutableList<Vertex<Vec3>>>()

    fun addSection(entity: Edge<Vec3>, pos : Vec3 ) : Vertex<Vec3>{
        val bound = entity.bound
        if(bound.start.point.near(pos, EPSILON) ){
            return bound.start
        }

        if(bound.end.point.near(pos, EPSILON) ){
            return bound.end
        }

        val list = sectionMap.computeIfAbsent(entity){ mutableListOf() }
        list.forEach {
            if(it.point.near(pos, EPSILON) ){
                return it
            }
        }

        val vertex = Vertex(pos)
        list.add(vertex)
        return vertex
    }

    class Section(val lhsLoop : Loop<Vec3>, val rhsLoop: Loop<Vec3>, val p : Vertex<Vec3>)

    fun addSection(loop: Loop<Vec3>, pos : Vec3 ) : Vertex<Vec3>{
        val edge = loop.edge.edge
        val bound = edge.bound
        if(bound.start.point.near(pos, EPSILON) ){
            return bound.start
        }

        if(bound.end.point.near(pos, EPSILON) ){
            return bound.end
        }

        val list = sectionMap.computeIfAbsent(edge){ mutableListOf() }
        list.forEach {
            if(it.point.near(pos, EPSILON) ){
                return it
            }
        }

        val vertex = Vertex(pos)
        list.add(vertex)
        return vertex
    }

    fun combine(kind: CombineKind, lhsVolume : Volume, rhsVolume: Volume) : Volume{
        val edges = arrayListOf<Edge<Vec3>>()
        for( lhsShell in lhsVolume.shells ){
            for( lhsFace in lhsShell.faces ){

                for( rhsShell in rhsVolume.shells ){
                    for( rhsFace in rhsShell.faces ){
                        edges.addAll(intersectFace(lhsFace, rhsFace))
                    }
                }
            }
        }

        return lhsVolume
    }

    data class Intersection(val point: Vec3, val face: Face, val loop: Loop<Vec3>, val edge: Edge<Vec3>)

    fun intersectFace(lhsFace: Face, rhsFace: Face) : List<Edge<Vec3>>{
        val interCurves = SurfaceIntersect.intersect(lhsFace.surface, rhsFace.surface)

        val edges = interCurves.flatMap {
            createEdges(it, lhsFace, rhsFace)
        }

        return edges
    }

    private fun findIters(
        interCurve: Curve<Vec3>,
        lhsFace: Face,
        rhsFace: Face
    ): ArrayList<Intersection> {
        val points = arrayListOf<Intersection>()

        fun scan(face: Face) {
            for (bound in face.bounds) {
                for (edgeLoop in bound.loop) {
                    val orientedEdge = edgeLoop.edge
                    val edge = orientedEdge.edge

                    val inters =
                        CurveEdgeIntersect.intersect(interCurve, face.surface, edge)

                    inters.forEach {
                        points.add(Intersection(it, face, edgeLoop, edge))
                    }
                }
            }
        }

        scan(lhsFace)
        scan(rhsFace)

        val line = interCurve as Line<Vec3>
        points.sortBy { line.direction.dot(it.point - line.origin) }
        return points
    }

    fun createEdges(curve: Curve<Vec3>, lhsFace: Face, rhsFace: Face) : List<Edge<Vec3>>{
        val inters = findIters(curve, lhsFace, rhsFace)
        var lhsActive = false
        var rhsActive = false
        var last : Intersection? = null
        val edges = arrayListOf<Edge<Vec3>>()
        for( inter in inters ){
            if(lhsActive && rhsActive){
                last!!
                val lastVertex = addSection(last.loop, last.point)
                val interVertex = addSection(inter.loop, inter.point)
                val edge = Edge(curve, EdgeBound(lastVertex, interVertex))

                edges.add(edge)
            }

            if(lhsFace === inter.face){
                lhsActive = !lhsActive
            }else if(rhsFace === inter.face){
                rhsActive = !rhsActive
            }

            last = inter
        }
        assert(!lhsActive && !rhsActive)
        return edges
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

