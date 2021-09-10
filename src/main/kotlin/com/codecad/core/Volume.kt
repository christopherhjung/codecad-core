package com.codecad.core

import com.codecad.common.PointD
import com.codecad.core.test.*

abstract class Volume

class PolygonVolume(val faces: List<PolygonFace>) : Volume()

class RoutedVolume(val faces: List<RoutedFace>){
    companion object{
        fun from(polygonVolume: PolygonVolume) : RoutedVolume{
            val faces = mutableListOf<RoutedFace>()
            fun generateEdges(points : List<Node>) : Edge{
                val edges = points.map { Corner(it) }.rollover().map { (left,right) ->
                    val forward = Edge(left,right)
                    forward.twin = Edge(right,left)
                    forward.twin.twin = forward
                    forward.side = Side.Outside
                    forward.twin.side = Side.Outside
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
                val root = generateEdges(face.positions)
                val holeEdges = face.holes.map { generateEdges(it.positions) }
                faces.add(RoutedFace(root, holeEdges))
            }

            return RoutedVolume(faces)
        }
    }
}

class Extrude(val polygonFace: PolygonFace, val directedPlane: DirectedPlane, val height: Value) : Volume() {
    fun extrude(): PolygonVolume {
        val height = height.value

        val map = HashMap<PointD, Node>()
        val polygons = mutableListOf<PolygonFace>()
        val inverted = height > 0

        val offsetVector = directedPlane.root.normal * height

        fun getOrAdd(new: PointD): Node {
            return map.computeIfAbsent(new) { Node(new) }
        }

        fun test(face: PolygonFace): Pair<PolygonFace, PolygonFace> {
            var basePoints =
                face.positions.map { Node(directedPlane.first * it.point.x + directedPlane.second * it.point.y) }
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

                polygons.add(PolygonFace(list))
            }

            if (inverted) {
                basePoints = basePoints.reversed()
            } else {
                topPoints = topPoints.reversed()
            }

            val basePolygon = PolygonFace(basePoints)
            val topPolygon = PolygonFace(topPoints)

            polygons.add(basePolygon)
            polygons.add(topPolygon)

            return Pair(basePolygon, topPolygon)
        }


        val (basePolygon, topPolygon) = test(polygonFace)

        for (child in polygonFace.holes) {
            val (baseHole, topHole) = test(child)

            basePolygon.holes.add(baseHole)
            topPolygon.holes.add(topHole)
        }

        return PolygonVolume(polygons)
    }
}
