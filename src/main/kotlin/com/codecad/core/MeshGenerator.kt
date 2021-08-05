package com.codecad.core

import com.codecad.common.Mesh
import com.codecad.common.PointD

class MeshGenerator {

    fun invertFace(polygonFace : PolygonFace) : PolygonFace{
        val result = PolygonFace(polygonFace.points, true)

        for( child in polygonFace.children ){
            result.children.add(invertFace(child))
        }

        return result
    }

    fun offsetFace(polygonFace : PolygonFace, offset: Double) : PolygonFace{
        val offsetVector = PointD(0.0,0.0, offset)
        val result = PolygonFace(polygonFace.points.map { it + offsetVector })

        for( child in polygonFace.children ){
            result.children.add(offsetFace(child, offset))
        }

        return result
    }

    fun generateFaces(extrude: Extrude) : List<Face>{
        val face = extrude.polygonFace
        val height = extrude.height.value

        val faces = mutableListOf<Face>()
        val map = HashMap<PointD, PointD>()
        val inverted = height > 0

        if(inverted){
            faces.add(invertFace(face))
            faces.add(offsetFace(face, height))
        }else{
            faces.add(face)
            faces.add(invertFace(offsetFace(face, height)))
        }

        fun getOrAdd(x: Double, y: Double, z: Double) : PointD{
            val new = PointD(x,y,z)
            return map.computeIfAbsent(new){new}
        }

        fun iterate(parent: PolygonFace){
            for( i in parent.points.indices ){
                val left = parent.points[i]
                val right = parent.points[(i+1)% parent.points.size]

                val list = mutableListOf(
                    getOrAdd(left.x, left.y, 0.0),
                    getOrAdd(left.x, left.y, height),
                    getOrAdd(right.x, right.y, height),
                    getOrAdd(right.x, right.y, 0.0)
                )

                if(inverted){
                    list.reverse()
                }

                val extrudeFace = ConvexFace(list)
                faces.add(extrudeFace)
            }

            for( child in parent.children){
                iterate(child)
            }
        }

        iterate(face)

        return faces
    }

    fun generate(extrude: Extrude) : Mesh {
        val testFaces = generateFaces(extrude)

        val map = HashMap<PointD, Int>()
        fun getOrAdd(point: PointD) : Int{
            return map.computeIfAbsent(point){map.size}
        }

        val faces = mutableListOf<Int>()
        for( face in testFaces ){
            val triangles = face.generateTriangles()

            for( triangle in triangles ){
                for( point in triangle.points){
                    faces.add(getOrAdd(point))
                }
            }
        }

        val meshPoints = FloatArray(map.size * 3)

        for( (point, index) in map.entries ){
            meshPoints[index * 3] = (point.x ).toFloat()
            meshPoints[index * 3 + 1] = (point.y ).toFloat()
            meshPoints[index * 3 + 2] = (point.z ).toFloat()
        }

        val mesh = Mesh()
        mesh.points = meshPoints
        mesh.indices = faces.toIntArray()

        return mesh
    }
}
