package com.codecad.core.part

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.*
import com.codecad.core.rollover
import com.codecad.core.volume.Volume
import kotlin.math.abs


class Revolver(){

    fun revolve(face: Face, axis: Line, theta: Expr) : Volume {

        val shells = arrayListOf<Shell>()
        for( faceBound in face.bounds ) {
            val edgeLoop = faceBound.edgeLoop
            val closed = edgeLoop.isClosed()
            val curve = edgeLoop.edge.edge.curve

            if(closed && curve is Circle){
                val point = curve.rightmostPoint()
                val center = axis.project(point)
                val radial = point - center
                val workplane = WorkplaneExpr(center, axis.direction, radial.normalized())
                val surface = ToroidalSurface(workplane, radial.length(), curve.radius)
                val face = Face(surface, listOf())
                shells.add(Shell(listOf(face)))
            }else{
                val faceBounds = arrayListOf<FaceBound>()
                val surfaces = arrayListOf<Surface>()
                for (currentEdgeLoop in edgeLoop) {
                    val orientedEdge = currentEdgeLoop.edge
                    val edge = orientedEdge.edge
                    val curve = edge.curve
                    val start = orientedEdge.start!!

                    val center = axis.project(start.point)
                    val radial = start.point - center
                    val radius = radial.length()
                    val workplane = WorkplaneExpr(center, axis.direction, radial.normalized())

                    val revolveSurface = when(curve){
                        is Line -> {
                            if(abs(curve.direction.dot(axis.direction).evalDouble()) < 1e-8){
                                //plane
                                PlaneSurface(workplane)
                            }else if(abs(curve.direction.cross(axis.direction).z.evalDouble()) < 1e-8){
                                //cylinder
                                CylindricalSurface(workplane, radius)
                            }else{
                                //cone
                                throw RuntimeException("Not yet implemented")
                                //val workplane = WorkplaneExpr(axis.origin,axis.direction, )
                                //ConicalSurface(workplane, radial.length(), axis.angle)
                            }
                        }

                        is Circle -> {
                            val radiusValue = radius.evalDouble()
                            if(radiusValue < 1e-8){
                                SphericalSurface(workplane, curve.radius)
                            }else{
                                ToroidalSurface(workplane, radius, curve.radius)
                            }
                        }
                        else -> throw RuntimeException("Not yet implemented")
                    }

                    val revolveCurve = Circle(workplane, radius)
                    val revolveEdge = OrientedEdge(Edge(revolveCurve), orientedEdge.orientation)
                    faceBounds.add(FaceBound(EdgeLoop.of(revolveEdge), FaceBoundKind.OuterBound))
                    surfaces.add(revolveSurface)
                }

                val faces = arrayListOf<Face>()
                for( (edges, surface) in faceBounds.rollover().zip(surfaces) ){
                    faces.add(Face(surface, listOf(edges.first, edges.second)))
                }

                shells.add(Shell(faces))
            }
        }

        return Volume(shells)
    }
}

