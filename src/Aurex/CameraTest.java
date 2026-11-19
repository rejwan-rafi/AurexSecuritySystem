package Aurex;


import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

public class CameraTest {

    public static void main(String[] args) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture camera = null;

        // Camera index check
        for (int i = 0; i < 6; i++) {

            VideoCapture test = new VideoCapture(i);

            if (test.isOpened()) {

                Mat frame = new Mat();

                if (test.read(frame) && !frame.empty()) {

                    System.out.println("Camera found: " + i);

                    camera = test;
                    break;

                } else {

                    test.release();
                }

            } else {

                test.release();
            }
        }

        if (camera == null) {

            System.out.println("No Camera Found!");
            return;
        }

        Mat frame = new Mat();

        while (true) {

            if (camera.read(frame) && !frame.empty()) {

                HighGui.imshow("AUREX Camera Test", frame);

            }

            if (HighGui.waitKey(30) == 27) {
                break;
            }
        }

        camera.release();
        HighGui.destroyAllWindows();
    }
}
