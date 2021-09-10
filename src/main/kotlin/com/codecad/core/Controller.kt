package com.codecad.core

import com.codecad.common.ExecutionResult
import com.codecad.common.LineError
import com.codecad.common.Model
import com.codecad.common.Path
import com.codecad.core.test.mapModel
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


            /*
            val mapper = ObjectMapper()
            val jsonModel = mapper.writeValueAsString(model)*/
            return ResponseEntity(mapModel(project), HttpStatus.OK)
        }catch (e: LineException){
            e.printStackTrace()
            return ResponseEntity(Wrapper(e.locations), HttpStatus.INTERNAL_SERVER_ERROR)
        }

    }

}
