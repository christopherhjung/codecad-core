package com.codecad.core.mesh

import com.codecad.core.ast.primitive.ParamExpr
import com.codecad.core.ast.vec.QuaternionExpr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.face.entity.Edge
import com.codecad.core.face.entity.EdgeLoop
import com.codecad.core.face.entity.Face
import com.codecad.core.face.entity.Shell
import com.codecad.core.face.entity.curve.Circle
import com.codecad.core.face.entity.curve.Line
import com.codecad.core.face.entity.surface.CylindricalSurface
import com.codecad.core.face.entity.surface.PlaneSurface
import com.codecad.core.volume.Volume


class MeshGenerator {
    val edgePoints = HashMap<Edge, List<Int>>()
    val map = HashMap<Vec3, Int>()
    val indices = arrayListOf<Int>()
    val vertices = arrayListOf<Float>()

    fun createVertex(point : Vec3) : Int{
        return map.computeIfAbsent(point){
            map.size
        }
    }

    fun generate(volume: Volume) : Mesh{
        for(shell in volume.shells){
            generate(shell)
        }

        return Mesh(indices.toIntArray(), vertices.toFloatArray())
    }

    fun generate(shell: Shell){
        for( face in shell.faces ){
            generate(face)
        }
    }

    fun generate(face: Face){
        val surface = face.surface
        if(surface is PlaneSurface){
            val bound = face.bounds.first()
            val newIndices = indicesLoop(bound.edgeLoop)

            if(newIndices.size < 2) return
            val first = newIndices[0]
            var prev = newIndices[1]

            for( current in newIndices.subList(2, newIndices.size) ){
                indices.add(first)
                indices.add(prev)
                indices.add(current)
                prev = current
            }
        }else if(surface is CylindricalSurface){
            val firstBound = face.bounds.first()
            val lastBound = face.bounds.last()
            val firstIndices = indicesLoop(firstBound.edgeLoop)
            val secondIndices = indicesLoop(lastBound.edgeLoop)

            val size = firstIndices.size
            for( idx in firstIndices.indices ){
                val nextIdx = (idx + 1) % size
                indices.add(firstIndices[idx])
                indices.add(secondIndices[idx])
                indices.add(firstIndices[nextIdx])

                indices.add(secondIndices[idx])
                indices.add(firstIndices[nextIdx])
                indices.add(secondIndices[nextIdx])
            }
        }
    }

    fun indicesLoop(edgeLoop: EdgeLoop) : List<Int>{
        var current = edgeLoop

        val indices = arrayListOf<Int>()
        while(true){
            indices.addAll(generate(current))

            current = current.next
            if(current === edgeLoop) break
        }

        return indices
    }

    fun generate(edgeLoop: EdgeLoop) : List<Int>{
        val indices = edgePoints.computeIfAbsent(edgeLoop.edge){
            generate(it)
        }

        return if(edgeLoop.orientation){
            indices.reversed()
        }else{
            indices
        }
    }

    fun generate(edge: Edge) : List<Int>{
        val indices = arrayListOf<Int>()
        val curve = edge.curve

        fun addPoint(point: Vec3){
            vertices.add(point.x.toFloat())
            vertices.add(point.y.toFloat())
            vertices.add(point.z.toFloat())
            indices.add(createVertex(point))
        }

        when(curve){
            is Line -> {
                val bound = edge.bound
                if(bound != null){
                    addPoint(bound.start.point.eval())
                    addPoint(bound.end.point.eval())
                }
            }

            is Circle -> {
                val workplane = curve.workplane
                val axisUp = workplane.axisA.cross(workplane.axisB)

                val paramExpr = ParamExpr(axisUp.world, 0.0)
                val quaternion = QuaternionExpr.fromNAxis(axisUp, paramExpr)
                val somePoint = workplane.origin + workplane.axisA * curve.radius
                val rotatedPoint = quaternion.rotate(workplane.origin, somePoint)

                for( angle in 0 until 360 ){
                    val theta = angle * (Math.PI / 180.0)
                    paramExpr.value = theta
                    val point = rotatedPoint.eval()
                    addPoint(point)
                }
            }
        }

        return indices
    }
}





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
