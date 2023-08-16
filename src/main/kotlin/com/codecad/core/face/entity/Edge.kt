package com.codecad.core.face.entity

import com.codecad.common.PointD
import com.codecad.core.face.entity.curve.Curve
import com.codecad.core.face.entity.sketch.SketchVertex


data class Edge(val source: Vertex, val target : Vertex, var curve: Curve){

}