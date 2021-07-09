
import javax.script.ScriptEngineManager


class Util{
    companion object{
        fun load(name: String) : String{
            return String(Util::class.java.classLoader.getResourceAsStream(name)?.readAllBytes()!!)
        }
    }
}

fun main(args: Array<String>) {
    val file = Util.load("model.kts")
    Executor.execute(file)
}


