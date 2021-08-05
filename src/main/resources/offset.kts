import com.codecad.core.*
import com.codecad.core.Value.Companion.cos
import com.codecad.core.Value.Companion.sin
import com.codecad.core.SketchScope.Companion.ORIGIN




fun SketchScope.cycloid(bR: Value, sR: Value): FunctionFigure {
    return func { t ->
        val r = t*Math.PI*2
        val combined = sR + bR
        Point(combined*cos(r) - sR*cos(combined*(r/sR)) ,
            combined*sin(r) - sR*sin(combined*(r/sR)))
    }
}


project {
    val teeth = 10
    val offset = 0.1
    val road = 0.15

    val a = sketch {

        offset(Point(Const(0.0),Const(0.0)), -0.1){
            cycloid(const(teeth * offset),const(offset))
        }


        pattern(5, ORIGIN){
            circle(constPoint(0.5,0), const(0.2))
        }
    }

    extrude(a, Point(Const(0.0),Const(0.0)), Const(0.1))

}






