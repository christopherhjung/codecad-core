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
            fun generateEdges(points : List<Node>, plane: Plane) : Edge{
                val edges = points.rollover().map { (left,right) ->
                    val forward = Edge(left,right, plane)
                    val backward = Edge(right,left,  plane)
                    Edge.twinEachOther(forward, backward)
                    forward
                }
                edges.rollover().forEach{ (left, right) ->
                    left.connect(right)
                    right.twin.connect(left.twin)
                }

                return edges.first()
            }

            for(face in polygonVolume.faces){
                if(face is ConvexFace){
                    val root = generateEdges(face.positions, face.toPlane())
                    faces.add(RoutedFace(root, listOf(), root.plane))
                }else if(face is PolygonFace){
                    val root = generateEdges(face.positions, face.plane)
                    val holeEdges = face.holes.map { generateEdges(it.positions, face.plane) }
                    faces.add(RoutedFace(root, holeEdges, face.plane))
                }else if(face is RoutedFace){
                    faces.add(face)
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
        val plane = directedPlane.undirected

        fun getOrAdd(new: PointD): Node {
            return map.computeIfAbsent(new) { Node(new) }
        }

        fun test(face: PolygonFace): Pair<PolygonFace, PolygonFace> {
            var basePoints =
                face.positions.map { Node(directedPlane.projectXYTo(it.point) ) }
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

            val basePolygon = if (inverted) {
                PolygonFace(basePoints.reversed(), plane.flip())
            } else {
                PolygonFace(basePoints, plane)
            }

            val topPolygon = if (inverted) {
                PolygonFace(topPoints, plane.move(height))
            } else {
                PolygonFace(topPoints.reversed(), plane.flip().move(height))
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




























    fun extrudeRoutedFace(): FacedVolume {
        val height = height.value

        val faces = mutableListOf<RoutedFace>()
        val inverted = height > 0

        val offsetVector = directedPlane.normal * height
        val plane = directedPlane.undirected

        fun construct(face: PolygonFace): Pair<RoutedFace, RoutedFace> {
            val positions = if(inverted) face.positions.reversed() else face.positions

            val bottomNodes = positions.map { Node(directedPlane.projectXYTo(it.point)) }
            val topNodes = bottomNodes.map { Node(it.point + offsetVector) }

            var startForward: Edge? = null
            var lastForward: Edge? = null

            var startBackward: Edge? = null
            var lastBackward: Edge? = null

            var startEdge: Edge? = null
            var lastEdge: Edge? = null

            val bottomPlane = if (inverted) {
                plane.flip()
            } else {
                plane
            }

            val topPlane = if (inverted) {
                plane.move(height)
            } else {
                plane.flip(height)
            }

            for((base, top) in bottomNodes.rollover().zip(topNodes.rollover()) ){
                val forward = Edge(base.first, base.second, bottomPlane)

                val sidePlane = Plane.fromConvexPoints(listOf(
                    base.second.point,
                    base.first.point,
                    top.first.point,
                ))

                val topEdge = Edge(base.second, base.first, sidePlane)
                val leftEdge = Edge(base.first, top.first, sidePlane)
                val bottomEdge = Edge(top.first, top.second, sidePlane)
                val rightEdge = Edge(top.second, base.second, sidePlane)

                topEdge.connect(leftEdge)
                leftEdge.connect(bottomEdge)
                bottomEdge.connect(rightEdge)
                rightEdge.connect(topEdge)

                faces.add(RoutedFace(topEdge, listOf(), sidePlane ))

                val backward = Edge(top.second, top.first, topPlane)

                Edge.twinEachOther(forward, topEdge)
                Edge.twinEachOther(backward, bottomEdge)

                if(startForward == null){
                    startForward = forward
                    startBackward = backward
                    startEdge = leftEdge
                }else{
                    lastForward!!.connect(forward)
                    backward.connect(lastBackward!!)
                    Edge.twinEachOther(lastEdge!!, leftEdge)
                }

                if(base.second === startForward.source){
                    forward.connect(startForward)
                    startBackward!!.connect(backward)
                    Edge.twinEachOther(startEdge!!, rightEdge)
                }

                lastEdge = rightEdge
                lastForward = forward
                lastBackward = backward
            }

            val bottomHoles = mutableListOf<Edge>()
            val topHoles = mutableListOf<Edge>()
            for (child in face.holes) {
                val (baseHole, topHole) = construct(child)

                bottomHoles.add(baseHole.root)
                topHoles.add(topHole.root)
            }

            val bottomFace = RoutedFace(startForward!!, bottomHoles, bottomPlane)
            val topFace = RoutedFace(startBackward!!, topHoles, topPlane)

            faces.add(bottomFace)
            faces.add(topFace)

            return Pair(bottomFace, topFace)
        }

        construct(polygonFace)

        return FacedVolume(faces)
    }
}
