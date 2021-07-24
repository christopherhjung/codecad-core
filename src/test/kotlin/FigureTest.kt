import com.codecad.core.project
import com.codecad.core.sketch
import org.junit.jupiter.api.Test

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
