package com.codecad.core

import com.codecad.common.Model
import com.codecad.common.ModelCollection
import com.codecad.common.Path
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.runApplication
import java.io.File
import java.util.*
import java.io.IOException

import java.io.InputStream




class Util{
    companion object{
        fun load(name: String) : String{
            return String(Util::class.java.classLoader.getResourceAsStream(name)?.readAllBytes()!!)
        }
    }
}

@Throws(IOException::class)
fun readInputStreamWithTimeout(`is`: InputStream, b: ByteArray, timeoutMillis: Int): Int {
    var bufferOffset = 0
    val maxTimeMillis = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.size) {
        val readLength = Math.min(`is`.available(), b.size - bufferOffset)
        // can alternatively use bufferedReader, guarded by isReady():
        val readResult = `is`.read(b, bufferOffset, readLength)
        if (readResult == -1) break
        bufferOffset += readResult
    }
    return bufferOffset
}

fun main(args: Array<String>) {
    runApplication<Application>()

    /*
    //val code = String(System.`in`.readAllBytes())
    val code = Util.load("offset.kts")
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
    println(jsonModel)*/
}
