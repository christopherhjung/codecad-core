package com.codecad.core.parser.ast.complex

import com.codecad.core.Project
import com.codecad.core.Sketch
import com.codecad.core.parser.ast.primitive.Expr
import com.codecad.core.scope.NestedScope
import com.codecad.core.scope.Scope
import com.codecad.core.scope.*
import com.codecad.core.sketch.World

class SketchExpr(
    world: World,
    var name: String,
    var body: Expr) : Expr(world) {

    override fun eval(scope: Scope): Any? {
        val project = scope.project
        val sketch = Sketch(project)
        project.sketches.add(sketch)
        val nestedScope = NestedScope.mutual(scope)
        nestedScope.sketch = sketch
        val result = body.eval(nestedScope)
        sketch.solve(1e-6)
        return result
    }

    override fun bind(scope: Scope, define: Boolean): Expr {
        val fnScope: Scope = NestedScope.mutual(scope)
        val newBody = body.bind(fnScope, false)
        return SketchExpr(world, name, newBody)
    }
}