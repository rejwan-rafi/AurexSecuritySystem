package Aurex;



import org.opencv.core.Core;

public class OpenCVTest {

    public static void main(String[] args) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        System.out.println("OpenCV Loaded Successfully!");

        System.out.println("Version: " + Core.VERSION);
    }
}