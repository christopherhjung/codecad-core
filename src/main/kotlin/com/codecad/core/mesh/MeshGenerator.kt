package com.codecad.core.mesh

import com.codecad.common.Mesh
import com.codecad.common.PointD
import com.codecad.core.ast.vec.Vec3
/*
class MeshGenerator {

    private val faces = mutableListOf<Int>()
    private val map = HashMap<Vec3, Int>()
    private fun index(point: Vec3) : Int{
        return map.computeIfAbsent(point){map.size}
    }

    fun add(volume: FacedVolume){
        for( face in volume.faces ){
            val triangles = face.generateTriangles()

            for( triangle in triangles ){
                for( pos in triangle.positions){
                    faces.add(index(pos))
                }
            }
        }
    }

    fun build() : Mesh{
        val meshPoints = FloatArray(map.size * 3)

        for( (point, index) in map.entries ){
            meshPoints[index * 3] = (point.x).toFloat()
            meshPoints[index * 3 + 1] = (point.y).toFloat()
            meshPoints[index * 3 + 2] = (point.z).toFloat()
        }

        val mesh = Mesh()
        mesh.points = meshPoints
        mesh.indices = faces.toIntArray()
        return mesh
    }
}*/
