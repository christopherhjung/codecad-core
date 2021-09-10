package com.codecad.core.test

import com.codecad.common.Model
import com.codecad.common.Path
import com.codecad.core.*

fun mapModel(project: Project) : Model{
    val model = Model()

    val meshGenerator = MeshGenerator()

    for(sketch in project.sketches){
        for(figure in sketch.figures){
            if(figure is Point){
                model.points.add(figure.fixed())
            }else{
                val points = figureToPoints(figure)

                val path = Path()

                for(point in points){
                    path.points.add(point)
                }

                model.faces.add(path)
            }
        }
    }

    var result: FacedVolume? = null

    for(volume in project.volumes){
        if(result == null){
            result = FacedVolume.from(volume)
        }else{
            result = addVolumes(result, volume)
        }
    }

    if(result != null){
        model.volumes.add(meshGenerator.generate(result))
    }

    return model
}
