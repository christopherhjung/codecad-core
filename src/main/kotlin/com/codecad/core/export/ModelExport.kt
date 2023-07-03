package com.codecad.core.export

import com.codecad.common.Mesh

interface ModelExport {
    fun export(mesh: Mesh): ByteArray
}