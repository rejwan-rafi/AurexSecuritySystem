package Aurex;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Color;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.Timer;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

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


public class FaceRecognition extends JFrame {

    // ===============================
    // CAMERA COMPONENTS
    // ===============================

    private JLabel cameraLabel;
    private JLabel statusLabel;

    private JLabel employeeNameLabel;
    private JLabel employeeIdLabel;
    private JLabel departmentLabel;
    private JLabel attendanceLabel;

    private JTextField guardField;

    private JButton checkInButton;
    private JButton checkOutButton;
    private JButton stopButton;
    private JButton registerButton;


    // ===============================
    // OPENCV COMPONENTS
    // ===============================

    private VideoCapture camera;
    private Timer timer;

    private FaceDetectorYN detector;
    private FaceRecognizerSF recognizer;


    // ===============================
    // EMPLOYEE DATA
    // ===============================

    private final List<Employee> employees =
            new ArrayList<>();

    private Employee currentEmployee;

    private double bestMatchScore = -1;


    // ===============================
    // ATTENDANCE STATE
    // ===============================

    private final Map<String, String> lastActionByEmployee =
            new HashMap<>();


    // ===============================
    // FILE PATHS
    // ===============================

    private final String YUNET_MODEL =
            "Models/face_detection_yunet_2023mar.onnx";

    private final String SFACE_MODEL =
            "Models/face_recognition_sface_2021dec.onnx";

    private final String DATA_FOLDER =
            "FaceData";

    private final String ATTENDANCE_FOLDER =
            "AurexSecurityData/EmployeeAttendance";

    private final String CSV_FILE =
            ATTENDANCE_FOLDER + "/attendance.csv";

    private final String TXT_FILE =
            ATTENDANCE_FOLDER + "/attendance.txt";


    // ===============================
    // CAMERA SETTINGS
    // ===============================

    private final int CAMERA_WIDTH = 640;
    private final int CAMERA_HEIGHT = 480;


    // ===============================
    // MATCHING THRESHOLD
    // ===============================

    private final double MATCH_THRESHOLD = 0.363;


