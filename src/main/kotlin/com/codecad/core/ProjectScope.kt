package com.codecad.core

import com.codecad.common.Plane
import com.codecad.core.test.DirectedPlane

class ProjectScope(val project: Project){
    fun sketch(init: SketchScope.() -> Unit): Sketch {
        val builder = SketchScope(project)
        builder.init()
        project.sketches.add(builder.sketch)
        builder.sketch.solve(1e-8)
        return builder.sketch
    }

    fun extrude(sketch: Sketch, pointer: Point, height: Value) {
        val face = findFace(sketchToLines(sketch, ignoreConstruction = true), pointer.fixed())

        if(face != null){
            project.volumes.add(Extrude(face, DirectedPlane.XY, height))
        }
    }

    fun extrude(sketch: Sketch, height: Value, plane: DirectedPlane = DirectedPlane.XY) {
        val lines = sketchToLines(sketch, ignoreConstruction = true)
        val faces = findFaces(lines)

        for(face in faces){
            project.volumes.add(Extrude(face, plane,  height))
        }
    }
}

fun project(block: ProjectScope.() -> Unit) : Project {
    val project = Project()
    val projectScope = ProjectScope(project)
    try{
        block(projectScope)
    }catch (e: StackOverflowError){
        e.printStackTrace()
    }
    return project
}

