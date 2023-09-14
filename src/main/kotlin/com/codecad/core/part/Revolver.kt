package com.codecad.core.part

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.QuaternionExpr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.*
import com.codecad.core.rollover
import com.codecad.core.volume.Volume
import kotlin.math.abs


class Revolver(){

    fun revolve(face: Face, axis: Line, theta: Expr) : Volume {
        val quat = QuaternionExpr.fromAxis(axis.direction, theta)

        val shells = arrayListOf<Shell>()
        for( faceBound in face.bounds ) {
            val edgeLoop = faceBound.edgeLoop
            val circleCurve = edgeLoop.edge.edge.curve

            if(circleCurve is Circle && edgeLoop.isClosed()){
                val center = circleCurve.workplane.origin
                val surfaceWorkplane = axis.alignWorkplane(center)
                val radius = surfaceWorkplane.origin.distanceTo(center)

                val revolveSurface = if(abs(radius.evalDouble()) < 1e-8){
                    SphericalSurface(surfaceWorkplane, circleCurve.radius)
                }else{
                    ToroidalSurface(surfaceWorkplane, radius, circleCurve.radius)
                }

                val faceBounds = arrayListOf<FaceBound>()
                val faces = arrayListOf<Face>()
                if(true){ // try revolve endstops
                    val newCenter = quat.rotate(surfaceWorkplane.origin, center)
                    val radial = (newCenter - surfaceWorkplane.origin).normalized()
                    val rotatedWorkplane = WorkplaneExpr(newCenter, axis.direction.cross(radial), radial)
                    val rotatedCircle = Circle(rotatedWorkplane, circleCurve.radius)
                    val plane = PlaneSurface(rotatedWorkplane)

                    val newFaceBound = FaceBound(EdgeLoop.of(Edge(rotatedCircle)), FaceBoundKind.OuterBound)

                    val test = face.surface as ElementarySurface
                    val invertedWorkplane = test.workplane.invert()
                    val firstPlane = PlaneSurface(invertedWorkplane)
                    val invertedFaceBound = FaceBound(
                        EdgeLoop.of(Edge(Circle(invertedWorkplane, circleCurve.radius))),
                        FaceBoundKind.OuterBound
                    )
                    val rotated = Face(firstPlane, listOf(invertedFaceBound))

                    val otherFace = Face(plane, listOf(newFaceBound))

                    faceBounds.add(newFaceBound)
                    faceBounds.add(invertedFaceBound)
                    faces.add(rotated)
                    faces.add(otherFace)
                }

                val revolveFace = Face(revolveSurface, faceBounds)
                faces.add(revolveFace)
                shells.add(Shell(faces))
            }else{
                val faceBounds = arrayListOf<FaceBound>()
                val surfaces = arrayListOf<Surface>()
                for (currentEdgeLoop in edgeLoop) {
                    val orientedEdge = currentEdgeLoop.edge
                    val curve = orientedEdge.edge.curve
                    val start = orientedEdge.start!!

                    val workplane = axis.alignWorkplane(start.point)
                    val radius = workplane.origin.distanceTo(start.point)

                    val revolveSurface = when(curve){
                        is Line -> {
                            if(abs(curve.direction.dot(axis.direction).evalDouble()) < 1e-8){
                                //plane
                                PlaneSurface(workplane)
                            }else if(abs(curve.direction.cross(axis.direction).length().evalDouble()) < 1e-8){
                                //cylinder
                                CylindricalSurface(workplane, radius)
                            }else{
                                //cone
                                val angle = Vec3Expr.angle(axis.direction, curve.direction)
                                ConicalSurface(workplane, radius, angle)
                            }
                        }

                        is Circle -> {
                            val workplane = axis.alignWorkplane(curve.workplane.origin)
                            val radius = workplane.origin.distanceTo(curve.workplane.origin)
                            if(radius.evalDouble() < 1e-8){
                                SphericalSurface(workplane, curve.radius)
                            }else{
                                ToroidalSurface(workplane, radius, curve.radius)
                            }
                        }
                        else -> throw RuntimeException("Not yet implemented")
                    }

                    val revolveCurve = Circle(workplane, radius)
                    val revolveEdge = OrientedEdge(Edge(revolveCurve), EdgeOrientation.Forward)
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

