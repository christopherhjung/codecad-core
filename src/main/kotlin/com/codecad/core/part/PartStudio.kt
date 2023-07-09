package com.codecad.core.part

import com.codecad.core.*
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.World
import com.codecad.core.face.*
import com.codecad.core.face.entity.DirectedPlane
import com.codecad.core.volume.Extrude
import com.codecad.core.volume.Volume

class PartStudio {
    val sketches: MutableList<Sketch> = mutableListOf()
    val tracker: Tracker = Tracker()
    val volumes = mutableListOf<Volume>()
    val world = World()

    fun extrude(sketch: Sketch, height: Expr, plane: DirectedPlane = DirectedPlane.XY) {
        val lines = sketchToLines(sketch, ignoreConstruction = true)
        val rootHole = createFaceTree(lines)
        val surfaces = collectSurfaces(rootHole)

        val rootFaces = surfaces.first()
        volumes.add(Extrude(rootFaces, plane,  height).extrude())
    }

    fun extrudePos(sketch: Sketch, pos : Vec2, height: Expr, plane: DirectedPlane = DirectedPlane.XY) {
        val lines = sketchToLines(sketch, ignoreConstruction = true)
        val rootHole = createFaceTree(lines)
        val surfaces = collectSurfaces(rootHole)
        val faceFinder = FaceFinder(surfaces)
        faceFinder.find(pos.fixed())?.let {
            volumes.add(Extrude(it, plane,  height).extrude())
        }
    }
}
