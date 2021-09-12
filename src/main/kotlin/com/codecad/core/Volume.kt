package com.codecad.core

import com.codecad.common.Plane
import com.codecad.common.PointD
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
                volume.extrude()
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
            fun generateEdges(points : List<Node>) : Edge{
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
                    val root = generateEdges(face.points)
                    faces.add(RoutedFace(root, listOf(), face.toPlane(), face))
                }else if(face is PolygonFace){
                    val root = generateEdges(face.positions)
                    val holeEdges = face.holes.map { generateEdges(it.positions) }
                    faces.add(RoutedFace(root, holeEdges, face.plane, face))
                }
            }

            return RoutedVolume(faces)
        }
    }
}

class Extrude(val polygonFace: PolygonFace, val directedPlane: DirectedPlane, val height: Value) : Volume() {
    fun extrude(): FacedVolume {
        val height = height.value

        val map = HashMap<PointD, Node>()
        val faces = mutableListOf<Face>()
        val inverted = height > 0

        val offsetVector = directedPlane.normal * height

        fun getOrAdd(new: PointD): Node {
            return map.computeIfAbsent(new) { Node(new) }
        }

        fun test(face: PolygonFace): Pair<PolygonFace, PolygonFace> {
            var basePoints =
                face.positions.map { Node(directedPlane.first * it.point.x + directedPlane.second * it.point.y + directedPlane.normal * directedPlane.distance) }
            var topPoints = basePoints.map { getOrAdd(it.point + offsetVector) }

            for ((base, top) in basePoints.rollover().zip(topPoints.rollover())) {
                val list = mutableListOf(
                    base.first,
                    top.first,
                    top.second,
                    base.second
                )

                if (inverted) {
                    list.reverse()
                }

                faces.add(ConvexFace(list))
            }

            val basePolygon : PolygonFace
            val topPolygon : PolygonFace
            if (inverted) {
                basePolygon = PolygonFace(basePoints.reversed(), Plane(directedPlane.normal * -1.0, -directedPlane.distance))
                topPolygon = PolygonFace(topPoints, Plane(directedPlane.normal,directedPlane.distance + height))
            } else {
                basePolygon = PolygonFace(basePoints, directedPlane.undirected)
                topPolygon = PolygonFace(topPoints.reversed(), Plane(directedPlane.normal * -1.0, height - directedPlane.distance))
            }

            faces.add(basePolygon)
            faces.add(topPolygon)

            return Pair(basePolygon, topPolygon)
        }


        val (basePolygon, topPolygon) = test(polygonFace)

        for (child in polygonFace.holes) {
            val (baseHole, topHole) = test(child)

            basePolygon.holes.add(baseHole)
            topPolygon.holes.add(topHole)
        }

        return FacedVolume(faces)
    }
}
