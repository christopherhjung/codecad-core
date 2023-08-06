package com.codecad.core.face.entity

import com.codecad.common.PointD

data class Vertex(val point: PointD){
    val edges = mutableListOf<Edge>()

    fun addEdge(edge: Edge){
        if(edge.source.point !== point){
            throw RuntimeException("ss")
        }

        edges.add(edge)
    }
}