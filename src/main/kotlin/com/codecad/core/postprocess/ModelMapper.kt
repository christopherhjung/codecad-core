package com.codecad.core.postprocess

import com.codecad.common.Mesh
import com.codecad.common.Model

import com.codecad.core.mesh.MeshGenerator
import com.codecad.core.part.PartStudio

fun mapModel(partStudio: PartStudio) : Model{
   val model = Model()

    val meshGenerator = MeshGenerator()
    for( face in partStudio.context.faces ){
        meshGenerator.generate(face)
    }

    for( volume in partStudio.context.volumes ){
        meshGenerator.generate(volume)
    }
    val mesh = meshGenerator.build()
    val uiMesh = Mesh()
    uiMesh.indices = mesh.indices
    uiMesh.points = mesh.vertices
    model.volumes.add(uiMesh)
/*
  for(sketch in partStudio.sketches){
       for(figure in sketch.entities){
           if(figure is SketchLineSegmentExpr){
               val plotter = figure.plotter()
               val path = Path()
               while( plotter.hasNext() ){
                   path.points.add(plotter.next().fixed())
               }

               model.faces.add(path)
           }
       }
   }

   for(volume in partStudio.volumes){
       if(volume is FacedVolume){
           meshGenerator.add(volume)
       }
   }

    model.volumes.add(meshGenerator.build())*/

    return model
}
