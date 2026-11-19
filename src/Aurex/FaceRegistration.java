package Aurex;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.Locale;

import javax.imageio.ImageIO;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ImageIcon;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class FaceRegistration extends JFrame {

    // Employee Information Fields
    private JTextField idField;
    private JTextField nameField;
    private JTextField designationField;
    private JTextField phoneField;
    private JComboBox<String> departmentBox;

    // Camera Components
    private JLabel cameraLabel;
    private JLabel statusLabel;
    private JButton captureButton;
    private JButton stopButton;

    // OpenCV Components
    private VideoCapture camera;
    private Timer timer;
    private FaceDetectorYN detector;
    private FaceRecognizerSF recognizer;
    private Mat currentFrame;

    // Camera Settings
    private final int CAMERA_WIDTH = 640;
    private final int CAMERA_HEIGHT = 480;

    // Model Paths
    private final String YUNET_MODEL = "Models/face_detection_yunet_2023mar.onnx";
    private final String SFACE_MODEL = "Models/face_recognition_sface_2021dec.onnx";

    // Face Data Folder
    private final String DATA_FOLDER = "FaceData";

    // Constructor
    public FaceRegistration() {
        setTitle("AUREX - Employee Face Registration");
        setSize(1150, 750);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Header
        JLabel heading = new JLabel("AUREX - EMPLOYEE FACE REGISTRATION", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setForeground(new java.awt.Color(0, 150, 255));
        heading.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(heading, BorderLayout.NORTH);

        // Camera Panel
        JPanel cameraPanel = new JPanel(new BorderLayout());
        cameraPanel.setBorder(BorderFactory.createTitledBorder("LIVE CAMERA"));
        cameraLabel = new JLabel("Camera Starting...", SwingConstants.CENTER);
        cameraLabel.setPreferredSize(new Dimension(650, 480));
        cameraPanel.add(cameraLabel, BorderLayout.CENTER);

        // Employee Information Panel
        JPanel infoPanel = new JPanel(new java.awt.GridLayout(7, 1, 8, 8));
        infoPanel.setBorder(BorderFactory.createTitledBorder("EMPLOYEE INFORMATION"));

        idField = new JTextField();
        nameField = new JTextField();
        designationField = new JTextField();
        phoneField = new JTextField();

        // Department Dropdown
        String[] departments = {
                "Select Department",
                "Administration",
                "Security",
                "Police Operations",
                "Detective Branch",
                "Traffic Police",
                "Cyber Crime",
                "Intelligence",
                "Investigation",
                "Records and Documentation",
                "IT Support",
                "Control Room",
                "Special Branch"
        };

        departmentBox = new JComboBox<>(departments);
        departmentBox.setFont(new Font("Arial", Font.BOLD, 13));

        addField(infoPanel, "Employee ID", idField);
        addField(infoPanel, "Full Name", nameField);
        addField(infoPanel, "Designation", designationField);
        addField(infoPanel, "Department", departmentBox);
        addField(infoPanel, "Mobile Number", phoneField);

        // Capture Button
        captureButton = new JButton("CAPTURE & REGISTER");
        captureButton.setFont(new Font("Arial", Font.BOLD, 14));
        captureButton.setForeground(java.awt.Color.WHITE);
        captureButton.setBackground(new java.awt.Color(0, 120, 210));
        captureButton.setFocusPainted(false);
        captureButton.setEnabled(false);
        captureButton.addActionListener(e -> registerFace());
        infoPanel.add(captureButton);

        // Stop Camera Button
        stopButton = new JButton("STOP CAMERA");
        stopButton.setFont(new Font("Arial", Font.BOLD, 13));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopCamera());
        infoPanel.add(stopButton);

        // Center Layout
        JPanel centerPanel = new JPanel(new BorderLayout(12, 12));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        centerPanel.add(cameraPanel, BorderLayout.CENTER);
        centerPanel.add(infoPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        // Status Panel
        statusLabel = new JLabel("Status: Initializing...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(statusLabel, BorderLayout.SOUTH);

        setVisible(true);

        // Start Camera
        startCamera();
    }

    // Add Form Field
    private void addField(JPanel panel, String label, java.awt.Component field) {
        JPanel row = new JPanel(new BorderLayout(5, 3));
        JLabel title = new JLabel(label);
        title.setFont(new Font("Arial", Font.BOLD, 12));
        row.add(title, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
    }

    // Load YuNet and SFace Models
    private void loadModels() {
        File yunetFile = new File(YUNET_MODEL);
        File sfaceFile = new File(SFACE_MODEL);

        if (!yunetFile.exists()) {
            throw new RuntimeException("YuNet Model Not Found: " + yunetFile.getAbsolutePath());
        }

        if (!sfaceFile.exists()) {
            throw new RuntimeException("SFace Model Not Found: " + sfaceFile.getAbsolutePath());
        }

        detector = FaceDetectorYN.create(yunetFile.getAbsolutePath(), "", new Size(320, 320), 0.7f, 0.3f, 5000);
        recognizer = FaceRecognizerSF.create(sfaceFile.getAbsolutePath(), "");

        if (detector == null || recognizer == null) {
            throw new RuntimeException("Could not load Face Recognition Models!");
        }

        System.out.println("AUREX: YuNet and SFace Loaded Successfully!");
    }

    // Start Camera
    private void startCamera() {
        try {
            statusLabel.setText("Status: Loading OpenCV...");
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            loadModels();
            camera = null;

            // Search Camera Index 0 to 5
            for (int i = 0; i < 6; i++) {
                VideoCapture test = new VideoCapture(i, Videoio.CAP_DSHOW);
                if (test.isOpened()) {
                    Mat testFrame = new Mat();
                    boolean received = false;

                    for (int attempt = 0; attempt < 10; attempt++) {
                        received = test.read(testFrame);
                        if (received && !testFrame.empty()) break;
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    if (received && !testFrame.empty()) {
                        camera = test;
                        testFrame.release();
                        break;
                    } else {
                        testFrame.release();
                        test.release();
                    }
                } else {
                    test.release();
                }
            }

            if (camera == null) {
                statusLabel.setText("Status: Camera Not Found");
                JOptionPane.showMessageDialog(this, "Camera Not Found!");
                return;
            }

            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT);

            currentFrame = new Mat();

            timer = new Timer(40, e -> showCameraFrame());
            timer.start();

            captureButton.setEnabled(true);
            stopButton.setEnabled(true);

            statusLabel.setText("Status: Camera Running - Look at Camera");

        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Camera Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Show Live Camera and Detect Face
    private void showCameraFrame() {
        if (camera == null || !camera.isOpened()) return;

        Mat frame = new Mat();
        if (camera.read(frame) && !frame.empty()) {
            if (currentFrame != null) {
                currentFrame.release();
            }
            currentFrame = frame.clone();

            Mat faces = new Mat();
            detector.setInputSize(frame.size());
            detector.detect(frame, faces);

            // Draw Green Rectangle Around Face
            for (int i = 0; i < faces.rows(); i++) {
                double x = faces.get(i, 0)[0];
                double y = faces.get(i, 1)[0];
                double w = faces.get(i, 2)[0];
                double h = faces.get(i, 3)[0];

                Imgproc.rectangle(frame, new Point(x, y), new Point(x + w, y + h), new Scalar(0, 255, 0), 3);
            }

            // Update Status
            if (faces.rows() == 0) {
                statusLabel.setText("Status: No Face Detected");
            } else if (faces.rows() == 1) {
                statusLabel.setText("Status: One Face Detected - Ready to Register");
            } else {
                statusLabel.setText("Status: Multiple Faces Detected - Keep One Person");
            }

            cameraLabel.setIcon(matToIcon(frame));
            cameraLabel.setText("");

            faces.release();
        }
        frame.release();
    }

    // Convert Mat to Swing Image
    private ImageIcon matToIcon(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, buffer);

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(buffer.toArray()));
            if (image != null) {
                Image scaled = image.getScaledInstance(650, 480, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            buffer.release();
        }
        return null;
    }

    // Employee Face Registration
    private void registerFace() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String designation = designationField.getText().trim();
        String department = (String) departmentBox.getSelectedItem();
        String phone = phoneField.getText().trim();

        if (id.isEmpty() || name.isEmpty() || designation.isEmpty() || phone.isEmpty() || department == null || department.equals("Select Department")) {
            JOptionPane.showMessageDialog(this, "Please fill in all employee information!");
            return;
        }

        if (!id.matches("[A-Za-z0-9_-]+")) {
            JOptionPane.showMessageDialog(this, "Invalid Employee ID!\nUse only letters, numbers, _ and -.");
            return;
        }

        File personFolder = new File(DATA_FOLDER, id);
        if (personFolder.exists()) {
            JOptionPane.showMessageDialog(this, "This Employee ID is already registered!");
            return;
        }

        if (camera == null || !camera.isOpened() || currentFrame == null || currentFrame.empty()) {
            JOptionPane.showMessageDialog(this, "Camera is not ready!");
            return;
        }

        Mat faces = new Mat();
        Mat alignedFace = new Mat();
        Mat feature = new Mat();

        try {
            detector.setInputSize(currentFrame.size());
            detector.detect(currentFrame, faces);

            if (faces.rows() == 0) {
                JOptionPane.showMessageDialog(this, "No Face Detected!\nPlease look directly at the camera.");
                return;
            }

            if (faces.rows() > 1) {
                JOptionPane.showMessageDialog(this, "Multiple Faces Detected!\nOnly one person should be in front of camera.");
                return;
            }

            Mat faceBox = faces.row(0);
            recognizer.alignCrop(currentFrame, faceBox, alignedFace);
            recognizer.feature(alignedFace, feature);

            if (feature.empty()) {
                JOptionPane.showMessageDialog(this, "Face Feature Extraction Failed!");
                return;
            }

            File mainFolder = new File(DATA_FOLDER);
            if (!mainFolder.exists()) mainFolder.mkdirs();
            if (!personFolder.mkdir()) throw new IOException("Could not create employee folder!");

            // Save Face Image
            File imageFile = new File(personFolder, "face.jpg");
            Imgcodecs.imwrite(imageFile.getAbsolutePath(), alignedFace);

            // Extract Feature Array for exact formatting matching FaceRecognition
            float[] featureArray = new float[(int) feature.total()];
            feature.get(0, 0, featureArray);

            StringBuilder featureString = new StringBuilder();
            for (int i = 0; i < featureArray.length; i++) {
                featureString.append(String.format(Locale.US, "%.8f", featureArray[i]));
                if (i < featureArray.length - 1) {
                    featureString.append(",");
                }
            }

            // Save Employee Information data.txt
            File dataFile = new File(personFolder, "data.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile))) {
                writer.write("ID=" + id);
                writer.newLine();
                writer.write("Name=" + name);
                writer.newLine();
                writer.write("Designation=" + designation);
                writer.newLine();
                writer.write("Department=" + department);
                writer.newLine();
                writer.write("Phone=" + phone);
                writer.newLine();
                writer.write("Feature=" + featureString.toString());
                writer.newLine();
            }

            JOptionPane.showMessageDialog(this, "EMPLOYEE REGISTERED SUCCESSFULLY!\n\nID: " + id + "\nName: " + name + "\nDepartment: " + department);

            statusLabel.setText("Status: Registration Successful - " + name);

            // Clear Form
            idField.setText("");
            nameField.setText("");
            designationField.setText("");
            phoneField.setText("");
            departmentBox.setSelectedIndex(0);

        } catch (Exception ex) {
            deleteFolder(personFolder);
            JOptionPane.showMessageDialog(this, "Registration Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            faces.release();
            alignedFace.release();
            feature.release();
        }
    }

    private void deleteFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            folder.delete();
        }
    }

    private void stopCamera() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (camera != null) {
            camera.release();
            camera = null;
        }
        if (currentFrame != null) {
            currentFrame.release();
            currentFrame = null;
        }
        cameraLabel.setIcon(null);
        cameraLabel.setText("Camera Stopped");
        captureButton.setEnabled(false);
        stopButton.setEnabled(false);
        statusLabel.setText("Status: Camera Stopped");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FaceRegistration());
    }

    @Override
    public void dispose() {
        stopCamera();
        super.dispose();
    }
}