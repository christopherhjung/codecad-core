package com.codecad.core

class CodeToMesh {
    fun transform(code : String) : List<Mesh>{
        var executionResult = Executor.execute(code)
        val project = executionResult.project

        val objects = mutableListOf<Mesh>()

        val meshGenerator = MeshGenerator()

        for(volume in project.volumes){
            if(volume is Extrude){
                objects.add(meshGenerator.generate(volume))
            }
        }

        return objects
    }
}
