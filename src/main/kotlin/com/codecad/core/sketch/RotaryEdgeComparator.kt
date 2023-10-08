package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line

fun startTangent(orientedEdge: OrientedEdge<Vec2>) : Vec2{
    return if(orientedEdge.orientation == EdgeOrientation.Forward){
        startTangent(orientedEdge.edge)
    }else{
        endTangent(orientedEdge.edge)
    }
}

fun startNormal(orientedEdge: OrientedEdge<Vec2>) : Vec2{
    return if(orientedEdge.orientation == EdgeOrientation.Forward){
        startNormal(orientedEdge.edge)
    }else{
        endNormal(orientedEdge.edge).negate()
    }
}

fun endNormal(orientedEdge: OrientedEdge<Vec2>) : Vec2{
    return if(orientedEdge.orientation == EdgeOrientation.Forward){
        endNormal(orientedEdge.edge)
    }else{
        startNormal(orientedEdge.edge).negate()
    }
}

fun startTangent(edge: Edge<Vec2>) : Vec2{
    return when(val curve = edge.curve){
        is Line -> curve.direction
        is Circle -> {
            val center = curve.workplane.origin
            val bound = edge.bound ?: return Vec2.Zero
            (center - bound.start.point).apply {
                if(bound.sense == Sense.Same){
                    rotateCCW()
                }else{
                    rotateCW()
                }
            }
        }
        else -> Vec2.Zero
    }
}

fun endTangent(edge: Edge<Vec2>) : Vec2{
    return when(val curve = edge.curve){
        is Line -> curve.direction.negate()
        is Circle -> {
            val center = curve.workplane.origin
            val bound = edge.bound ?: return Vec2.Zero
            (center - bound.end.point).apply {
                if(bound.sense == Sense.Same){
                    rotateCW()
                }else{
                    rotateCCW()
                }
            }
        }
        else -> Vec2.Zero
    }
}

fun startNormal(edge: Edge<Vec2>) : Vec2{
    return when(val curve = edge.curve){
        is Line -> curve.direction.rotateCW()
        is Circle -> {
            val bound = edge.bound ?: return Vec2.Zero
            circleNormal(curve, bound.start, bound.sense)
        }
        else -> Vec2.Zero
    }
}

fun endNormal(edge: Edge<Vec2>) : Vec2{
    return when(val curve = edge.curve){
        is Line -> curve.direction.rotateCW()
        is Circle -> {
            val bound = edge.bound ?: return Vec2.Zero
            circleNormal(curve, bound.end, bound.sense)
        }
        else -> Vec2.Zero
    }
}

fun circleNormal(curve: Circle<Vec2>, boundVertex: Vertex<Vec2>, sense: Sense) : Vec2{
    val center = curve.workplane.origin
    val toCenter = (boundVertex.point - center).normalized()

    return if(sense == Sense.Same){
        toCenter
    }else{
        toCenter.negate()
    }
}

object RotaryEdgeComparator : Comparator<OrientedEdge<Vec2>>{
    override fun compare(lhs: OrientedEdge<Vec2>, rhs: OrientedEdge<Vec2>): Int {
        return Vec2.rotaryCmp(Vec2.DirX, startTangent(lhs), startTangent(rhs))
    }
}

class RotaryVertexComparator(private val center : Vec2, private val reference : Vec2 = Vec2.DirX) : Comparator<Vertex<Vec2>>{
    override fun compare(lhs: Vertex<Vec2>, rhs: Vertex<Vec2>): Int {
        return compare(lhs.point, rhs.point)
    }

    fun compare(lhs: Vec2, rhs: Vec2): Int {
        return Vec2.rotaryCmp(reference, lhs - center, rhs - center)
    }
}


class DirectionVertexComparator(private val reference : Vec2 = Vec2.DirX) : Comparator<Vertex<Vec2>>{
    override fun compare(lhs: Vertex<Vec2>, rhs: Vertex<Vec2>): Int {
        return compare(lhs.point, rhs.point)
    }

    fun compare(lhs: Vec2, rhs: Vec2): Int {
        return 0.0.compareTo(reference.dot(rhs - lhs))
    }
}

