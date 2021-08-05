package com.codecad.core

import com.codecad.common.Model
import com.codecad.common.ModelCollection
import com.codecad.common.Path
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api")
class Controller {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(Controller::class.java)
    }

    @PostMapping("eval")
    fun eval(@RequestBody code: String) : Model{
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

        /*
        val mapper = ObjectMapper()
        val jsonModel = mapper.writeValueAsString(model)*/
        return model
    }

}
