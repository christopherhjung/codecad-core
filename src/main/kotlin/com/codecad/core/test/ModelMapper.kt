package com.codecad.core.test

import com.codecad.common.Model
import com.codecad.common.Path
import com.codecad.core.FacedVolume
import com.codecad.core.Vec2
import com.codecad.core.part.Project
import com.codecad.core.part.figureToPoints
import com.codecad.core.mesh.MeshGenerator

fun mapModel(project: Project) : Model{
    val model = Model()

    val meshGenerator = MeshGenerator()
    for(sketch in project.sketches){
        for(figure in sketch.figures){
            if(figure is Vec2){
                model.points.add(figure.fixed())
            }else{
                val plotter = figure.plotter()
                val path = Path()
                while( plotter.hasNext() ){
                    path.points.add(plotter.next().fixed())
                }

                model.faces.add(path)
            }
        }
    }

    var result: FacedVolume? = null
    for(volume in project.volumes){
        result = if(result == null){
            FacedVolume.from(volume)
        }else{
            addVolumes(result, volume)
        }
    }

    if(result != null){
        model.volumes.add(meshGenerator.generate(result))
    }

    return model
}
