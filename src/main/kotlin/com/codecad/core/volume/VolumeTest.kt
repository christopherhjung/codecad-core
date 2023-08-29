package com.codecad.core.volume

import com.codecad.core.ast.primitive.Expr
import com.codecad.core.ast.vec.Vec3Expr
import com.codecad.core.brep.*
import com.codecad.core.brep.curve.Circle
import com.codecad.core.brep.curve.Line
import com.codecad.core.brep.surface.CylindricalSurface
import com.codecad.core.brep.surface.PlaneSurface

class Debugger{
    var indent = 0
    val vertices = hashMapOf<Vertex, Int>()
    val vertexBuilder = StringBuilder()
    val faceBuilder = StringBuilder()

    fun getVertexIndex(vertex: Vertex) : Int{
        return vertices.computeIfAbsent(vertex){
            val nextIdx = vertices.size
            vertexBuilder.append(nextIdx).append(" = ").append(vertex.point).append("\n")
            nextIdx
        }
    }

    fun addFace(face: Face){
        for(faceBound in face.bounds){
            faceBuilder.append("Face(")
            var sep = ""
            for(edgeLoop in faceBound.edgeLoop){
                val edge = edgeLoop.edge
                val bound = edge.bound
                if(bound != null){
                    faceBuilder.append(sep).append(getVertexIndex(bound.start))
                    sep = ", "
                }
            }
            faceBuilder.append(")\n")
        }
    }

    fun build() : String{
        return vertexBuilder.toString() + faceBuilder.toString()
    }
}


class Volume(val shells : List<Shell>){

    fun debug(debugger: Debugger){
        for(shell in shells){
            shell.debug(debugger)
        }
    }

    override fun toString(): String {
        val debugger = Debugger()
        debug(debugger)
        return debugger.build()
    }
}

