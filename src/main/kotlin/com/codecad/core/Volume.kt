package com.codecad.core

import com.codecad.common.PointD
import com.codecad.core.test.*
import com.codecad.core.test.Edge
import com.codecad.core.test.Node

abstract class Volume

class ConnectedFaces(val faces: List<EdgedFace>) : Volume()

class Extrude(val polygonFace: PolygonFace, val height: Value) : Volume(){
    fun extrude() : ConnectedFaces{
            val face = polygonFace
            val height = height.value

            val map = HashMap<PointD, Node>()
            val faces = mutableListOf<EdgedFace>()
            val inverted = height > 0

            val offsetVector = PointD(0.0,0.0, height)

            fun getOrAdd(new : PointD) : Node {
                return map.computeIfAbsent(new){ Node(new) }
            }

            fun generateEdges(points : List<PointD>, invert: Boolean = false) : List<Edge>{

                val edges = (if(invert) points.reversed() else points).map { Corner(getOrAdd(it)) }.rollover().map { (left,right) ->
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

                faces.add(EdgedFace(edges.first()))
                return edges
            }

            val points = face.positions.map { it.point }

            generateEdges(points, inverted)
            generateEdges(points.map { it + offsetVector }, !inverted)

            fun iterate(parent: PolygonFace){
                for( (left, right) in parent.positions.map { it.point }.rollover() ){
                    val list = mutableListOf(
                        PointD(left.x, left.y, 0.0),
                        PointD(left.x, left.y, height),
                        PointD(right.x, right.y, height),
                        PointD(right.x, right.y, 0.0)
                    )

                    generateEdges(list, inverted)
                }

                for( child in parent.children){
                    iterate(child)
                }
            }

            iterate(face)

            return ConnectedFaces(faces)
        }

}
