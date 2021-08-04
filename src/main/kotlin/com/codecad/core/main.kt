package com.codecad.core

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*

class Util{
    companion object{
        fun load(name: String) : String{
            return String(Util::class.java.classLoader.getResourceAsStream(name)?.readAllBytes()!!)
        }
    }
}

fun main(args: Array<String>) {
    println(args.contentToString())
    val code = String(System.`in`.readAllBytes())
    println(code)
    val result = Executor.execute(code)
    val project = result.project

    val output = ModelCollection()
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

    for(volume in project.volumes){
        if(volume is Extrude){
            model.volumes.add(meshGenerator.generate(volume))
        }
    }

    val mapper = ObjectMapper()
    val jsonModel = mapper.writeValueAsString(model)
    println(jsonModel)
}
