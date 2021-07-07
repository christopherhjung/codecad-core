import com.angusj.clipper.Clipper


sketch {
    val poly = polygon(
        point(0.0, -2.0),
        point(1.0, -1.0),
        point(2.0, -2.0),
        point(2.0, 2.0),
        point(1.0, 1.0),
        point(0.0, 2.0),
    )

    val poly2 = polygon(
        point(1.0, 0.2),
        point(1.05, 0.05),
        point(1.2, 0.0),
        point(1.0, 0.025),
    )

    //val clock = getAutocadFile("EdelstahlFrontV2.dxf")
    val shaftRadius = 0.39
    val offset = 0.2

    var paths = elementsToPath(poly)
    var paths2 = elementsToPath(poly2)

    paths = arrayOf(*paths, *paths2)
    var i = 0

    val innerPaths = mutableListOf<DoubleArray>()
    val outerPaths = mutableListOf<DoubleArray>()
    while(true){
        val toolPath =  Clipper().offsetPath(2.0,0.01, paths, -( shaftRadius + i * offset))

        pathsToPoly(toolPath, LineType.ToolPath)

        val innerPath = Clipper().offsetPath(2.0,0.01, toolPath, -shaftRadius)
        val outerPath = Clipper().offsetPath(2.0,0.01, toolPath, shaftRadius)

        //innerPaths.addAll(innerPath)
        outerPaths.addAll(outerPath)

        if(innerPath.isEmpty()){
            break
        }
        i++
    }

    /*val innerResult = Clipper().intersectPath(innerPaths.toTypedArray())
    pathsToPoly(innerResult, LineType.ToolContour)*/

    val outerResult = Clipper().unionPath(outerPaths.toTypedArray())
    pathsToPoly(outerResult, LineType.ToolContour)
}
