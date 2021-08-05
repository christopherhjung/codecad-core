package com.codecad.core

import com.codecad.common.ExecutionResult
import com.codecad.common.LineError
import com.codecad.common.Model
import com.codecad.common.Path
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

class Wrapper(val errors: List<LineError> = mutableListOf()){

}

@RestController
@RequestMapping("api")
class Controller {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(Controller::class.java)
    }

    @PostMapping("eval")
    fun eval(@RequestBody code: String) : ResponseEntity<Any>{
        try{
            val result = Executor.execute(code)

            val project = result.project

            val output = ExecutionResult()
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
            return ResponseEntity(model, HttpStatus.OK)
        }catch (e: LineException){
            return ResponseEntity(Wrapper(e.locations), HttpStatus.INTERNAL_SERVER_ERROR)
        }

    }

}
