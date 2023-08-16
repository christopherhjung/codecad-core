package com.codecad.core.face.entity

class EdgeBound {
    private lateinit var boundEdge : BoundEdge
}

class BoundEdge{
    private lateinit var edge : Edge
    private var orientation : Boolean = false
    private lateinit var twin : BoundEdge

    private lateinit var next : BoundEdge
    private lateinit var prev : BoundEdge
}