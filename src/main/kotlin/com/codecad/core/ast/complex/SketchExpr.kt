package com.codecad.core.ast.complex

import com.codecad.core.part.Sketch
import com.codecad.core.ast.primitive.Expr
import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.scope.partStudio
import com.codecad.core.scope.sketch
import com.codecad.core.World

class SketchExpr(
    world: World,
    var name: String,
    var body: Expr) : Expr(world) {

    override fun eval(scope: Scope): Any? {
        val project = scope.partStudio
        val sketch = Sketch(project, name)
        project.sketches.add(sketch)
        val nestedScope = NestedScope.mutual(scope)
        nestedScope.sketch = sketch
        val result = body.eval(nestedScope)
        //sketch.solve(1e-6)
        return result
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val fnScope: Scope = NestedScope.mutual(scope)
        val newBody = body.bind(fnScope, false)
        return com.codecad.core.ast.complex.SketchExpr(world, name, newBody)
    }
}