import com.codecad.common.PointD
import com.codecad.core.ClipperOffset
import de.lighti.clipper.Clipper
import org.junit.jupiter.api.Test

class OffsetTest {

    @Test
    fun test(){
        val offset = ClipperOffset()

        val points = mutableListOf<PointD>()

        points.add(PointD(0.0,0.0))
        points.add(PointD(100.0,0.0))
        points.add(PointD(100.0,100.0))
        points.add(PointD(0.0,100.0))

        offset.addPath(points, Clipper.JoinType.ROUND, Clipper.EndType.CLOSED_POLYGON)

        offset.execute(10.0)
    }
}
