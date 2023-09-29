package com.codecad.core.sketch

import com.codecad.core.ast.vec.Vec2
import com.codecad.core.brep.EdgeOrientation
import com.codecad.core.brep.OrientedEdge
import com.codecad.core.brep.Vertex
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line

fun tangent(orientedEdge: OrientedEdge<Vec2>) : Vec2{
    val edge = orientedEdge.edge
    return when(val curve = edge.curve){
        is Line -> if(orientedEdge.orientation == EdgeOrientation.Forward){
            curve.direction
        }else{
            curve.direction.negate()
        }

        is Circle -> {
            val origin = curve.workplane.origin
            val bound = edge.bound!!
            if(orientedEdge.orientation == EdgeOrientation.Forward){
                (origin - bound.start.point).normalLeft()
            }else{
                (origin - bound.end.point).normalRight()
            }
        }
        else -> Vec2.Zero
    }
}

object RotaryEdgeComparator : Comparator<OrientedEdge<Vec2>>{
    override fun compare(lhs: OrientedEdge<Vec2>, rhs: OrientedEdge<Vec2>): Int {
        return Vec2.rotaryCmp(Vec2.DirX, tangent(lhs), tangent(rhs))
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

