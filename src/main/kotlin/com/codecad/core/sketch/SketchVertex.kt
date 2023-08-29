package com.codecad.core.sketch


import com.codecad.core.ast.vec.Vec2

data class SketchVertex(val point: Vec2){
    val edges = mutableListOf<SketchEdge>()

    fun addEdge(edge: SketchEdge){
        if(edge.source.point !== point){
            throw RuntimeException("ss")
        }

        edges.add(edge)
    }
}