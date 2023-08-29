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

        val bottomFace = offsetFace(face, world.ZeroVec3)
        val topFace = offsetFace(face, offset)

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
                            val up = curve.direction.cross(normal).cross(curve.direction).normalized()
                            val workplane = WorkplaneExpr(bottomEdgeBound.start.point, curve.direction, up)
                            PlaneSurface(workplane)
                        }
                        is Circle -> CylindricalSurface(faceSurface.workplane, curve.radius)
                        else -> throw RuntimeException()
                    }

                    val extrusionFace = Face(surface, listOf(bound))
                    faces.add(extrusionFace)
                }else if(curve is Circle){
                    val surface = CylindricalSurface(faceSurface.workplane, curve.radius)
                    faces.add(Face(surface, listOf()))
                }else{
                    throw RuntimeException("Missing bounds!!")
                }
            }
        }

        val volume = Volume(listOf(Shell(faces)))
        //println(volume)
        return volume
    }

    fun offsetFace(face : Face, offset : Vec3Expr) : Face {
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

