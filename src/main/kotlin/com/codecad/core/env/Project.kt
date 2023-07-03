package com.codecad.core.env

import com.codecad.core.*
import com.codecad.core.parser.ast.primitive.Expr
import com.codecad.core.sketch.World
import com.codecad.core.test.DirectedPlane

class Project(){
    val sketches: MutableList<Sketch> = mutableListOf()
    val tracker: Tracker = Tracker()
    val volumes = mutableListOf<Volume>()
    val world = World()

    fun extrude(sketch: Sketch, pointer: Vec2, height: Expr) {
        val face = findFace(sketchToLines(sketch, ignoreConstruction = true), pointer.fixed())

        if(face != null){
            volumes.add(Extrude(face, DirectedPlane.XY, height))
        }
    }

    fun extrude(sketch: Sketch, height: Expr, plane: DirectedPlane = DirectedPlane.XY) {
        val lines = sketchToLines(sketch, ignoreConstruction = true)
        val faces = findFaces(lines)

        for(face in faces){
            volumes.add(Extrude(face, plane,  height))
        }
    }
}
