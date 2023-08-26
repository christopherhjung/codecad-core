package com.codecad.core.face.entity

import com.codecad.core.face.entity.curve.Curve

class EdgeBound(val start: Vertex, val end : Vertex)

open class Edge(var curve: Curve, val bound : EdgeBound? = null)