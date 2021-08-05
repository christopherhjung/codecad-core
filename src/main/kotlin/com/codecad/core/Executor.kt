package com.codecad.core

import com.codecad.common.LineError
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import javax.script.*

class ExecutionResult(val output: String, val project: Project)

class Executor{
    companion object{
        var engine: ScriptEngine? = null

        fun execute(code: String) : ExecutionResult {
            if(engine == null){
                engine = ScriptEngineManager().getEngineByExtension("kts")
            }

            return Executor().execute(engine!!, code)
        }
    }

    fun execute(engine: ScriptEngine, code: String) : ExecutionResult {
        val reset = System.out

        val newContext = SimpleScriptContext()
        val output = ByteArrayOutputStream()
        val printWriter = PrintWriter(output, true)
        newContext.writer = printWriter
        newContext.errorWriter = printWriter
        val stream = PrintStream(output)




        System.setOut(stream)
        System.setErr(stream)
        try{
            val project = engine.eval(code, newContext.getBindings(ScriptContext.ENGINE_SCOPE)) as? Project
            return ExecutionResult(output.toString(), project ?: Project())
        }catch (e: AccessDeniedException){
            println("Sicherheitsangriff")
            throw RuntimeException(e)
        } catch (e: ScriptException){
            val cause = e.cause
            if(cause is LineException){
                throw cause
            }else{
                val lineErrors = mutableListOf<LineError>()
                val pattern = "^(?<msg>.+) \\((?<file>.+?):(?<line>\\d+):(?<column>\\d+)\\)$".toRegex()
                for(line in e.message?.lines() ?: emptyList()){
                    val result = pattern.matchEntire(line)
                    if(result != null){

                        val msg = result.groups[1]!!.value
                        val file = result.groups[2]!!.value
                        val line = result.groups[3]!!.value.toInt()
                        val column = result.groups[4]!!.value.toInt()

                        lineErrors.add(LineError(msg, line, column))
                    }
                }

                throw LineException(lineErrors, output.toString() + " " + e.message)
            }
        }finally {
            System.setOut(reset)
            System.setErr(reset)
        }
    }
}
