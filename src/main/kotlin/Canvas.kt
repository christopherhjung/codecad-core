import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

class Canvas {
    val matrix: Mat
    val scale = 1024.0 / 5
    init{
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        matrix =  Mat.zeros(1024,1024, CvType.CV_8UC3)

        // Drawing a line

        // Drawing a line

    }

    fun getPoint(x: Double, y: Double) : Point{
        return Point(24 + x * scale,1000 - y * scale)
    }

    fun line(x: Double, y: Double, x2: Double, y2: Double){
        Imgproc.line(
            matrix,  //Matrix obj of the image
            getPoint(x,y),  //p1
            getPoint(x2,y2),  //p2
            Scalar(0.0, 255.0, 0.0),  //Scalar object for color
            1, //Thickness of the line
            Imgproc.LINE_AA
        )
    }

    fun circle(x: Double, y: Double, rad: Double){
        Imgproc.circle(
            matrix,  //Matrix obj of the image
            getPoint(x,y),  //p1
            (rad * scale + 0.5).toInt(),
            Scalar(0.0, 255.0, 0.0),  //Scalar object for color
            1, //Thickness of the line
            Imgproc.LINE_AA
        )
    }

    fun point(x: Double, y: Double){
        Imgproc.circle(
            matrix,  //Matrix obj of the image
            getPoint(x,y),  //p1
            (4).toInt(),
            Scalar(0.0, 0.0, 255.0),  //Scalar object for color
            Imgproc.FILLED, //Thickness of the line
            Imgproc.LINE_AA
        )
    }

    fun writeImage(){
        Imgcodecs.imwrite("out.png", matrix)
    }
}
