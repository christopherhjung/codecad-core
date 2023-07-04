package com.codecad.core.postprocess

import com.codecad.common.Model
import com.codecad.common.Path
import com.codecad.core.Vec2
import com.codecad.core.part.PartStudio
import com.codecad.core.mesh.MeshGenerator
import com.codecad.core.volume.FacedVolume

fun mapModel(partStudio: PartStudio) : Model{
    val model = Model()

    val meshGenerator = MeshGenerator()
    for(sketch in partStudio.sketches){
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
    for(volume in partStudio.volumes){
        result = if(result == null){
            FacedVolume.from(volume)
        }else{
            throw Error("no addition supported")
            //addVolumes(result, volume)
        }
    }

    if(result != null){
        model.volumes.add(meshGenerator.generate(result))
    }

    return model
}
