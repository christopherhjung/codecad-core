package com.codecad.core.volume

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.*
import com.codecad.core.brep.surface.PlaneSurface

class Debugger{
    var indent = 0
    val vertices = hashMapOf<Vertex<Vec3>, Int>()
    val faces = hashMapOf<Face, Int>()
    val vertexBuilder = StringBuilder()
    val faceBuilder = StringBuilder()

    fun getVertexIndex(vertex: Vertex<Vec3>) : Int{
        return vertices.computeIfAbsent(vertex){
            val nextIdx = vertices.size
            vertexBuilder.append(nextIdx).append(" = ").append(vertex.point).append("\n")
            nextIdx
        }
    }

    fun getFaceIndex(face: Face) : Int{
        return faces.computeIfAbsent(face){
            faces.size
        }
    }

    fun addFace(face: Face) : Debugger{
        faceBuilder.append("----------------\n")

        val planeSurface = face.surface
        if(planeSurface is PlaneSurface){
            val workplane = planeSurface.workplane
            faceBuilder.append("PlaneSurface(")
                .append(workplane.origin).append(",")
                .append(workplane.normal).append(",")
                .append(workplane.x)
        }

        faceBuilder.append(")\n")
        for(faceBound in face.bounds){
            faceBuilder.append("Face(")
            faceBuilder.append(getFaceIndex(face)).append(",")
            var sep = ""
            for(edgeLoop in faceBound.loop){
                val orientedEdge = edgeLoop.edge
                val bound = orientedEdge.bound
                if(bound != null){
                    faceBuilder.append(sep)
                        .append(getVertexIndex(bound.start))
                        .append("->")
                        .append(getVertexIndex(bound.end))

                    faceBuilder.append("[")
                        .append(getFaceIndex(edgeLoop.twin!!.face))
                        .append("]")

                    /*
                    if(bound.sense != Sense.None){
                        faceBuilder.append("(")
                            .append(bound.sense)
                            .append(")")
                    }*/
                    sep = ", "
                }
            }
            faceBuilder.append(")\n")
        }

        return this
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

