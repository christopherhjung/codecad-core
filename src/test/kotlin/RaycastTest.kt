import com.codecad.common.Line
import com.codecad.common.Plane
import com.codecad.common.PointD
import com.codecad.core.*
import com.codecad.core.test.Node
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap

class RaycastTest {

    @Test
    fun cacheTest(){
        val ray = Ray(
            PointD(0.0,0.0,0.0),
            PointD(0.0,0.0,1.0)
        )

        val triangle = TriangleFace(
            Node(PointD(-0.4,0.0,-1.0)),
            Node(PointD(0.4,0.4,-1.0)),
            Node(PointD(0.4,-0.4,-1.0))
        )

        val raycast = Raycast()
        val result = raycast.raycast(triangle, ray)

        print("s")
    }



    @Test
    fun planeTest2(){

        val a = PointD(1.0,1.0,0.0)
        val b = PointD(1.0,0.0,0.0)

        val test = b - b.projectTo(a)

        println(b.projectTo(a))
        println(test)
    }

    @Test
    fun planeTest(){
        /*val a = PointD(1.0,2.0,3.0)
        val b = PointD(1.0,3.0,3.0)
        val c = PointD(2.0,2.0,2.0)*/

        val a = PointD(-0.4,0.0,-1.0)
        val b = PointD(0.4,0.4,-1.0)
        val c = PointD(0.4,-0.4,-1.0)

        //a*b/(a*a) * a

        val plane = Plane.fromPoints(a,b,c)
        val xPlane = Plane(PointD(1.0,0.0,0.0), 0.0)

        Line.fromPlanes(plane,xPlane)
    }

    @Test
    fun planeTest3(){
        val a = Plane(PointD(1.0,2.0,1.0), 1.0)
        val b = Plane(PointD(2.0,-3.0,2.0), 2.0)
        val line = Line.fromPlanes(a,b)
    }

    @Test
    fun test(){

    }


}
