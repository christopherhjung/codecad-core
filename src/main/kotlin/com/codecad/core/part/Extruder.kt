package com.codecad.core.part

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.BSpline
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.BSplineSurface
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.volume.Volume


class Extruder(){

    fun extrude(face: Face, normal: Vec3Expr, height: Expr) : Volume {
        val faceSurface = face.surface
        if(faceSurface !is PlaneSurface) throw RuntimeException()
        val world = normal.world
        val normal = normal.normalized()
        val offset = normal * height
        val extrudeFaceNormal = offset.normalized()

        val baseFace = offsetFace(face, world.ZeroVec3, extrudeFaceNormal.negate())
        val extrudeFace = offsetFace(face, offset, extrudeFaceNormal)

        val faces = arrayListOf<Face>()
        faces.add(baseFace)
        faces.add(extrudeFace)

        val map = hashMapOf<Vertex, Edge>()
        fun extrusionLine(start: Vertex, end: Vertex) : Edge {
            return map.computeIfAbsent(start){ Edge.line(start, end) }
        }

        for( (baseBound, extrudeBound) in baseFace.bounds.zip(extrudeFace.bounds) ){
            for((baseEdgeLoop, extrudeEdgeLoop) in baseBound.edgeLoop.zip(extrudeBound.edgeLoop)){
                val baseOrientedEdge = baseEdgeLoop.edge
                val extrudeOrientedEdge = extrudeEdgeLoop.edge
                val baseEdge = baseOrientedEdge.edge
                val extrudeEdge = extrudeOrientedEdge.edge
                val baseEdgeBound = baseEdge.bound
                val extrudeEdgeBound = extrudeEdge.bound
                val curve = baseEdge.curve
                val sideFace = if(baseEdgeBound != null && extrudeEdgeBound != null){
                    val startEdge = extrusionLine(baseEdgeBound.start, extrudeEdgeBound.start)
                    val endEdge = extrusionLine(baseEdgeBound.end, extrudeEdgeBound.end)

                    val bound = FaceBound(
                        EdgeLoop.of(
                            baseOrientedEdge,
                            OrientedEdge(endEdge, baseOrientedEdge.orientation),
                            OrientedEdge(extrudeEdge, baseOrientedEdge.orientation.invert()),
                            OrientedEdge(startEdge, baseOrientedEdge.orientation.invert())
                        ),
                        FaceBoundKind.OuterBound
                    )

                    val surface =  when(curve) {
                        is Line -> {
                            val newNormal = curve.direction.cross(normal).normalized()
                            val workplane = WorkplaneExpr(baseEdgeBound.start.point, newNormal, curve.direction)
                            PlaneSurface(workplane)
                        }
                        is Circle -> CylindricalSurface(curve.workplane, curve.radius)
                        is BSpline -> {
                            val bottomControls = curve.points
                            val topSpline = extrudeEdge.curve as BSpline
                            val topControls = topSpline.points

                            val resultControls =
                                bottomControls.zip(topControls).map {
                                    arrayOf(it.first, it.second)
                                }.toTypedArray()

                            BSplineSurface(curve.degree, 2, resultControls)
                        }
                        else -> throw RuntimeException()
                    }

                    Face(surface, listOf(bound))
                }else if(curve is Circle){
                    val surface = CylindricalSurface(faceSurface.workplane, curve.radius)
                    Face(surface, listOf(baseBound, extrudeBound))
                }else{
                    throw RuntimeException("Missing bounds!!")
                }

                faces.add(sideFace)
            }
        }

        val volume = Volume(listOf(Shell(faces)))
        return volume
    }

    private fun offsetFace(face : Face, offset : Vec3Expr, normal: Vec3Expr) : Face {
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

                val offsetBound = edge.bound?.let {
                    EdgeBound(
                        remap(it.start),
                        remap(it.end),
                        it.sense
                    )
                }

                val offsetCurve = when(val curve = edge.curve){
                    is Circle -> Circle(curve.workplane.move(offset).withNormal(normal), curve.radius)
                    else -> curve.move(offset)
                }

                edges.add(OrientedEdge(Edge(offsetCurve, offsetBound), orientedEdge.orientation))
            }

            faceBounds.add(FaceBound(EdgeLoop.of(edges), faceBound.sense))
        }

        val surface = face.surface as PlaneSurface
        val offsetWorkplane = surface.workplane.move(offset).withNormal(normal)
        return Face(PlaneSurface(offsetWorkplane), faceBounds)
    }
}

