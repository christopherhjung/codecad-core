import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class FigureTest {

    @Test
    fun arcTest(){
        project {
            sketch {
                val arc = arc(point(0.0,0.0), point(1.0,0.0), param(-1.0))

                println(arc.center)
            }
        }
    }

}
