package de.lighti.clipper;

public class Main {
    public static void main(String[] args) {

        Path path = new Path();

        path.add(new Point.LongPoint(0,0));
        path.add(new Point.LongPoint(200,0));
        path.add(new Point.LongPoint(200,200));
        path.add(new Point.LongPoint(100,5));
        path.add(new Point.LongPoint(0,200));

        ClipperOffset offset = new ClipperOffset();


        offset.addPath(path, Clipper.JoinType.ROUND, Clipper.EndType.CLOSED_POLYGON);

        Paths paths = new Paths();


        offset.execute(paths, -10);


        System.out.println("sss");
    }
}
