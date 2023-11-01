package com.codecad.core.part

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.BSpline
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.BSplineSurface
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface
import com.codecad.core.volume.Volume


object Extruder{

    fun extrude(face: Face, normal: Vec3, height: Double) : Volume {
        val faceSurface = face.surface
        if(faceSurface !is PlaneSurface) throw RuntimeException()
        val normal = normal.normalized()
        val offset = normal * height
        val extrudeFaceNormal = offset.normalized()

        val baseFace = offsetFace(face, Vec3.Zero, extrudeFaceNormal.negate())
        val extrudeFace = offsetFace(face, offset, extrudeFaceNormal)

        val faces = arrayListOf<Face>()
        faces.add(baseFace)

        val map = hashMapOf<Vertex<Vec3>, Edge<Vec3>>()
        fun extrusionLine(start: Vertex<Vec3>, end: Vertex<Vec3>) : Edge<Vec3> {
            return map.computeIfAbsent(start){ Edge.line(start, end) }
        }

        for( (baseBound, extrudeBound) in baseFace.bounds.zip(extrudeFace.bounds) ){
            var initExtrusionLine : Loop<Vec3>? = null
            var lastExtrusionLine : Loop<Vec3>? = null
            for((baseLoop, extrudeLoop) in baseBound.loop.zip(extrudeBound.loop)){
                val baseOrientedEdge = baseLoop.edge
                val extrudeOrientedEdge = extrudeLoop.edge
                val baseEdge = baseOrientedEdge.edge
                val extrudeEdge = extrudeOrientedEdge.edge
                val baseEdgeBound = baseEdge.bound
                val extrudeEdgeBound = extrudeEdge.bound

                val surface = when(val curve = baseEdge.curve) {
                    is Line -> {
                        val newNormal = curve.direction.cross(normal).normalized()
                        val workplane = Workplane(baseEdgeBound.start.point, newNormal, curve.direction)
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

                val sideFace = if(baseEdgeBound.isUnbounded()){
                    val baseCopyLoop = Loop.wireCircular(baseOrientedEdge)
                    val extrudeCopyLoop = Loop.wireCircular(extrudeEdge)

                    baseCopyLoop.twinWith(baseLoop)
                    extrudeCopyLoop.twinWith(extrudeLoop)

                    Face(surface, listOf(
                        FaceBound(baseCopyLoop, FaceBoundKind.OuterBound),
                        FaceBound(extrudeCopyLoop, FaceBoundKind.OuterBound)
                    ))
                }else{
                    val startEdge = extrusionLine(baseEdgeBound.start, extrudeEdgeBound.start)
                    val endEdge = extrusionLine(baseEdgeBound.end, extrudeEdgeBound.end)

                    val loop = Loop.wireCircular(
                        baseOrientedEdge,
                        OrientedEdge(endEdge, baseOrientedEdge.orientation),
                        OrientedEdge(extrudeEdge, baseOrientedEdge.orientation.invert()),
                        OrientedEdge(startEdge, baseOrientedEdge.orientation.invert())
                    )

                    loop.twinWith(baseLoop)
                    val nextLastExtrusionLine = loop.next
                    nextLastExtrusionLine.next.twinWith(extrudeLoop)

                    if(lastExtrusionLine != null){
                        lastExtrusionLine.twinWith(nextLastExtrusionLine)
                    }else{
                        initExtrusionLine = loop.prev
                    }

                    lastExtrusionLine = nextLastExtrusionLine
                    val bound = FaceBound(loop, FaceBoundKind.OuterBound )
                    Face(surface, listOf(bound))
                }

                faces.add(sideFace)
            }

            initExtrusionLine?.twinWith(lastExtrusionLine!!)
        }

        faces.add(extrudeFace)
        val volume = Volume(listOf(Shell(faces)))
        return volume
    }

    private fun offsetFace(face : Face, offset : Vec3, normal: Vec3) : Face {
        val map = hashMapOf<Vertex<Vec3>, Vertex<Vec3>>()
        fun remap(vertex: Vertex<Vec3>) : Vertex<Vec3> {
            return map.computeIfAbsent(vertex){ Vertex(vertex.point + offset) }
        }

        val faceBounds = arrayListOf<FaceBound<Vec3>>()
        for( faceBound in face.bounds ) {
            val edges = arrayListOf<OrientedEdge<Vec3>>()
            for (currentEdgeLoop in faceBound.loop) {
                val orientedEdge = currentEdgeLoop.edge
                val edge = orientedEdge.edge

                val offsetBound = edge.bound.let {
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

            faceBounds.add(FaceBound(Loop.wireCircular(edges), faceBound.sense))
        }

        val surface = face.surface as PlaneSurface
        val offsetWorkplane = surface.workplane.move(offset).withNormal(normal)
        return Face(PlaneSurface(offsetWorkplane), faceBounds)
    }
}

