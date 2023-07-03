package com.codecad.core

import com.codecad.common.LineError
import com.codecad.core.part.Executor
import com.codecad.core.exception.LineException
import com.codecad.core.test.mapModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class Wrapper(val errors: List<LineError> = mutableListOf())

@RestController
@RequestMapping("api")
class Controller {
    @PostMapping("eval")
    fun eval(@RequestBody code: String) : ResponseEntity<Any>{
        try{
            val result = Executor.execute(code)
            val project = result.project
            val model = mapModel(project)
            return ResponseEntity(model, HttpStatus.OK)
        }catch (e: LineException){
            e.printStackTrace()
            return ResponseEntity(Wrapper(e.locations), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}
