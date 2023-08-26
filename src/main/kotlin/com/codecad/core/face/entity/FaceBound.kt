package com.codecad.core.face.entity

class FaceBound(var edgeLoop : EdgeLoop, var sense: Boolean)

class EdgeLoop{
    lateinit var edge : Edge
    var orientation : Boolean = false
    lateinit var twin : EdgeLoop

    lateinit var next : EdgeLoop
    lateinit var prev : EdgeLoop

    companion object{
        fun closed(edge: Edge) : EdgeLoop{
            val loop = EdgeLoop()
            loop.edge = edge
            loop.next = loop
            loop.prev = loop
            return loop
        }
    }
}