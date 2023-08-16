package com.codecad.core.face.entity.sketch

import com.codecad.common.PointD
import com.codecad.core.face.SketchEdge

data class SketchVertex(val point: PointD){
    val edges = mutableListOf<SketchEdge>()

    fun addEdge(edge: SketchEdge){
        if(edge.source.point !== point){
            throw RuntimeException("ss")
        }

        edges.add(edge)
    }
}