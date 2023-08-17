package com.codecad.core.face.entity

import com.codecad.core.face.entity.curve.Curve


class BoundedEdge(val source: Vertex, val target : Vertex, curve: Curve) : Edge(curve){

}

open class Edge(var curve: Curve){

}