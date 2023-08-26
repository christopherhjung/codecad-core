package com.codecad.core.export

import com.codecad.core.mesh.Mesh


interface ModelExport {
    fun export(mesh: Mesh): ByteArray
}