    // ===============================
    // DATE FORMAT
    // ===============================

    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );


    // ===============================
    // EMPLOYEE CLASS
    // ===============================

    private static class Employee {

        String id;
        String name;
        String designation;
        String department;
        String phone;

        float[] feature;

        Employee(
                String id,
                String name,
                String designation,
                String department,
                String phone,
                float[] feature
        ) {

            this.id = id;
            this.name = name;
            this.designation = designation;
            this.department = department;
            this.phone = phone;
            this.feature = feature;
        }
    }


    // ===============================
    // CONSTRUCTOR
    // ===============================

    public FaceRecognition() {

        setTitle(
                "AUREX - Employee Face Recognition & Attendance"
        );

        setSize(1250, 800);

        setMinimumSize(
                new Dimension(1050, 700)
        );

        setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        setLocationRelativeTo(null);

        setLayout(
                new BorderLayout(10, 10)
        );


        // HEADER

        JLabel heading = new JLabel(
                "AUREX - SECURITY CHECKPOINT",
                SwingConstants.CENTER
        );

        heading.setFont(
                new Font("Arial", Font.BOLD, 25)
        );

        heading.setForeground(
                new Color(0, 150, 255)
        );

        heading.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 10, 15, 10
                )
        );

        add(heading, BorderLayout.NORTH);


        // CAMERA PANEL

        JPanel cameraPanel =
                new JPanel(new BorderLayout());

        cameraPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "LIVE FACE RECOGNITION"
                )
        );

        cameraLabel = new JLabel(
                "Starting Camera...",
                SwingConstants.CENTER
        );

        cameraLabel.setPreferredSize(
                new Dimension(750, 580)
        );

        cameraPanel.add(
                cameraLabel,
                BorderLayout.CENTER
        );


        // EMPLOYEE INFORMATION PANEL

        JPanel infoPanel =
                new JPanel(new BorderLayout(8, 8));

        infoPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "EMPLOYEE INFORMATION"
                )
        );

        infoPanel.setPreferredSize(
                new Dimension(330, 580)
        );


        JPanel detailsPanel = new JPanel();

        detailsPanel.setLayout(
                new javax.swing.BoxLayout(
                        detailsPanel,
                        javax.swing.BoxLayout.Y_AXIS
                )
        );

        detailsPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        12, 12, 12, 12
                )
        );


        employeeNameLabel = createInfoLabel(
                "Name: Waiting..."
        );

        employeeIdLabel = createInfoLabel(
                "Employee ID: ---"
        );

        departmentLabel = createInfoLabel(
                "Department: ---"
        );

        attendanceLabel = createInfoLabel(
                "Attendance: ---"
        );


        detailsPanel.add(employeeNameLabel);
        detailsPanel.add(employeeIdLabel);
        detailsPanel.add(departmentLabel);
        detailsPanel.add(attendanceLabel);

        detailsPanel.add(new JLabel(" "));


        // GUARD FIELD

        JLabel guardTitle = new JLabel(
                "Guard Name / ID"
        );

        guardTitle.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        detailsPanel.add(guardTitle);


        guardField = new JTextField();

        guardField.setMaximumSize(
                new Dimension(300, 35)
        );

        guardField.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );

        detailsPanel.add(guardField);

        detailsPanel.add(new JLabel(" "));


        // CHECK IN BUTTON

        checkInButton = new JButton("CHECK IN");

        styleButton(
                checkInButton,
                new Color(0, 150, 90)
        );

        checkInButton.setEnabled(false);

        checkInButton.addActionListener(
                e -> checkInEmployee()
        );


        // CHECK OUT BUTTON

        checkOutButton = new JButton("CHECK OUT");

        styleButton(
                checkOutButton,
                new Color(190, 55, 55)
        );

        checkOutButton.setEnabled(false);

        checkOutButton.addActionListener(
                e -> checkOutEmployee()
        );


        // STOP CAMERA BUTTON

        stopButton = new JButton("STOP CAMERA");

        styleButton(
                stopButton,
                new Color(65, 75, 95)
        );

        stopButton.setEnabled(false);

        stopButton.addActionListener(
                e -> stopCamera()
        );


        // NEW EMPLOYEE REGISTRATION BUTTON

        registerButton = new JButton(
                "REGISTER NEW EMPLOYEE"
        );

        styleButton(
                registerButton,
                new Color(0, 110, 200)
        );

        registerButton.addActionListener(
                e -> registerNewEmployee()
        );


        // ADD BUTTONS

        detailsPanel.add(checkInButton);
        detailsPanel.add(new JLabel(" "));

        detailsPanel.add(checkOutButton);
        detailsPanel.add(new JLabel(" "));

        detailsPanel.add(stopButton);
        detailsPanel.add(new JLabel(" "));

        detailsPanel.add(registerButton);


        infoPanel.add(
                detailsPanel,
                BorderLayout.CENTER
        );


        // CENTER PANEL

        JPanel centerPanel =
                new JPanel(new BorderLayout(10, 10));

        centerPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 12, 10, 12
                )
        );

        centerPanel.add(
                cameraPanel,
                BorderLayout.CENTER
        );

        centerPanel.add(
                infoPanel,
                BorderLayout.EAST
        );

        add(centerPanel, BorderLayout.CENTER);


        // STATUS PANEL

        statusLabel = new JLabel(
                "Status: Initializing...",
                SwingConstants.CENTER
        );

        statusLabel.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        statusLabel.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 10, 10, 10
                )
        );

        add(statusLabel, BorderLayout.SOUTH);


        setVisible(true);

        startRecognition();
    }


    // ===============================
    // CREATE INFORMATION LABEL
    // ===============================

    private JLabel createInfoLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        label.setAlignmentX(LEFT_ALIGNMENT);

        label.setBorder(
                BorderFactory.createEmptyBorder(
                        8, 0, 8, 0
                )
        );

        return label;
    }


    // ===============================
    // STYLE BUTTONS
    // ===============================

    private void styleButton(
            JButton button,
            Color color
    ) {

        button.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        button.setForeground(Color.WHITE);

        button.setBackground(color);

        button.setFocusPainted(false);

        button.setMaximumSize(
                new Dimension(300, 45)
        );

        button.setAlignmentX(LEFT_ALIGNMENT);
    }


    // ===============================
    // LOAD MODELS
    // ===============================

    private void loadModels() {

        File yunet = new File(YUNET_MODEL);

        File sface = new File(SFACE_MODEL);


        if (!yunet.exists()) {

            throw new RuntimeException(
                    "YuNet Model Not Found!"
            );
        }

        if (!sface.exists()) {

            throw new RuntimeException(
                    "SFace Model Not Found!"
            );
        }


        detector = FaceDetectorYN.create(
                yunet.getAbsolutePath(),
                "",
                new Size(320, 320),
                0.7f,
                0.3f,
                5000
        );


        recognizer = FaceRecognizerSF.create(
                sface.getAbsolutePath(),
                ""
        );


        System.out.println(
                "AUREX: Models Loaded!"
        );
    }


    // ===============================
    // LOAD EMPLOYEES
    // ===============================

    private void loadEmployees() {

        employees.clear();

        File mainFolder =
                new File(DATA_FOLDER);


        if (!mainFolder.exists()) {

            mainFolder.mkdirs();
        }


        File[] folders =
                mainFolder.listFiles();


        if (folders == null) {
            return;
        }


        for (File folder : folders) {

            if (!folder.isDirectory()) {
                continue;
            }


            File dataFile = new File(
                    folder,
                    "data.txt"
            );


            if (!dataFile.exists()) {
                continue;
            }


            String id = "";
            String name = "";
            String designation = "";
            String department = "";
            String phone = "";

            float[] feature = null;


            try (
                    BufferedReader reader =
                    new BufferedReader(
                            new FileReader(dataFile)
                    )
            ) {

                String line;


                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("ID=")) {

                        id = line.substring(3).trim();

                    } else if (line.startsWith("Name=")) {

                        name = line.substring(5).trim();

                    } else if (line.startsWith("Designation=")) {

                        designation =
                                line.substring(12).trim();

                    } else if (line.startsWith("Department=")) {

                        department =
                                line.substring(11).trim();

                    } else if (line.startsWith("Phone=")) {

                        phone =
                                line.substring(6).trim();

                    } else if (line.startsWith("Feature=")) {

                        String values =
                                line.substring(8).trim();

                        String[] parts =
                                values.split(",");

                        feature =
                                new float[parts.length];


                        for (int i = 0; i < parts.length; i++) {

                            feature[i] = Float.parseFloat(
                                    parts[i].trim()
                            );
                        }
                    }
                }


                if (!id.isEmpty()
                        && !name.isEmpty()
                        && feature != null
                        && feature.length == 128) {

                    employees.add(
                            new Employee(
                                    id,
                                    name,
                                    designation,
                                    department,
                                    phone,
                                    feature
                            )
                    );
                }


            } catch (Exception ex) {

                System.out.println(
                        "Employee Load Error: "
                        + folder.getName()
                );

                ex.printStackTrace();
            }
        }


        System.out.println(
                "Total Employees: " + employees.size()
        );

        for (Employee emp : employees) {

            System.out.println(
                    "LOADED EMPLOYEE: "
                    + emp.id
                    + " | "
                    + emp.name
                    + " | Feature Length: "
                    + emp.feature.length
            );
        }
    }


    // ===============================
    // MANUAL EMPLOYEE REGISTRATION
    // ===============================

    private void registerNewEmployee() {

        if (detector == null || recognizer == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Face Recognition Models are not loaded!"
            );

            return;
        }


        // EMPLOYEE INFORMATION FIELDS

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField designationField = new JTextField();
        JTextField departmentField = new JTextField();
        JTextField phoneField = new JTextField();


        JPanel form = new JPanel(
                new java.awt.GridLayout(0, 2, 8, 8)
        );

        form.add(new JLabel("Employee ID:"));
        form.add(idField);

        form.add(new JLabel("Full Name:"));
        form.add(nameField);

        form.add(new JLabel("Designation:"));
        form.add(designationField);

        form.add(new JLabel("Department:"));
        form.add(departmentField);

        form.add(new JLabel("Phone:"));
        form.add(phoneField);


        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "AUREX - New Employee Registration",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );


        if (result != JOptionPane.OK_OPTION) {
            return;
        }


        // READ INFORMATION

        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String designation = designationField.getText().trim();
        String department = departmentField.getText().trim();
        String phone = phoneField.getText().trim();


        // VALIDATION

        if (id.isEmpty()
                || name.isEmpty()
                || designation.isEmpty()
                || department.isEmpty()
                || phone.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please fill in all employee information!"
            );

            return;
        }


        if (!id.matches("[A-Za-z0-9_-]+")) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid Employee ID!\n"
                    + "Use only letters, numbers, _ or -."
            );

            return;
        }


        for (Employee emp : employees) {

            if (emp.id.equalsIgnoreCase(id)) {

                JOptionPane.showMessageDialog(
                        this,
                        "This Employee ID is already registered!"
                );

                return;
            }
        }


        File employeeFolder = new File(
                DATA_FOLDER,
                id
        );


        if (employeeFolder.exists()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Employee Folder already exists!\n"
                    + "Please check the existing FaceData."
            );

            return;
        }


        // SELECT IMAGE

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle(
                "Select Employee Face Photograph"
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Image Files (JPG, JPEG, PNG)",
                        "jpg", "jpeg", "png"
                )
        );


        int selected = chooser.showOpenDialog(this);


        if (selected != JFileChooser.APPROVE_OPTION) {
            return;
        }


        File selectedImage = chooser.getSelectedFile();


        // BIOMETRIC CONSENT CONFIRMATION

        int consent = JOptionPane.showConfirmDialog(
                this,
                "Confirm that this employee has given informed "
                + "consent for face data registration and that "
                + "you are authorized to process it.\n\n"
                + "Employee: " + name + "\n"
                + "ID: " + id,
                "AUREX - Biometric Registration Consent",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );


        if (consent != JOptionPane.YES_OPTION) {
            return;
        }


        // READ IMAGE

        Mat image = Imgcodecs.imread(
                selectedImage.getAbsolutePath()
        );

        Mat faces = new Mat();

        Mat faceRow = null;

        Mat alignedFace = new Mat();

        Mat feature = new Mat();


        try {

            if (image.empty()) {

                throw new Exception(
                        "Could not read selected image!"
                );
            }


            // YUNET FACE DETECTION

            detector.setInputSize(image.size());

            detector.detect(image, faces);


            if (faces.rows() != 1) {

                throw new Exception(
                        "Exactly ONE face must be detected.\n"
                        + "Detected faces: " + faces.rows()
                );
            }


            // SFACE FEATURE EXTRACTION

            faceRow = faces.row(0);

            recognizer.alignCrop(
                    image,
                    faceRow,
                    alignedFace
            );

            recognizer.feature(
                    alignedFace,
                    feature
            );


            if (feature.empty() || feature.total() == 0) {

                throw new Exception(
                        "Face Feature Extraction Failed!"
                );
            }


            float[] vector = new float[
                    (int) feature.total()
            ];


            feature.get(
                    0,
                    0,
                    vector
            );


            if (vector.length != 128) {

                throw new Exception(
                        "Unexpected SFace Feature Length: "
                        + vector.length
                );
            }


            // CREATE FOLDER

            File mainFolder = new File(DATA_FOLDER);

            if (!mainFolder.exists()
                    && !mainFolder.mkdirs()) {

                throw new IOException(
                        "Could not create FaceData Folder!"
                );
            }


            if (!employeeFolder.mkdir()) {

                throw new IOException(
                        "Could not create Employee Folder!"
                );
            }


            File savedImage = new File(
                    employeeFolder,
                    "face.jpg"
            );

            File dataFile = new File(
                    employeeFolder,
                    "data.txt"
            );


            try {

                // SAVE IMAGE

                if (!Imgcodecs.imwrite(
                        savedImage.getAbsolutePath(),
                        image
                )) {

                    throw new IOException(
                            "Could not save Employee Photo!"
                    );
                }


                // SAVE EMPLOYEE DATA AND FEATURE VECTOR

                try (BufferedWriter writer =
                        Files.newBufferedWriter(
                                dataFile.toPath(),
                                StandardCharsets.UTF_8
                        )) {

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

                    writer.write("Feature=");


                    for (int i = 0; i < vector.length; i++) {

                        if (i > 0) {
                            writer.write(",");
                        }

                        writer.write(
                                Float.toString(vector[i])
                        );
                    }

                    writer.newLine();
                }


                // RELOAD EMPLOYEE LIST

                loadEmployees();


                JOptionPane.showMessageDialog(
                        this,
                        "EMPLOYEE REGISTERED SUCCESSFULLY!\n\n"
                        + "Employee ID: " + id + "\n"
                        + "Name: " + name + "\n"
                        + "Designation: " + designation + "\n"
                        + "Department: " + department + "\n"
                        + "Phone: " + phone + "\n"
                        + "Feature Length: " + vector.length
                );


                System.out.println(
                        "NEW EMPLOYEE REGISTERED: "
                        + id
                        + " | "
                        + name
                );


            } catch (Exception ex) {

                Files.deleteIfExists(dataFile.toPath());

                Files.deleteIfExists(savedImage.toPath());

                Files.deleteIfExists(
                        employeeFolder.toPath()
                );

                throw ex;
            }


        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "REGISTRATION FAILED!\n\n"
                    + ex.getMessage(),
                    "AUREX Registration Error",
                    JOptionPane.ERROR_MESSAGE
            );

            ex.printStackTrace();


        } finally {

            image.release();

            faces.release();

            if (faceRow != null) {
                faceRow.release();
            }

            alignedFace.release();

            feature.release();
        }
    }


    // ===============================
    // PREPARE ATTENDANCE FILES
    // ===============================

    private void prepareAttendanceFiles()
            throws IOException {

        File folder =
                new File(ATTENDANCE_FOLDER);


        if (!folder.exists() && !folder.mkdirs()) {

            throw new IOException(
                    "Could not create Attendance Folder!"
            );
        }


        File csv = new File(CSV_FILE);


        if (!csv.exists()) {

            try (BufferedWriter writer =
                    Files.newBufferedWriter(
                            csv.toPath(),
                            StandardCharsets.UTF_8
                    )) {

                writer.write(
                        "EmployeeID,Name,Department,Action,DateTime,Guard"
                );

                writer.newLine();
            }
        }


        File txt = new File(TXT_FILE);


        if (!txt.exists()) {
            txt.createNewFile();
        }
    }


    // ===============================
    // LOAD ATTENDANCE STATE
    // ===============================

    private void loadAttendanceState()
            throws IOException {

        lastActionByEmployee.clear();

        File csv = new File(CSV_FILE);


        if (!csv.exists()) {
            return;
        }


        try (BufferedReader reader =
                new BufferedReader(
                        new FileReader(csv)
                )) {

            String line;

            boolean firstLine = true;


            while ((line = reader.readLine()) != null) {

                if (firstLine) {

                    firstLine = false;

                    continue;
                }


                List<String> columns =
                        parseCsvLine(line);


                if (columns.size() >= 6) {

                    String id = columns.get(0);

                    String action = columns.get(3);


                    lastActionByEmployee.put(
                            id,
                            action
                    );
                }
            }
        }
    }


    // ===============================
    // CSV PARSER
    // ===============================

    private List<String> parseCsvLine(String line) {

        List<String> result =
                new ArrayList<>();

        StringBuilder value =
                new StringBuilder();

        boolean insideQuotes = false;


        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);


            if (c == '"') {

                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    value.append('"');

                    i++;

                } else {

                    insideQuotes = !insideQuotes;
                }

            } else if (c == ',' && !insideQuotes) {

                result.add(value.toString());

                value.setLength(0);

            } else {

                value.append(c);
            }
        }


        result.add(value.toString());

        return result;
    }


    // ===============================
    // CSV ESCAPE
    // ===============================

    private String csvEscape(String value) {

        if (value == null) {
            value = "";
        }


        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }


    // ===============================
    // START RECOGNITION
    // ===============================

    private void startRecognition() {

        try {

            statusLabel.setText(
                    "Status: Loading OpenCV..."
            );


            System.loadLibrary(
                    Core.NATIVE_LIBRARY_NAME
            );


            loadModels();

            loadEmployees();

            prepareAttendanceFiles();

            loadAttendanceState();


            statusLabel.setText(
                    "Status: Searching Camo Camera..."
            );


            // CAMERA INDEX 0 TO 5

            for (int i = 0; i < 6; i++) {

                VideoCapture test =
                        new VideoCapture(
                                i,
                                Videoio.CAP_DSHOW
                        );


                if (test.isOpened()) {

                    Mat testFrame = new Mat();

                    boolean received = false;


                    for (int j = 0; j < 10; j++) {

                        received = test.read(testFrame);


                        if (received && !testFrame.empty()) {
                            break;
                        }


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

                        System.out.println(
                                "Camera Found: " + i
                        );

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

                throw new RuntimeException(
                        "Camo Camera Not Found!"
                );
            }


            camera.set(
                    Videoio.CAP_PROP_FRAME_WIDTH,
                    CAMERA_WIDTH
            );

            camera.set(
                    Videoio.CAP_PROP_FRAME_HEIGHT,
                    CAMERA_HEIGHT
            );


            timer = new Timer(
                    50,
                    e -> processFrame()
            );

            timer.start();


            stopButton.setEnabled(true);


            statusLabel.setText(
                    "Status: Camera Running"
            );


        } catch (Exception ex) {

            statusLabel.setText(
                    "System Error: " + ex.getMessage()
            );


            JOptionPane.showMessageDialog(
                    this,
                    "AUREX Error:\n" + ex.getMessage()
            );


            ex.printStackTrace();
        }
    }


    // ===============================
    // PROCESS CAMERA FRAME
    // ===============================

    private void processFrame() {

        if (camera == null || !camera.isOpened()) {
            return;
        }


        Mat frame = new Mat();

        Mat faces = new Mat();


        try {

            if (!camera.read(frame) || frame.empty()) {
                return;
            }


            detector.setInputSize(
                    frame.size()
            );

            detector.detect(
                    frame,
                    faces
            );


            if (faces.rows() == 0) {

                currentEmployee = null;

                bestMatchScore = -1;

                clearEmployeeInformation();

                statusLabel.setText(
                        "Status: No Face Detected"
                );

            } else if (faces.rows() > 1) {

                currentEmployee = null;

                clearEmployeeInformation();

                statusLabel.setText(
                        "Multiple Faces Detected - Keep One Person"
                );


                for (int i = 0; i < faces.rows(); i++) {

                    double x = faces.get(i, 0)[0];

                    double y = faces.get(i, 1)[0];

                    double w = faces.get(i, 2)[0];

                    double h = faces.get(i, 3)[0];


                    Imgproc.rectangle(
                            frame,
                            new Point(x, y),
                            new Point(x + w, y + h),
                            new Scalar(0, 165, 255),
                            2
                    );
                }

            } else {

                recognizeSingleFace(frame, faces);
            }


            cameraLabel.setIcon(
                    matToIcon(frame)
            );

            cameraLabel.setText("");


        } catch (Exception ex) {

            currentEmployee = null;

            clearEmployeeInformation();

            statusLabel.setText(
                    "Recognition Error: " + ex.getMessage()
            );

            ex.printStackTrace();

        } finally {

            faces.release();

            frame.release();
        }
    }


    // ===============================
    // RECOGNIZE SINGLE FACE
    // ===============================

    private void recognizeSingleFace(
            Mat frame,
            Mat faces
    ) {

        double x = faces.get(0, 0)[0];

        double y = faces.get(0, 1)[0];

        double w = faces.get(0, 2)[0];

        double h = faces.get(0, 3)[0];


        Mat faceRow = faces.row(0);

        Mat alignedFace = new Mat();

        Mat feature = new Mat();


        try {

            recognizer.alignCrop(
                    frame,
                    faceRow,
                    alignedFace
            );


            recognizer.feature(
                    alignedFace,
                    feature
            );


            float[] liveFeature =
                    new float[(int) feature.total()];


            feature.get(
                    0,
                    0,
                    liveFeature
            );


            Employee matched =
                    findMatchingEmployee(liveFeature);


            if (matched != null) {

                currentEmployee = matched;


                Imgproc.rectangle(
                        frame,
                        new Point(x, y),
                        new Point(x + w, y + h),
                        new Scalar(0, 255, 0),
                        3
                );


                Imgproc.putText(
                        frame,
                        matched.name,
                        new Point(x, y - 10),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        new Scalar(0, 255, 0),
                        2
                );


                showEmployeeInformation(matched);


                statusLabel.setText(
                        "IDENTIFIED: "
                        + matched.name
                        + " | ID: "
                        + matched.id
                );


            } else {

                currentEmployee = null;


                Imgproc.rectangle(
                        frame,
                        new Point(x, y),
                        new Point(x + w, y + h),
                        new Scalar(0, 0, 255),
                        3
                );


                Imgproc.putText(
                        frame,
                        "UNKNOWN PERSON",
                        new Point(x, y - 10),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        new Scalar(0, 0, 255),
                        2
                );


                clearEmployeeInformation();


                statusLabel.setText(
                        "UNKNOWN PERSON - NOT REGISTERED"
                );
            }


        } finally {

            faceRow.release();

            alignedFace.release();

            feature.release();
        }
    }


    // ===============================
    // FIND MATCHING EMPLOYEE
    // ===============================

    private Employee findMatchingEmployee(
            float[] liveFeature
    ) {

        Employee bestMatch = null;

        double bestScore = -1;


        for (Employee employee : employees) {

            double score = cosineSimilarity(
                    liveFeature,
                    employee.feature
            );


            if (score > bestScore) {

                bestScore = score;

                bestMatch = employee;
            }


            System.out.println(
                    "MATCH TEST: "
                    + employee.id
                    + " | "
                    + employee.name
                    + " | SCORE: "
                    + score
            );
        }


        bestMatchScore = bestScore;


        if (bestScore >= MATCH_THRESHOLD) {

            return bestMatch;
        }


        return null;
    }


    // ===============================
    // COSINE SIMILARITY
    // ===============================

    private double cosineSimilarity(
            float[] first,
            float[] second
    ) {

        if (first == null
                || second == null
                || first.length != second.length
                || first.length == 0) {

            return -1;
        }


        double dot = 0;

        double normA = 0;

        double normB = 0;


        for (int i = 0; i < first.length; i++) {

            dot += first[i] * second[i];

            normA += first[i] * first[i];

            normB += second[i] * second[i];
        }


        if (normA == 0 || normB == 0) {
            return -1;
        }


        return dot / (
                Math.sqrt(normA) * Math.sqrt(normB)
        );
    }


    // ===============================
    // SHOW EMPLOYEE INFORMATION
    // ===============================

    private void showEmployeeInformation(
            Employee employee
    ) {

        employeeNameLabel.setText(
                "Name: " + employee.name
        );

        employeeIdLabel.setText(
                "Employee ID: " + employee.id
        );

        departmentLabel.setText(
                "Department: " + employee.department
        );


        String lastAction =
                lastActionByEmployee.get(employee.id);


        if ("CHECK_IN".equals(lastAction)) {

            attendanceLabel.setText(
                    "Attendance: INSIDE"
            );

            checkInButton.setEnabled(false);

            checkOutButton.setEnabled(true);

        } else {

            attendanceLabel.setText(
                    "Attendance: OUTSIDE"
            );

            checkInButton.setEnabled(true);

            checkOutButton.setEnabled(false);
        }
    }


    // ===============================
    // CLEAR EMPLOYEE INFORMATION
    // ===============================

    private void clearEmployeeInformation() {

        employeeNameLabel.setText(
                "Name: Waiting..."
        );

        employeeIdLabel.setText(
                "Employee ID: ---"
        );

        departmentLabel.setText(
                "Department: ---"
        );

        attendanceLabel.setText(
                "Attendance: ---"
        );


        checkInButton.setEnabled(false);

        checkOutButton.setEnabled(false);
    }


    // ===============================
    // VALIDATE GUARD
    // ===============================

    private boolean validateGuard() {

        String guard =
                guardField.getText().trim();


        if (guard.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter Guard Name or ID!"
            );

            guardField.requestFocus();

            return false;
        }


        return true;
    }


    // ===============================
    // CHECK IN EMPLOYEE
    // ===============================

    private void checkInEmployee() {

        if (currentEmployee == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "No Employee Identified!"
            );

            return;
        }


        if (!validateGuard()) {
            return;
        }


        Employee employee =
                currentEmployee;


        String lastAction =
                lastActionByEmployee.get(employee.id);


        if ("CHECK_IN".equals(lastAction)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Employee is already inside!"
            );

            return;
        }


        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Confirm Employee Check-In?\n\n"
                + "Name: " + employee.name + "\n"
                + "ID: " + employee.id + "\n"
                + "Department: " + employee.department + "\n"
                + "Guard: " + guardField.getText().trim(),
                "AUREX - Confirm Check-In",
                JOptionPane.YES_NO_OPTION
        );


        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }


        if (currentEmployee != employee) {

            JOptionPane.showMessageDialog(
                    this,
                    "Employee is no longer identified. Try again."
            );

            return;
        }


        recordAttendance(
                employee,
                "CHECK_IN"
        );
    }


    // ===============================
    // CHECK OUT EMPLOYEE
    // ===============================

    private void checkOutEmployee() {

        if (currentEmployee == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "No Employee Identified!"
            );

            return;
        }


        if (!validateGuard()) {
            return;
        }


        Employee employee =
                currentEmployee;


        String lastAction =
                lastActionByEmployee.get(employee.id);


        if (!"CHECK_IN".equals(lastAction)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Employee is not currently inside!"
            );

            return;
        }


        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Confirm Employee Check-Out?\n\n"
                + "Name: " + employee.name + "\n"
                + "ID: " + employee.id + "\n"
                + "Department: " + employee.department + "\n"
                + "Guard: " + guardField.getText().trim(),
                "AUREX - Confirm Check-Out",
                JOptionPane.YES_NO_OPTION
        );


        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }


        if (currentEmployee != employee) {

            JOptionPane.showMessageDialog(
                    this,
                    "Employee is no longer identified. Try again."
            );

            return;
        }


        recordAttendance(
                employee,
                "CHECK_OUT"
        );
    }


    // ===============================
    // SAVE ATTENDANCE CSV AND TXT
    // ===============================

 // ===============================
    // SAVE ATTENDANCE CSV AND TXT
    // ===============================

    private void recordAttendance(
            Employee employee,
            String action
    ) {

        try {

            String guard =
                    guardField.getText().trim();


            String timestamp =
                    LocalDateTime.now().format(
                            timeFormatter
                    );


            String csvLine =
                    csvEscape(employee.id) + ","
                    + csvEscape(employee.name) + ","
                    + csvEscape(employee.department) + ","
                    + csvEscape(action) + ","
                    + csvEscape(timestamp) + ","
                    + csvEscape(guard);


            // SAVE CSV

            try (BufferedWriter writer =
                    Files.newBufferedWriter(
                            new File(CSV_FILE).toPath(),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND
                    )) {

                writer.write(csvLine);

                writer.newLine();
            }


            // SAVE TXT

            String txtLine =
                    "Employee ID: " + employee.id
                    + " | Name: " + employee.name
                    + " | Department: " + employee.department
                    + " | Action: " + action
                    + " | Date & Time: " + timestamp
                    + " | Guard: " + guard
                    + System.lineSeparator();


            Files.write(
                    new File(TXT_FILE).toPath(),
                    txtLine.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );


            // =========================================================
            // 🎯 NEW: MAIN DASHBOARD DATA SYNC (নতুন যোগ করা হয়েছে)
            // =========================================================
            updateMainEmployeePresence(employee.id, "CHECK_IN".equals(action) ? "Inside" : "Outside");
            writeAuditLog("CHECK_IN".equals(action) ? "EMPLOYEE CHECK IN" : "EMPLOYEE CHECK OUT", employee.id);
            // =========================================================


            // UPDATE CURRENT STATE

            lastActionByEmployee.put(
                    employee.id,
                    action
            );


            showEmployeeInformation(employee);


            if ("CHECK_IN".equals(action)) {

                statusLabel.setText(
                        "CHECK-IN SUCCESSFUL: "
                        + employee.name
                );

            } else {

                statusLabel.setText(
                        "CHECK-OUT SUCCESSFUL: "
                        + employee.name
                );
            }


            JOptionPane.showMessageDialog(
                    this,
                    "ATTENDANCE RECORDED SUCCESSFULLY!\n\n"
                    + "Employee: " + employee.name + "\n"
                    + "Employee ID: " + employee.id + "\n"
                    + "Action: " + action + "\n"
                    + "Date & Time: " + timestamp + "\n"
                    + "Guard: " + guard
            );


            System.out.println(
                    "AUREX Attendance: "
                    + employee.id
                    + " - "
                    + action
            );


        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Attendance Save Error:\n"
                    + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }
 // ===============================
    // DASHBOARD DATA SYNC HELPERS
    // ===============================

 // ===============================
    // FIXED DASHBOARD PRESENCE SYNC
    // ===============================

    private void updateMainEmployeePresence(String empId, String newPresence) {
        File empFile = new File("AurexSecurityData/employees.txt");
        if (!empFile.exists()) return;

        try {
            List<String> lines = Files.readAllLines(empFile.toPath(), StandardCharsets.UTF_8);
            List<String> updatedLines = new ArrayList<>();
            String targetId = empId.trim();

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\t", -1);
                
                if (parts.length > 0) {
                    // Raw string এবং Base64 decoded string দুটো দিয়েই মিলিয়ে দেখা
                    String rawId = parts[0].trim();
                    String decodedId = rawId;
                    try {
                        decodedId = new String(java.util.Base64.getDecoder().decode(rawId), StandardCharsets.UTF_8).trim();
                    } catch (Exception ignored) {}

                    // ID মিলে গেলে Presence (Index 6) আপডেট হবে
                    if (rawId.equalsIgnoreCase(targetId) || decodedId.equalsIgnoreCase(targetId)) {
                        if (parts.length >= 7) {
                            parts[6] = java.util.Base64.getEncoder().encodeToString(newPresence.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
                updatedLines.add(String.join("\t", parts));
            }
            Files.write(empFile.toPath(), updatedLines, StandardCharsets.UTF_8);
            System.out.println("Main Employee presence updated to: " + newPresence + " for ID: " + targetId);
        } catch (Exception e) {
            System.err.println("Main Employee status sync failed: " + e.getMessage());
        }
    }

    private void writeAuditLog(String action, String details) {
        File logFile = new File("AurexSecurityData/audit_logs.txt");
        try {
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String guard = guardField.getText().trim();
            
            String logLine = String.join("\t", 
                java.util.Base64.getEncoder().encodeToString(now.getBytes(StandardCharsets.UTF_8)),
                java.util.Base64.getEncoder().encodeToString(guard.getBytes(StandardCharsets.UTF_8)),
                java.util.Base64.getEncoder().encodeToString(action.getBytes(StandardCharsets.UTF_8)),
                java.util.Base64.getEncoder().encodeToString(details.getBytes(StandardCharsets.UTF_8))
            );

            Files.write(logFile.toPath(), java.util.Arrays.asList(logLine), StandardCharsets.UTF_8, 
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("Audit log update failed: " + e.getMessage());
        }
    }

    // ===============================
    // CONVERT MAT TO IMAGE
    // ===============================

    private ImageIcon matToIcon(Mat mat) {

        MatOfByte buffer = new MatOfByte();


        try {

            Imgcodecs.imencode(
                    ".jpg",
                    mat,
                    buffer
            );


            BufferedImage image =
                    ImageIO.read(
                            new ByteArrayInputStream(
                                    buffer.toArray()
                            )
                    );


            if (image != null) {

                Image scaled =
                        image.getScaledInstance(
                                750,
                                580,
                                Image.SCALE_SMOOTH
                        );


                return new ImageIcon(scaled);
            }


        } catch (Exception ex) {

            ex.printStackTrace();

        } finally {

            buffer.release();
        }


        return null;
    }


    // ===============================
    // STOP CAMERA
    // ===============================

    private void stopCamera() {

        if (timer != null) {

            timer.stop();

            timer = null;
        }


        if (camera != null) {

            camera.release();

            camera = null;
        }


        currentEmployee = null;


        if (cameraLabel != null) {

            cameraLabel.setIcon(null);

            cameraLabel.setText(
                    "Camera Stopped"
            );
        }


        if (stopButton != null) {
            stopButton.setEnabled(false);
        }


        if (checkInButton != null) {
            checkInButton.setEnabled(false);
        }


        if (checkOutButton != null) {
            checkOutButton.setEnabled(false);
        }


        if (statusLabel != null) {

            statusLabel.setText(
                    "Status: Camera Stopped"
            );
        }


        if (employeeNameLabel != null) {
            employeeNameLabel.setText("Name: ---");
        }

        if (employeeIdLabel != null) {
            employeeIdLabel.setText("Employee ID: ---");
        }

        if (departmentLabel != null) {
            departmentLabel.setText("Department: ---");
        }

        if (attendanceLabel != null) {
            attendanceLabel.setText("Attendance: ---");
        }
    }


    // ===============================
    // MAIN METHOD
    // ===============================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> new FaceRecognition()
        );
    }


    // ===============================
    // CLOSE WINDOW
    // ===============================

    @Override
    public void dispose() {

        stopCamera();

        super.dispose();
    }
}