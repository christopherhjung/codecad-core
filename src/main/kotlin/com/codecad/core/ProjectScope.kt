package com.codecad.core

class ProjectScope(val project: Project){
    fun sketch(init: SketchScope.() -> Unit): Sketch {
        val builder = SketchScope(project)
        builder.init()
        project.sketches.add(builder.sketch)
        builder.sketch.solve(1e-8)
        return builder.sketch
    }

    fun extrude(sketch: Sketch, pointer: Point, height: Value) {
        val face = findFace(sketchToLines(sketch), pointer.fixed())

        if(face != null){
            project.volumes.add(Extrude(face, height))
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

