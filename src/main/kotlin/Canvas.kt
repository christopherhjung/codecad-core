import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.highgui.HighGui
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

class Canvas {
    val matrix: Mat
    val scale = 1024.0 / 5
    val size = 1024
    init{
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        matrix =  Mat.zeros(size,size, CvType.CV_8UC3)



        // Drawing a line

        // Drawing a line

    }

    fun getPoint(x: Double, y: Double) : Point{
        return Point( x * scale + size/2,y * scale + size/2)
    }

    fun line(x: Double, y: Double, x2: Double, y2: Double, construction: Boolean = false){
        Imgproc.line(
            matrix,  //Matrix obj of the image
            getPoint(x,y),  //p1
            getPoint(x2,y2),  //p2
            if(construction) Scalar(0.0, 0.0, 255.0) else Scalar(0.0, 255.0, 0.0),  //Scalar object for color
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

    fun arc(x: Double, y: Double, rad: Double, start: Double, end: Double){
        Imgproc.ellipse(
            matrix,  //Matrix obj of the image
            getPoint(x,y),  //p1
            Size(rad * scale + 0.5, rad * scale + 0.5),
            0.0,
            Math.toDegrees(start),
            Math.toDegrees(end),
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
        Core.flip(matrix, matrix, 0);
        Imgcodecs.imwrite("out${counter++}.png", matrix)
        matrix.release()
    }
}

var counter = 0
