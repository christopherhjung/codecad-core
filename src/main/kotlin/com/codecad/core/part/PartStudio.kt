package com.codecad.core.part

import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.World
import com.codecad.core.face.entity.DirectedPlane
import com.codecad.core.face.entity.FaceType
import com.codecad.core.face.findFaces
import com.codecad.core.volume.Extrude
import com.codecad.core.volume.Volume

class PartStudio {
    val sketches: MutableList<Sketch> = mutableListOf()
    val tracker: Tracker = Tracker()
    val volumes = mutableListOf<Volume>()
    val world = World()

    fun extrude(sketch: Sketch, height: Expr, plane: DirectedPlane = DirectedPlane.XY) {
        val lines = sketchToLines(sketch, ignoreConstruction = true)
        val faces = findFaces(lines)

        faces.filter { it.type == FaceType.Surface }.minByOrNull { it.positions.minOf { it.x } }?.let {
            volumes.add(Extrude(it, plane,  height).extrude())
        }
    }
}
