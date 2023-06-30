import com.codecad.core.*
import com.codecad.core.parser.Parser
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun cacheTest(){
        var expr = Parser.parse("a + b - c")

        println(expr)
    }

}
