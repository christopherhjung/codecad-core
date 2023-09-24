package com.codecad.core.import.step.ast

import com.codecad.core.ast.vec.Vec3
import com.codecad.core.brep.Workplane
import com.codecad.core.brep.curve.*
import com.codecad.core.brep.surface.*
import com.codecad.core.import.step.DefaultGeometricContext
import com.codecad.core.import.step.GeometricContext

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

    fun asObject() : StepObject{
        return this as StepObject
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
            objs[idx] = objs[idx].bind(scope).asObject()
        }

        return this
    }

    fun findObject(type: String) : StepObject?{
        return objs.find { it.type == type }
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

val StepFile.geometrics get() = findObjects("MECHANICAL_DESIGN_GEOMETRIC_PRESENTATION_REPRESENTATION")

val StepObject.geometricContext get() = when(type){
    "MECHANICAL_DESIGN_GEOMETRIC_PRESENTATION_REPRESENTATION"
        -> args[2] as StepMultiObject
    else -> throw RuntimeException()
}

val StepObject.styledItems get() = when(type){
    "MECHANICAL_DESIGN_GEOMETRIC_PRESENTATION_REPRESENTATION"
        -> args[1].asTuple().elems.filterIsInstance<StepObject>()
    else -> throw RuntimeException()
}

val StepObject.brep get() = when(type){
    "STYLED_ITEM" -> args[2].asObject()
    else -> throw RuntimeException()
}
val StepObject.shell get() = when(type){
    "MANIFOLD_SOLID_BREP" -> args[1].asObject()
    else -> throw RuntimeException()
}

val StepObject.advancedFaces get() = when(type){
    "CLOSED_SHELL" -> args[1].asTuple().elems
    else -> throw RuntimeException()
}.map { it.asObject() }

val StepObject.faceBounds get() = when(type){
    "ADVANCED_FACE" -> args[1].asTuple().elems
    else -> throw RuntimeException()
}.map { it.asObject() }

fun StepObject.surface(context: GeometricContext) : Surface{
    return when(type){
        "ADVANCED_FACE" -> (args[2].asObject()).surface(context)
        "PLANE" -> PlaneSurface(workplane(context))
        "CYLINDRICAL_SURFACE" -> CylindricalSurface(workplane(context), args[2].doubleValue())
        "TOROIDAL_SURFACE" -> ToroidalSurface(workplane(context), major, minor)
        "CONICAL_SURFACE" -> ConicalSurface(workplane(context), args[2].doubleValue(), args[3].doubleValue())
        "B_SPLINE_SURFACE_WITH_KNOTS" -> {
            val uDegree = args[1].intValue()
            val vDegree = args[2].intValue()
            val controlPoints = args[3].asTuple().elems.map {
                it.asTuple().vecs(context).map { BSplineControlPoint(it, 1.0) }.toTypedArray()
            }.toTypedArray()

            BSplineSurface(uDegree, vDegree, controlPoints)
        }
        else -> throw RuntimeException()
    }
}

val StepObject.edgeLoop get() = when(type){
    "FACE_BOUND" -> args[1].asObject()
    else -> throw RuntimeException()
}

val StepObject.orientedEdges get() = when(type){
    "EDGE_LOOP" -> args[1].asTuple().elems.filterIsInstance<StepObject>()
    else -> throw RuntimeException()
}

val StepObject.edgeCurves get() = when(type){
    "ORIENTED_EDGE" -> args[3].asObject()
    else -> throw RuntimeException()
}

val StepObject.sense get() = when(type){
    "ORIENTED_EDGE", "EDGE_CURVE" -> args[4].booleanValue()
    "ADVANCED_FACE" -> args[3].booleanValue()
    else -> throw RuntimeException()
}

val StepObject.start get() = when(type){
    "EDGE_CURVE" -> args[1].asObject()
    "LINE" -> args[1].asObject()
    else -> throw RuntimeException()
}

val StepObject.end get() = when(type){
    "EDGE_CURVE" -> args[2].asObject()
    "LINE" -> args[2].asObject()
    else -> throw RuntimeException()
}

fun StepObject.curve(context: GeometricContext) : Curve{
    return when(type){
        "EDGE_CURVE" -> (args[3].asObject()).curve(context)
        "LINE" -> Line(start.vec(context), end.vec(context))
        "CIRCLE" -> Circle(workplane(context), radius)
        "ELLIPSE" -> Ellipse(workplane(context), major, minor)
        "B_SPLINE_CURVE_WITH_KNOTS" -> {
            val degree = args[1].intValue()
            val vecs = args[2].vecs(context)
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
}

fun StepObject.workplane(context: GeometricContext) : Workplane{
    val axisPlacement = when(type){
        "CIRCLE",
        "ELLIPSE",
        "PLANE",
        "CYLINDRICAL_SURFACE",
        "TOROIDAL_SURFACE",
        "CONICAL_SURFACE"
            -> args[1].asObject()
        else -> throw RuntimeException()
    }

    return Workplane(
        axisPlacement[1].asObject().vec(context),
        axisPlacement[2].asObject().vec(context),
        axisPlacement[3].asObject().vec(context)
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

fun StepObject.vec(context : GeometricContext) : Vec3{
    return when(type){
        "VERTEX_POINT" -> (args[1].asObject()).vec(context)
        "CARTESIAN_POINT" -> {
            val xyz = args[1].asTuple().elems
            Vec3(
                xyz[0].doubleValue(),
                xyz[1].doubleValue(),
                xyz[2].doubleValue()
            ) * context.lengthFactor
        }
        "DIRECTION" -> {
            val xyz = args[1].asTuple().elems
            Vec3(
                xyz[0].doubleValue(),
                xyz[1].doubleValue(),
                xyz[2].doubleValue()
            )
        }
        "VECTOR" -> args[1].asObject().vec(context) * args[2].doubleValue()
        else -> throw RuntimeException()
    }
}

fun StepDef.vecs(context : GeometricContext) : List<Vec3>{
    if(this !is StepTuple) throw RuntimeException()
    return elems.map{ it.asObject().vec(context) }
}

fun StepDef.doubleArray() : List<Double>{
    if(this !is StepTuple) throw RuntimeException()
    return elems.map{ it.doubleValue() }
}




