package com.codecad.core

import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.test.*

abstract class Volume

class FacedVolume(val faces: List<Face>) : Volume() {
    companion object {
        fun from(volume: Volume): FacedVolume {
            return if (volume is RoutedVolume) {
                TODO("not yet implemented")
            } else if (volume is FacedVolume) {
                return volume
            } else if (volume is Extrude) {
                volume.extrudeRoutedFace()
            } else {
                TODO("not yet implemented")
            }
        }
    }
}

class PolygonVolume(val faces: List<PolygonFace>) : Volume()

class RoutedVolume(val faces: List<RoutedFace>) : Volume(){
    companion object{

        fun from(volume: Volume) : RoutedVolume{
            return if(volume is RoutedVolume){
                volume
            }else if( volume is FacedVolume ){
                from(volume)
            }else if( volume is Extrude ){
                from(volume.extrude())
            }else{
                TODO("not yet implemented")
            }
        }

        fun from(polygonVolume: FacedVolume) : RoutedVolume{
            val faces = mutableListOf<RoutedFace>()
            fun generateEdges(points : List<PointD>) : Edge{
                val edges = points.map { Corner(it) }.rollover().map { (left,right) ->
                    val forward = Edge(left,right)
                    forward.twin = Edge(right,left)
                    forward.twin.twin = forward
                    //forward.side = Side.Outside
                    //forward.twin.side = Side.Outside
                    left.addEdge(forward)
                    right.addEdge(forward.twin)
                    forward
                }
                edges.rollover().forEach{ (left, right) ->
                    left.next = right
                    right.twin.next = left
                }

                return edges.first()
            }

            for(face in polygonVolume.faces){
                if(face is ConvexFace){
                    val root = generateEdges(face.positions)
                    faces.add(RoutedFace(root, listOf(), face.toPlane(), face))
                }else if(face is PolygonFace){
                    val root = generateEdges(face.positions)
                    val holeEdges = face.holes.map { generateEdges(it.positions) }
                    faces.add(RoutedFace(root, holeEdges, face.plane, face))
                }else if(face is RoutedFace){
                    faces.add(face)
                }
            }

            return RoutedVolume(faces)
        }
    }
}

class Extrude(val polygonFace: PolygonFace, val directedPlane: DirectedPlane, val height: Expr) : Volume() {
    fun extrude(): FacedVolume {
        val height = height.evalDouble()

        val faces = mutableListOf<Face>()
        val inverted = height > 0

        val offsetVector = directedPlane.normal * height
        val plane = directedPlane.undirected

        fun addSideFace(face: PolygonFace): Pair<PolygonFace, PolygonFace> {
            val basePoints = face.positions.map { directedPlane.projectXYTo(it) }
            val topPoints = basePoints.map { it + offsetVector }

            for ((base, top) in basePoints.rollover().zip(topPoints.rollover())) {
                val list = mutableListOf(
                    base.first, top.first,
                    top.second, base.second
                )

                if (inverted) {
                    list.reverse()
                }

                faces.add(ConvexFace(list))
            }

            val basePolygon = if (inverted) {
                PolygonFace(basePoints.reversed(), FaceType.Surface, plane.flip())
            } else {
                PolygonFace(basePoints, FaceType.Surface, plane)
            }

            val topPolygon = if (inverted) {
                PolygonFace(topPoints, FaceType.Surface, plane.move(height))
            } else {
                PolygonFace(topPoints.reversed(), FaceType.Surface, plane.flip().move(height))
            }

            return Pair(basePolygon, topPolygon)
        }

        val (basePolygon, topPolygon) = addSideFace(polygonFace)

        faces.add(basePolygon)
        faces.add(topPolygon)

        for (child in polygonFace.holes) {
            val (baseHole, topHole) = addSideFace(child)

            basePolygon.holes.add(baseHole)
            topPolygon.holes.add(topHole)
        }

        return FacedVolume(faces)
    }


    fun extrudeRoutedFace(): FacedVolume {
        throw Error("not implemented")
    }
}
