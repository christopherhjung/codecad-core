package com.codecad.core.part

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.volume.Volume


class Extruder(){

    fun extrude(face: Face, normal: Vec3Expr, height: Expr) : Volume {
        val faceSurface = face.surface
        if(faceSurface !is PlaneSurface) throw RuntimeException()

        val normal = normal.normalized()
        val offset = normal * height
        val world = normal.world

        val bottomFace = offsetFace(face, world.ZeroVec3, true)
        val topFace = offsetFace(face, offset, false)

        val faces = arrayListOf<Face>()
        faces.add(bottomFace)
        faces.add(topFace)

        val map = hashMapOf<Vertex, Edge>()
        fun extrusionLine(start: Vertex, end: Vertex) : Edge {
            return map.computeIfAbsent(start){ Edge.line(start, end) }
        }

        for( (bottomBound, topBound) in bottomFace.bounds.zip(topFace.bounds) ){
            for((bottomEdgeLoop, topEdgeLoop) in bottomBound.edgeLoop.zip(topBound.edgeLoop)){
                val bottomOrientedEdge = bottomEdgeLoop.edge
                val topOrientedEdge = topEdgeLoop.edge
                val bottomEdge = bottomOrientedEdge.edge
                val topEdge = topOrientedEdge.edge
                val bottomEdgeBound = bottomEdge.bound
                val topEdgeBound = topEdge.bound
                val curve = bottomEdge.curve

                if(bottomEdgeBound != null && topEdgeBound != null){
                    val startEdge = extrusionLine(bottomEdgeBound.start, topEdgeBound.start)
                    val endEdge = extrusionLine(bottomEdgeBound.end, topEdgeBound.end)

                    val bound = FaceBound(
                        EdgeLoop.of(
                            bottomOrientedEdge,
                            OrientedEdge(endEdge, bottomOrientedEdge.orientation),
                            OrientedEdge(topEdge, bottomOrientedEdge.orientation.invert()),
                            OrientedEdge(startEdge, bottomOrientedEdge.orientation.invert())
                        ),
                        FaceBoundSense.Inside
                    )

                    val surface =  when(curve) {
                        is Line -> {
                            val newNormal = curve.direction.cross(normal).normalized()
                            val workplane = WorkplaneExpr(bottomEdgeBound.start.point, newNormal, curve.direction)
                            PlaneSurface(workplane)
                        }
                        is Circle -> CylindricalSurface(curve.workplane, curve.radius)
                        else -> throw RuntimeException()
                    }

                    val extrusionFace = Face(surface, listOf(bound))
                    faces.add(extrusionFace)
                }else if(curve is Circle){
                    val surface = CylindricalSurface(faceSurface.workplane, curve.radius)

                    val bottomBound = FaceBound(
                        EdgeLoop.of(bottomOrientedEdge),
                        FaceBoundSense.Outside
                    )
                    val topBound = FaceBound(
                        EdgeLoop.of(topOrientedEdge),
                        FaceBoundSense.Outside
                    )

                    faces.add(Face(surface, listOf(bottomBound, topBound)))
                }else{
                    throw RuntimeException("Missing bounds!!")
                }
            }
        }

        val volume = Volume(listOf(Shell(faces)))
        return volume
    }

    fun offsetFace(face : Face, offset : Vec3Expr, invert: Boolean) : Face {
        val map = hashMapOf<Vertex, Vertex>()
        fun remap(vertex: Vertex) : Vertex {
            return map.computeIfAbsent(vertex){ Vertex(vertex.point + offset) }
        }

        val faceBounds = arrayListOf<FaceBound>()
        for( faceBound in face.bounds ) {
            val edges = arrayListOf<OrientedEdge>()
            for (currentEdgeLoop in faceBound.edgeLoop) {
                val orientedEdge = currentEdgeLoop.edge
                val edge = orientedEdge.edge
                val bound = edge.bound

                val newBound = bound?.let {
                    EdgeBound(
                        remap(bound.start),
                        remap(bound.end),
                        bound.sense
                    )
                }

                val curve = edge.curve.move(offset)
                edges.add(OrientedEdge(Edge(curve, newBound), orientedEdge.orientation))
            }

            faceBounds.add(FaceBound(EdgeLoop.of(edges), faceBound.sense))
        }

        val newWorkplane = face.surface.workplane.move(offset)
        return Face(PlaneSurface(newWorkplane), faceBounds)
    }
}

