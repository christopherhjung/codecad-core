package com.codecad.core.import.step.ast

import com.codecad.core.ast.primitive.TupleExpr
import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.curve.*
import com.codecad.core.brep.surface.*

open class StepDef{
    open fun bind(scope : Map<Int, StepDef>) : StepDef{
        return this
    }

    open fun doubleValue() : Double{
        throw RuntimeException()
    }

    open fun intValue() : Int{
        throw RuntimeException()
    }

    open fun booleanValue() : Boolean{
        throw RuntimeException()
    }

    fun asTuple() : StepTuple{
        return this as StepTuple
    }
}

class StepObject(val type : String, val args: Array<StepDef>) : StepDef(){
    override fun bind(scope: Map<Int, StepDef>): StepDef {
        for( idx in args.indices ){
            args[idx] = args[idx].bind(scope)
        }

        return this
    }

    operator fun get(idx : Int) : StepDef{
        return args[idx]
    }
}

class StepMultiObject(val objs: Array<StepObject>) : StepDef(){
    override fun bind(scope: Map<Int, StepDef>): StepDef {
        for( idx in objs.indices ){
            objs[idx] = objs[idx].bind(scope) as StepObject
        }

        return this
    }
}

class StepTuple(val elems : Array<StepDef>) : StepDef(){
    override fun bind(scope: Map<Int, StepDef>): StepDef {
        for( idx in elems.indices ){
            elems[idx] = elems[idx].bind(scope)
        }

        return this
    }
}

class StepInstance(val idx : Int) : StepDef(){
    override fun bind(scope: Map<Int, StepDef>): StepDef {
        return scope[idx]!!
    }
}

class StepFile(val defs : Array<StepDef>) : StepDef(){
    fun findObjects(type: String) : List<StepObject>{
        return defs.filterIsInstance<StepObject>()
            .filter { it.type == type }
    }
}
data class StepDouble(val value : Double) : StepDef(){
    override fun doubleValue(): Double {
        return value
    }
}
data class StepInt(val value : Int) : StepDef(){
    override fun doubleValue(): Double {
        return value.toDouble()
    }

    override fun intValue(): Int {
        return value
    }
}
data class StepString(val value : String) : StepDef()
data class StepBoolean(val value : Boolean) : StepDef(){
    override fun booleanValue(): Boolean {
        return value
    }
}
object StepNull : StepDef()




val StepFile.styledItems get() = findObjects("STYLED_ITEM")
val StepObject.brep get() = when(type){
    "STYLED_ITEM" -> args[2] as StepObject
    else -> throw RuntimeException()
}
val StepObject.shell get() = when(type){
    "MANIFOLD_SOLID_BREP" -> args[1] as StepObject
    else -> throw RuntimeException()
}

val StepObject.advancedFaces get() = when(type){
    "CLOSED_SHELL" -> (args[1] as StepTuple).elems
    else -> throw RuntimeException()
}.map { it as StepObject }

val StepObject.faceBounds get() = when(type){
    "ADVANCED_FACE" -> (args[1] as StepTuple).elems
    else -> throw RuntimeException()
}.map { it as StepObject }

val StepObject.surface : Surface get() = when(type){
    "ADVANCED_FACE" -> (args[2] as StepObject).surface
    "PLANE" -> PlaneSurface(workplane)
    "CYLINDRICAL_SURFACE" -> CylindricalSurface(workplane, args[2].doubleValue())
    "TOROIDAL_SURFACE" -> ToroidalSurface(workplane, major, minor)
    "CONICAL_SURFACE" -> ConicalSurface(workplane, args[2].doubleValue(), args[3].doubleValue())
    "B_SPLINE_SURFACE_WITH_KNOTS" -> {
        val uDegree = args[1].intValue()
        val vDegree = args[2].intValue()
        val controlPoints = args[3].asTuple().elems.map {
            it.asTuple().vecs.map { BSplineControlPoint(it, 1.0) }.toTypedArray()
        }.toTypedArray()

        BSplineSurface(uDegree, vDegree, controlPoints)
    }
    else -> throw RuntimeException()
}

val StepObject.edgeLoop get() = when(type){
    "FACE_BOUND" -> args[1] as StepObject
    else -> throw RuntimeException()
}

