import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.lang.Exception
import javax.script.*

class ExecutionResult(val output: String, val project: Project)

class Executor{
    companion object{
        fun execute(code: String) : ExecutionResult{
            with(ScriptEngineManager().getEngineByExtension("kts")) {
                return Executor().execute(this, code)
            }
        }
    }

    fun execute(engine: ScriptEngine, code: String) : ExecutionResult{
        val reset = System.out
        try{
            val newContext = SimpleScriptContext()
            val output = ByteArrayOutputStream()
            val printWriter = PrintWriter(output, true)
            newContext.writer = printWriter
            newContext.errorWriter = printWriter
            val stream = PrintStream(output)


            System.setOut(stream)
            System.setErr(stream)
            val project = engine.eval(code, newContext.getBindings(ScriptContext.ENGINE_SCOPE)) as Project
            stream.flush()
            return ExecutionResult(output.toString(), project)
        }catch (e: ScriptException){
            val cause = e.cause
            if(cause is LineException){
                throw cause
            }else{
                val locations = mutableListOf<Location>()
                val pattern = "^(?<msg>.+) \\((?<file>.+?):(?<line>\\d+):(?<column>\\d+)\\)$".toRegex()
                for(line in e.message?.lines() ?: emptyList()){
                    val result = pattern.matchEntire(line)
                    if(result != null){
                        locations.add(Location(result.groups[3]!!.value.toInt(), 0))
                    }
                }

                throw LineException(locations)
            }
        }finally {
            System.setOut(reset)
            System.setErr(reset)
        }
    }
}
