import com.codecad.core.*
import com.codecad.core.parser.Parser
import com.codecad.core.sketch.Param
import com.codecad.core.sketch.World
import com.codecad.core.sketch.times
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ParserTest {

    @Test
    fun cacheTest(){
        var expr = Parser.parse("a + b - c")

        println(expr)
    }

}