val StepObject.orientedEdges get() = when(type){
    "EDGE_LOOP" -> (args[1] as StepTuple).elems.filterIsInstance<StepObject>()
    else -> throw RuntimeException()
}

val StepObject.edgeCurves get() = when(type){
    "ORIENTED_EDGE" -> args[3] as StepObject
    else -> throw RuntimeException()
}

val StepObject.sense get() = when(type){
    "ORIENTED_EDGE", "EDGE_CURVE" -> (args[4] as StepBoolean).value
    "ADVANCED_FACE" -> (args[3] as StepBoolean).value
    else -> throw RuntimeException()
}

val StepObject.start get() = when(type){
    "EDGE_CURVE" -> args[1] as StepObject
    "LINE" -> args[1] as StepObject
    else -> throw RuntimeException()
}

val StepObject.end get() = when(type){
    "EDGE_CURVE" -> args[2] as StepObject
    "LINE" -> args[2] as StepObject
    else -> throw RuntimeException()
}

val StepObject.curve : Curve get() = when(type){
    "EDGE_CURVE" -> (args[3] as StepObject).curve
    "LINE" -> Line(start.vec, end.vec)
    "CIRCLE" -> Circle(workplane, radius)
    "ELLIPSE" -> Ellipse(workplane, major, minor)
    "B_SPLINE_CURVE_WITH_KNOTS" -> {
        val dim = args[1].intValue()
        val vecs = args[2].vecs
        args[4].booleanValue()
        args[5].booleanValue()
        val weights = args[7].doubleArray()

        val controls = vecs.zip(weights).map {
            BSplineControlPoint(it.first, it.second)
        }.toTypedArray()
        BSpline(controls)
    }
    else -> throw RuntimeException()
}

val StepObject.workplane get() = run {
    val axisPlacement = when(type){
        "CIRCLE",
        "ELLIPSE",
        "PLANE",
        "CYLINDRICAL_SURFACE",
        "TOROIDAL_SURFACE",
        "CONICAL_SURFACE"
            -> args[1] as StepObject
        else -> throw RuntimeException()
    }

    Workplane(
        (axisPlacement[1] as StepObject).vec,
        (axisPlacement[2] as StepObject).vec,
        (axisPlacement[3] as StepObject).vec
    )
}

val StepObject.radius : Double get() = when(type){
    "CIRCLE" -> args[2].doubleValue()
    else -> throw RuntimeException()
}

val StepObject.major : Double get() = when(type){
    "ELLIPSE", "TOROIDAL_SURFACE" -> args[2].doubleValue()
    else -> throw RuntimeException()
}

val StepObject.minor : Double get() = when(type){
    "ELLIPSE", "TOROIDAL_SURFACE" -> args[3].doubleValue()
    else -> throw RuntimeException()
}

val StepObject.vec : Vec3 get() = when(type){
    "VERTEX_POINT" -> (args[1] as StepObject).vec
    "CARTESIAN_POINT", "DIRECTION" -> {
        val xyz = (args[1] as StepTuple).elems
        Vec3(
            xyz[0].doubleValue(),
            xyz[1].doubleValue(),
            xyz[2].doubleValue()
        )
    }
    "VECTOR" -> (args[1] as StepObject).vec * args[2].doubleValue()
    else -> throw RuntimeException()
}

val StepDef.vecs : List<Vec3> get() = let{
    if(this !is StepTuple) throw RuntimeException()
    elems.map{ (it as StepObject).vec }
}

fun StepDef.doubleArray() : List<Double>{
    if(this !is StepTuple) throw RuntimeException()
    return elems.map{ it.doubleValue() }
}



/*


fun importSolid(solid: StepObject){
    val shell = solid[1] as StepObject
    if(shell.type == "CLOSED_SHELL"){
        importClosedShell(shell)
    }
}

fun importClosedShell(shell: StepObject) : Shell {
    val advancedFaces = shell[1] as StepTuple
    return Shell(advancedFaces.elems.map { importFace(it as StepObject) })
}

fun importFace(advancedFace: StepObject) : Face {
    println("s")
    val faceBounds = (advancedFace[1] as StepTuple).elems.map { importFaceBound(it as StepObject) }
    val surface = advancedFace[1] as StepObject


    throw RuntimeException()
}

fun importFaceBound(faceBound : StepObject){
    val edgeLoop = faceBound[1]

}
*/

