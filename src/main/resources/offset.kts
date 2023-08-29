import com.codecad.core.*
import com.codecad.core.Expr.Companion.cos
import com.codecad.core.Expr.Companion.sin
import com.codecad.core.face.brep.Vec2Expr
import com.codecad.core.part.SketchScope
import com.codecad.core.part.SketchScope.Companion.ORIGIN


fun SketchScope.cycloid(bR: Expr, sR: Expr): FunctionFigure {
    return func { t ->
        val r = t*Math.PI*2
        val combined = sR + bR
        Vec2(combined*cos(r) - sR*cos(combined*(r/sR)) ,
            combined*sin(r) - sR*sin(combined*(r/sR)))
    }
}


project {
    val teeth = 10
    val offset = 0.1
    val road = 0.15

    val a = sketch {

        offset(Vec2Expr(Literal(0.0),Literal(0.0)), -0.1){
            cycloid(literal(teeth * offset),literal(offset))
        }


        pattern(5, ORIGIN){
            circle(literalPoint(0.5,0), literal(0.2))
        }
    }

    extrude(a, Vec2Expr(Literal(0.0),Literal(0.0)), Literal(0.1))

}






