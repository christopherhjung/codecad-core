package com.codecad.core

import com.codecad.common.Mesh
import com.codecad.common.PointD
import com.codecad.core.test.Node

class MeshGenerator {

    fun generate(extrude: Extrude) : Mesh {
        val testFaces = extrude.extrude()

        val map = HashMap<PointD, Int>()
        fun getOrAdd(point: PointD) : Int{
            return map.computeIfAbsent(point){map.size}
        }

        val faces = mutableListOf<Int>()
        for( face in testFaces.faces ){
            val triangles = face.generateTriangles()

            for( triangle in triangles ){
                for( node in triangle.points){
                    faces.add(getOrAdd(node.point))
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
