package Aurex;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*; //imp
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class AurexSecuritySystem extends JFrame {

    // ================= COLORS =================
    private static final Color BG = new Color(7, 13, 27);
    private static final Color PANEL = new Color(15, 27, 48);
    private static final Color PANEL2 = new Color(23, 39, 65);
    private static final Color BLUE = new Color(0, 190, 255);
    private static final Color GREEN = new Color(0, 235, 145);
    private static final Color TEXT = new Color(235, 244, 255);
    private static final Color MUTED = new Color(145, 164, 194);
    private static final Color RED = new Color(255, 85, 105);
    private static final Color GOLD = new Color(255, 190, 75);

    // Every module has its own colour
    private static final Map<String, Color> MODULE_COLORS = new HashMap<>();

    static {
        MODULE_COLORS.put("Dashboard", new Color(0, 190, 255));
        MODULE_COLORS.put("Employees", new Color(70, 150, 255));
        MODULE_COLORS.put("Visitors", new Color(0, 235, 145));
        MODULE_COLORS.put("Authorization", new Color(255, 190, 75));
        MODULE_COLORS.put("Face Recognition", new Color(0, 220, 255));
        MODULE_COLORS.put("Departments", new Color(175, 125, 255));
        MODULE_COLORS.put("Blacklist", new Color(255, 85, 105));
        MODULE_COLORS.put("Appointments", new Color(255, 140, 60));
        MODULE_COLORS.put("Vehicles", new Color(0, 205, 215));
        MODULE_COLORS.put("Materials", new Color(200, 225, 70));
        MODULE_COLORS.put("Restricted Access", new Color(255, 100, 175));
        MODULE_COLORS.put("Incidents", new Color(255, 70, 70));
        MODULE_COLORS.put("Lost & Found", new Color(130, 220, 130));
        MODULE_COLORS.put("Duty Roster", new Color(150, 150, 255));
        MODULE_COLORS.put("Reports", new Color(110, 235, 255));
        MODULE_COLORS.put("Audit Logs", new Color(165, 175, 195));
        MODULE_COLORS.put("Emergency Alerts", new Color(255, 75, 90));
        MODULE_COLORS.put("Analytics", new Color(0, 220, 190));
        MODULE_COLORS.put("Notifications", new Color(255, 190, 75));
        MODULE_COLORS.put("Attendance Analytics", new Color(0, 220, 190));
    }

    private static Color colorOf(String key) {
        return MODULE_COLORS.getOrDefault(key, BLUE);
    }

    // ================= FONTS (different sizes & styles) =================
    private static Font head(int size) {
        return new Font("Bahnschrift", Font.BOLD, size);
    }

    private static Font body(int size) {
        return new Font("Segoe UI", Font.PLAIN, size);
    }

    private static Font bodyBold(int size) {
        return new Font("Segoe UI", Font.BOLD, size);
    }

    private static Font italic(int size) {
        return new Font("Segoe UI", Font.ITALIC, size);
    }

    private static Font mono(int size) {
        return new Font("Consolas", Font.BOLD, size);
    }

    private static final String DIR = "AurexSecurityData";

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel clock = new JLabel();

    private final String role;
    private final String currentUser;

    private final Map<String, List<String[]>> data = new LinkedHashMap<>();
    private final Map<String, String[]> schemas = new LinkedHashMap<>();
    private final Map<String, List<JTable>> tables = new HashMap<>();
    private final Map<String, NavButton> navButtons = new HashMap<>();

    private JPanel reportsPanel;
    private JPanel analyticsPanel;
    private JPanel notificationsPanel;
    private JPanel movementPanel;

    private JLabel empCount;
    private JLabel visitorCount;
    private JLabel insideCount;
    private JLabel pendingCount;
    private JLabel notificationBadge;

    public AurexSecuritySystem(String userName, String role) {

        super("AUREX | Smart Security & Visitor Tracking System");

        this.currentUser = userName;
        this.role = role;

        initSchemas();
        loadAll();
        buildUI();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1450, 880);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        setIconImage(createAurexIcon());

        log("LOGIN", "Role: " + role);

        new javax.swing.Timer(1000, e ->
                clock.setText(LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  hh:mm:ss a")))
        ).start();
    }

    // ================= USERS / LOGIN DATA =================

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(s.getBytes(StandardCharsets.UTF_8))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String unb64(String s) {
        try {
            return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    // userId (lower case) -> {passwordHash, role, displayName}
    private static Map<String, String[]> loadUsers() {

        Map<String, String[]> users = new LinkedHashMap<>();
        Path p = Paths.get(DIR, "users.txt");

        try {
            Files.createDirectories(Paths.get(DIR));

            if (!Files.exists(p)) {
                List<String> seed = new ArrayList<>();
                String[][] def = {
                        {"admin", "admin123", "Admin", "Administrator"},
                        {"guard", "guard123", "Guard", "Security Guard"},
                        {"hr", "hr123", "Department", "HR Officer"}
                };
                for (String[] d : def) {
                    seed.add(String.join("\t",
                            b64(d[0]), b64(sha256(d[1])), b64(d[2]), b64(d[3])));
                }
                Files.write(p, seed, StandardCharsets.UTF_8);
            }

            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] a = line.split("\\t", -1);
                if (a.length >= 4) {
                    users.put(unb64(a[0]).toLowerCase(Locale.ROOT),
                            new String[]{unb64(a[1]), unb64(a[2]), unb64(a[3])});
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "User file error: " + ex.getMessage());
        }

        return users;
    }

    private static JTextField styledField(JTextField t) {

        t.setBackground(PANEL2);
        t.setForeground(TEXT);
        t.setCaretColor(GREEN);
        t.setFont(body(14));

        t.setBorder(new CompoundBorder(
                new LineBorder(new Color(48, 75, 110)),
                new EmptyBorder(8, 10, 8, 10)));

        return t;
    }

    // ================= LOGIN WINDOW =================

    private static class LoginFrame extends JFrame {

        private final JTextField idField = styledField(new JTextField());
        private final JPasswordField passField = new JPasswordField();
        private final JLabel msg = new JLabel(" ");
        private final NeonButton loginBtn = new NeonButton("LOGIN  →", GREEN);
        private final Map<String, String[]> users = loadUsers();
        private int attempts = 0;

        LoginFrame() {

            super("AUREX | Secure Login");

            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(920, 560);
            setResizable(false);
            setLocationRelativeTo(null);
            setIconImage(createAurexIcon());

            styledField(passField);
            passField.setEchoChar('•');

            JPanel root = new JPanel(new GridLayout(1, 2));

            // ---------- Left brand side ----------
            JPanel left = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(new GradientPaint(0, 0, new Color(5, 10, 22),
                            getWidth(), getHeight(), new Color(8, 60, 90)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };

            JPanel brand = new JPanel();
            brand.setOpaque(false);
            brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

            AurexLogo logo = new AurexLogo(true);
            logo.setPreferredSize(new Dimension(300, 100));
            logo.setMaximumSize(new Dimension(300, 100));
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel t1 = new JLabel("SMART SECURITY");
            t1.setFont(head(20));
            t1.setForeground(TEXT);
            t1.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel t2 = new JLabel("& VISITOR TRACKING SYSTEM");
            t2.setFont(bodyBold(13));
            t2.setForeground(GREEN);
            t2.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel t3 = new JLabel("Protect  •  Monitor  •  Control");
            t3.setFont(italic(12));
            t3.setForeground(MUTED);
            t3.setAlignmentX(Component.CENTER_ALIGNMENT);

            brand.add(logo);
            brand.add(Box.createVerticalStrut(18));
            brand.add(t1);
            brand.add(Box.createVerticalStrut(4));
            brand.add(t2);
            brand.add(Box.createVerticalStrut(22));
            brand.add(t3);

            left.add(brand);

            // ---------- Right form side ----------
            JPanel right = new JPanel();
            right.setBackground(PANEL);
            right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
            right.setBorder(new EmptyBorder(50, 55, 40, 55));

            JLabel welcome = new JLabel("Welcome Back");
            welcome.setFont(head(32));
            welcome.setForeground(BLUE);

            JLabel sub = new JLabel("Police Divisional Headquarters Security Control Center");
            sub.setFont(italic(13));
            sub.setForeground(MUTED);

            JLabel idLabel = new JLabel("USER ID");
            idLabel.setFont(bodyBold(11));
            idLabel.setForeground(GREEN);

            JLabel pwLabel = new JLabel("PASSWORD");
            pwLabel.setFont(bodyBold(11));
            pwLabel.setForeground(GREEN);

            JCheckBox show = new JCheckBox("Show password");
            show.setOpaque(false);
            show.setForeground(MUTED);
            show.setFont(body(12));
            show.setFocusPainted(false);
            show.addActionListener(e ->
                    passField.setEchoChar(show.isSelected() ? (char) 0 : '•'));

            msg.setFont(bodyBold(12));
            msg.setForeground(RED);

            loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            loginBtn.addActionListener(e -> attempt());

           
           

            for (JComponent c : new JComponent[]{welcome, sub, idLabel, idField,
                    pwLabel, passField, show, msg}) {  
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
            }

            idField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            passField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

            right.add(welcome);
            right.add(Box.createVerticalStrut(4));
            right.add(sub);
            right.add(Box.createVerticalStrut(34));
            right.add(idLabel);
            right.add(Box.createVerticalStrut(6));
            right.add(idField);
            right.add(Box.createVerticalStrut(18));
            right.add(pwLabel);
            right.add(Box.createVerticalStrut(6));
            right.add(passField);
            right.add(Box.createVerticalStrut(8));
            right.add(show);
            right.add(Box.createVerticalStrut(10));
            right.add(msg);
            right.add(Box.createVerticalStrut(12));
            right.add(loginBtn);
            right.add(Box.createVerticalGlue());
           

            root.add(left);
            root.add(right);

            setContentPane(root);

            getRootPane().setDefaultButton(loginBtn); // Enter key = login
        }

        private void attempt() {

            String id = idField.getText().trim().toLowerCase(Locale.ROOT);
            String pw = new String(passField.getPassword());

            if (id.isEmpty() || pw.isEmpty()) {
                msg.setForeground(GOLD);
                msg.setText("Please enter both User ID and Password.");
                return;
            }

            String[] u = users.get(id);

            if (u != null && u[0].equals(sha256(pw))) {
                dispose();
                new AurexSecuritySystem(u[2], u[1]).setVisible(true);
                return;
            }

            attempts++;
            msg.setForeground(RED);
            passField.setText("");

            if (attempts >= 3) {
                lock();
            } else {
                msg.setText("Invalid User ID or Password. Attempts left: " + (3 - attempts));
            }
        }

        private void lock() {

            loginBtn.setEnabled(false);
            final int[] sec = {10};

            javax.swing.Timer t = new javax.swing.Timer(1000, null);

            t.addActionListener(e -> {
                sec[0]--;
                if (sec[0] <= 0) {
                    t.stop();
                    attempts = 0;
                    loginBtn.setEnabled(true);
                    msg.setText(" ");
                } else {
                    msg.setText("Too many attempts. Try again in " + sec[0] + "s");
                }
            });

            msg.setText("Too many attempts. Try again in 10s");
            t.start();
        }
    }

    // ================= AUREX LOGO =================

    private static class AurexLogo extends JPanel {

        private final boolean showText;

        AurexLogo(boolean showText) {
            this.showText = showText;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int h = getHeight();
            int iconSize = Math.min(h - 8, 90);

            Graphics2D logo = (Graphics2D) g2.create();

            logo.translate(5, 3);
            logo.scale(iconSize / 100.0, iconSize / 100.0);

            Path2D arrow = new Path2D.Double();

            arrow.moveTo(12, 83);
            arrow.lineTo(30, 58);
            arrow.lineTo(42, 64);
            arrow.lineTo(83, 17);
            arrow.lineTo(83, 35);
            arrow.lineTo(95, 8);
            arrow.lineTo(65, 18);
            arrow.lineTo(79, 25);
            arrow.lineTo(40, 69);
            arrow.lineTo(27, 64);
            arrow.lineTo(4, 91);
            arrow.closePath();

            logo.setColor(new Color(0, 180, 255, 35));
            logo.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            logo.draw(arrow);

            logo.setPaint(new GradientPaint(0, 10, BLUE, 80, 90, GREEN));
            logo.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            logo.draw(arrow);

            Path2D flame = new Path2D.Double();

            flame.moveTo(18, 66);
            flame.curveTo(5, 51, 15, 35, 27, 25);
            flame.curveTo(24, 39, 36, 39, 37, 24);
            flame.curveTo(38, 12, 50, 4, 57, 1);
            flame.curveTo(52, 20, 61, 25, 66, 37);
            flame.curveTo(70, 29, 72, 24, 72, 18);
            flame.curveTo(88, 35, 83, 54, 69, 65);

            logo.setColor(new Color(0, 235, 145, 35));
            logo.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            logo.draw(flame);

            logo.setColor(GREEN);
            logo.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            logo.draw(flame);

            Path2D inner = new Path2D.Double();

            inner.moveTo(31, 58);
            inner.curveTo(39, 48, 43, 38, 46, 29);
            inner.curveTo(51, 42, 58, 48, 59, 58);

            logo.setColor(BLUE);
            logo.setStroke(new BasicStroke(2.5f));
            logo.draw(inner);

            logo.dispose();

            if (showText) {

                int tx = iconSize + 18;

                g2.setFont(head(Math.max(22, h / 2)));

                FontMetrics fm = g2.getFontMetrics();
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();

                String[] letters = {"A", "U", "R", "E", "X"};
                int x = tx;

                for (int i = 0; i < letters.length; i++) {
                    g2.setColor(i < 3 ? BLUE : GREEN);
                    g2.drawString(letters[i], x, y);
                    x += fm.stringWidth(letters[i]) + 1;
                }
            }

            g2.dispose();
        }
    }

    private static Image createAurexIcon() {

        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(BG);
        g.fillRoundRect(0, 0, 128, 128, 25, 25);

        g.translate(10, 10);
        g.scale(1.08, 1.08);

        Path2D arrow = new Path2D.Double();

        arrow.moveTo(8, 91);
        arrow.lineTo(29, 63);
        arrow.lineTo(43, 68);
        arrow.lineTo(85, 20);
        arrow.lineTo(85, 38);
        arrow.lineTo(99, 8);
        arrow.lineTo(68, 19);
        arrow.lineTo(81, 27);
        arrow.lineTo(42, 73);
        arrow.lineTo(26, 68);
        arrow.lineTo(3, 96);

        g.setColor(BLUE);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(arrow);

        Path2D flame = new Path2D.Double();

        flame.moveTo(18, 66);
        flame.curveTo(5, 50, 16, 35, 28, 25);
        flame.curveTo(24, 40, 38, 40, 39, 23);
        flame.curveTo(40, 12, 51, 4, 57, 2);
        flame.curveTo(53, 20, 63, 29, 68, 42);
        flame.curveTo(73, 31, 75, 25, 73, 18);
        flame.curveTo(89, 35, 84, 53, 69, 65);

        g.setColor(GREEN);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(flame);

        g.dispose();

        return image;
    }

    // ================= DATA SCHEMAS =================

    private void initSchemas() {

        schemas.put("Employees", new String[]{
                "Employee ID", "Name", "Department", "Designation",
                "Phone", "Status", "Presence"});

        schemas.put("Visitors", new String[]{
                "Visitor ID", "Name", "Phone", "NID/Passport", "Purpose",
                "Department", "Host", "Visit Date", "Expected Exit",
                "Status", "Check In", "Check Out"});

        schemas.put("Departments", new String[]{
                "Department", "Authorized Person", "Designation", "Phone", "Email"});

        schemas.put("Blacklist", new String[]{
                "Person Name", "NID/Passport", "Reason", "Added By", "Date"});

        schemas.put("Incidents", new String[]{
                "Incident ID", "Type", "Location", "Description",
                "Reported By", "Date", "Status"});

        schemas.put("Appointments", new String[]{
                "Appointment ID", "Visitor Name", "Phone", "Department",
                "Host", "Date & Time", "Purpose", "Status"});

        schemas.put("Vehicles", new String[]{
                "Vehicle No", "Driver", "Phone", "Purpose",
                "Entry Time", "Exit Time", "Status"});

        schemas.put("Materials", new String[]{
                "Record ID", "Item", "Quantity", "From/To",
                "Responsible Person", "Entry/Exit", "Date", "Status"});

        schemas.put("Restricted Access", new String[]{
                "Person ID", "Person Name", "Area", "Access Level",
                "Valid Until", "Status"});

        schemas.put("Lost & Found", new String[]{
                "Item ID", "Item Name", "Description", "Location",
                "Reported By", "Date", "Status"});

        schemas.put("Duty Roster", new String[]{
                "Guard ID", "Guard Name", "Gate", "Shift", "Date", "Status"});

        schemas.put("Audit Logs", new String[]{
                "Time", "User", "Action", "Details"});

        schemas.put("Emergency Alerts", new String[]{
                "Alert ID", "Emergency Type", "Location", "Priority",
                "Description", "Reported By", "Date & Time", "Status"});
    }

    // ================= FILE STORAGE =================

    private Path file(String key) {
        return Paths.get(DIR,
                key.toLowerCase(Locale.ROOT).replace(" & ", "_").replace(" ", "_") + ".txt");
    }

    private void loadAll() {

        try {
            Files.createDirectories(Paths.get(DIR));
        } catch (IOException ex) {
            showError("Cannot create data folder: " + ex.getMessage());
        }

        for (String key : schemas.keySet()) {

            List<String[]> rows = new ArrayList<>();
            Path p = file(key);

            if (Files.exists(p)) {

                try {

                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {

                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        String[] parts = line.split("\\t", -1);
                        String[] row = new String[schemas.get(key).length];

                        for (int i = 0; i < row.length && i < parts.length; i++) {
                            row[i] = unb64(parts[i]);
                        }

                        for (int i = 0; i < row.length; i++) {
                            if (row[i] == null) {
                                row[i] = "";
                            }
                        }

                        rows.add(row);
                    }

                } catch (IOException ex) {
                    showError("Could not read " + p + ": " + ex.getMessage());
                }
            }

            data.put(key, rows);
        }

        seedDepartments();
        syncAttendanceFiles();
    }

    private void save(String key) {

        try {

            Files.createDirectories(Paths.get(DIR));

            List<String> lines = new ArrayList<>();

            for (String[] row : data.get(key)) {

                List<String> enc = new ArrayList<>();

                for (String v : row) {
                    enc.add(b64(v));
                }

                lines.add(String.join("\t", enc));
            }

            Files.write(file(key), lines, StandardCharsets.UTF_8);

        } catch (IOException ex) {
            showError("Save failed: " + ex.getMessage());
        }
    }

    // ================= POLICE DIVISIONAL HQ STAFF DIRECTORY =================
    // The names below are illustrative sample contacts, not an official
    // directory and do not represent actual police personnel.
    private void seedDepartments() {

        String[][] defaults = {
                {"Office of the Police Commissioner", "Md. Mahbubur Rahman", "Police Commissioner", "N/A", "commissioner@example.invalid"},
                {"Administration & Human Resources", "Md. Saiful Islam", "Additional Commissioner (Administration)", "N/A", "admin@example.invalid"},
                {"Crime Division", "Md. Kamal Hossain", "Deputy Commissioner (Crime)", "N/A", "crime@example.invalid"},
                {"Detective Branch", "Md. Rashedul Karim", "Deputy Commissioner (DB)", "N/A", "detective@example.invalid"},
                {"Special Branch & Intelligence", "Md. Anisur Rahman", "Deputy Commissioner (Intelligence)", "N/A", "intelligence@example.invalid"},
                {"Traffic Division", "Md. Shahidul Islam", "Deputy Commissioner (Traffic)", "N/A", "traffic@example.invalid"},
                {"Cyber Crime & Digital Forensics", "Md. Tanvir Ahmed", "Unit Head (Cyber Crime)", "N/A", "cyber@example.invalid"},
                {"Counter Terrorism & Organized Crime", "Md. Mizanur Rahman", "Unit Head (CT)", "N/A", "ct@example.invalid"},
                {"Public Order & Riot Control", "Md. Ashraful Alam", "Unit Commander", "N/A", "operations@example.invalid"},
                {"Women, Children & Victim Support", "Farzana Akter", "Unit Head (Victim Support)", "N/A", "support@example.invalid"},
                {"Legal Affairs & Prosecution Coordination", "Md. Rezaul Karim", "Legal Affairs Officer", "N/A", "legal@example.invalid"},
                {"Training & Professional Development", "Md. Shafiqul Islam", "Training Superintendent", "N/A", "training@example.invalid"},
                {"Police Lines, Logistics & Transport", "Md. Nurul Huda", "Logistics Officer", "N/A", "logistics@example.invalid"},
                {"IT & Communications", "Md. Rakib Hasan", "IT Superintendent", "N/A", "it@example.invalid"},
                {"Finance & Accounts", "Md. Abdul Mannan", "Accounts Superintendent", "N/A", "accounts@example.invalid"},
                {"Procurement & Stores", "Md. Habibur Rahman", "Procurement Officer", "N/A", "procurement@example.invalid"},
                {"Internal Affairs & Discipline", "Md. Monir Hossain", "Internal Affairs Officer", "N/A", "internal@example.invalid"},
                {"Community Policing & Public Relations", "Nusrat Jahan", "Public Relations Officer", "N/A", "community@example.invalid"},
                {"Emergency Response & Control Room", "Md. Jahangir Alam", "Control Room Supervisor", "N/A", "controlroom@example.invalid"},
                {"Medical & Welfare Unit", "Dr. Sadia Sultana", "Welfare Officer", "N/A", "welfare@example.invalid"}
        };

        boolean departmentsChanged = false;
        for (String[] department : defaults) {
            String[] savedDepartment = find("Departments", 0, department[0]);
            if (savedDepartment == null) {
                data.get("Departments").add(department);
                departmentsChanged = true;
            } else if (savedDepartment.length > 1 && savedDepartment[1].startsWith("Demo ")) {
                savedDepartment[1] = department[1];
                departmentsChanged = true;
            }
        }
        if (departmentsChanged) save("Departments");

        // Remove old development labels from previously saved audit entries.
        boolean logsChanged = false;
        for (String[] entry : data.get("Audit Logs")) {
            for (int i = 0; i < entry.length; i++) {
                if (entry[i] != null && entry[i].toUpperCase(Locale.ROOT).contains("DEMO")) {
                    entry[i] = entry[i].replace("DEMO", "SYSTEM").replace("Demo", "System").replace("demo", "system");
                    logsChanged = true;
                }
            }
        }
        if (logsChanged) save("Audit Logs");

        // Clean up records created by earlier development versions.
        // Existing employee details are preserved except for the old sample labels.
        boolean employeeRecordsChanged = false;
        for (String[] employee : data.get("Employees")) {
            if (employee.length > 1 && employee[1].startsWith("Demo ")) {
                employee[1] = employee[1].substring(5).replaceFirst("\\s+\\d{3}$", "").trim();
                employeeRecordsChanged = true;
            }
            if (employee.length > 0 && employee[0].startsWith("DPH-DEMO-")) {
                employee[0] = employee[0].replace("DPH-DEMO-", "DPH-EMP-");
                employeeRecordsChanged = true;
            }
            if (employee.length > 4 && "N/A - DEMO".equals(employee[4])) {
                employee[4] = "Not Provided";
                employeeRecordsChanged = true;
            }
        }
        if (employeeRecordsChanged) save("Employees");

        // Create the initial 300 sample staff records only when the directory is empty.
        // Existing staff records are never overwritten or duplicated.
        if (!data.get("Employees").isEmpty()) return;

        String[] firstNames = {
                "Arif", "Sajid", "Nabil", "Tanvir", "Rafi", "Fahim", "Imran", "Hasan", "Mahmud", "Sabbir",
                "Nusrat", "Maliha", "Samira", "Farzana", "Tania", "Sumaiya", "Ayesha", "Nabila", "Sharmin", "Jannat",
                "Rashed", "Shakil", "Kamrul", "Jubayer", "Mushfiq", "Saiful", "Anika", "Mim", "Sadia", "Nafisa"
        };
        String[] lastNames = {
                "Rahman", "Ahmed", "Hossain", "Karim", "Islam", "Akter", "Chowdhury", "Hasan", "Uddin", "Khan",
                "Sarker", "Miah", "Kabir", "Alam", "Begum", "Sultana", "Mahmud", "Rashid", "Haque", "Das"
        };

        String[][] ranksByDepartment = {
                {"Police Commissioner", "Additional Police Commissioner", "Joint Police Commissioner", "Deputy Police Commissioner", "Assistant Police Commissioner"},
                {"Additional Police Commissioner", "Deputy Commissioner (Administration)", "Assistant Commissioner", "Administrative Officer", "Office Assistant"},
                {"Deputy Commissioner (Crime)", "Additional Deputy Commissioner", "Assistant Commissioner", "Inspector", "Sub-Inspector", "Assistant Sub-Inspector", "Constable"},
                {"Deputy Commissioner (Detective Branch)", "Additional Deputy Commissioner", "Assistant Commissioner", "Inspector", "Sub-Inspector", "Assistant Sub-Inspector", "Constable"},
                {"Deputy Commissioner (Intelligence)", "Additional Deputy Commissioner", "Assistant Commissioner", "Intelligence Officer", "Inspector", "Sub-Inspector", "Analyst"},
                {"Deputy Commissioner (Traffic)", "Additional Deputy Commissioner", "Assistant Commissioner", "Traffic Inspector", "Sergeant", "Sub-Inspector", "Traffic Constable"},
                {"Unit Head (Cyber Crime)", "Additional Unit Head", "Cyber Crime Officer", "Digital Forensics Analyst", "Cyber Investigator", "Technical Assistant"},
                {"Unit Commander", "Deputy Unit Commander", "Operations Officer", "Investigation Officer", "Inspector", "Sub-Inspector"},
                {"Unit Commander", "Deputy Commander", "Operations Officer", "Inspector", "Sergeant", "Constable"},
                {"Unit Head (Victim Support)", "Assistant Unit Head", "Victim Support Officer", "Counselling Coordinator", "Investigation Officer", "Support Staff"},
                {"Legal Affairs Officer", "Assistant Legal Officer", "Prosecution Liaison Officer", "Legal Research Officer", "Office Assistant"},
                {"Training Superintendent", "Assistant Training Superintendent", "Chief Instructor", "Training Instructor", "Course Coordinator"},
                {"Logistics Officer", "Transport Supervisor", "Fleet Coordinator", "Store Officer", "Vehicle Operator", "Logistics Assistant"},
                {"IT Superintendent", "Assistant IT Officer", "Network Administrator", "System Administrator", "Radio Communication Officer", "IT Support Technician"},
                {"Accounts Superintendent", "Assistant Accounts Officer", "Finance Officer", "Audit Officer", "Accounts Assistant"},
                {"Procurement Officer", "Assistant Procurement Officer", "Store Keeper", "Inventory Officer", "Procurement Assistant"},
                {"Internal Affairs Officer", "Assistant Internal Affairs Officer", "Discipline Inspector", "Inquiry Officer", "Investigation Assistant"},
                {"Public Relations Officer", "Community Policing Officer", "Media Liaison Officer", "Community Outreach Officer", "Public Information Assistant"},
                {"Control Room Supervisor", "Emergency Response Officer", "Shift Supervisor", "Radio Operator", "Call Taker", "Control Room Assistant"},
                {"Welfare Officer", "Medical Officer", "Nursing Assistant", "Welfare Coordinator", "Medical Support Staff"}
        };

        for (int i = 0; i < 300; i++) {
            int departmentIndex = i % defaults.length;
            int nameIndex = i;
            String fullName = firstNames[nameIndex % firstNames.length] + " "
                    + lastNames[(nameIndex / firstNames.length) % lastNames.length];
            String[] ranks = ranksByDepartment[departmentIndex];
            String designation = ranks[(i / defaults.length) % ranks.length];
            String employeeId = String.format(Locale.ROOT, "DPH-EMP-%03d", i + 1);
            data.get("Employees").add(new String[]{employeeId, fullName, defaults[departmentIndex][0],
                    designation, "Not Provided", "Active", "Outside"});
        }

        save("Employees");
        log("STAFF DIRECTORY INITIALIZED", "300 sample staff records across " + defaults.length + " departments");
    }

    private void log(String action, String details) {

        data.get("Audit Logs").add(new String[]{now(), currentUser, action, details});
        save("Audit Logs");

        if (action.equals("EMPLOYEE CHECK IN") || action.equals("EMPLOYEE CHECK OUT")
                || action.equals("VISITOR CHECK IN") || action.equals("VISITOR CHECK OUT")) {
            syncAttendanceFiles();
        }
    }

    // Build dedicated attendance.csv and attendance.txt from the permanent audit log.
    // This also imports older attendance events already present in Audit Logs.
    private List<String[]> attendanceRecords() {
        List<String[]> records = new ArrayList<>();

        for (String[] log : data.get("Audit Logs")) {
            if (log.length < 4) continue;

            String action = log[2];
            boolean employee = action.equals("EMPLOYEE CHECK IN") || action.equals("EMPLOYEE CHECK OUT");
            boolean visitor = action.equals("VISITOR CHECK IN") || action.equals("VISITOR CHECK OUT");
            if (!employee && !visitor) continue;

            String personId = log[3].trim();
            String[] person = find(employee ? "Employees" : "Visitors", 0, personId);
            String name = person == null ? "Unknown" : person[1];
            String movement = action.endsWith("CHECK IN") ? "CHECK-IN" : "CHECK-OUT";

            records.add(new String[]{
                    log[0], employee ? "Employee" : "Visitor",
                    personId, name, movement, log[1]
            });
        }

        return records;
    }

    private String csvValue(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private void syncAttendanceFiles() {
        try {
            Files.createDirectories(Paths.get(DIR));
            List<String[]> records = attendanceRecords();

            List<String> csv = new ArrayList<>();
            csv.add("Date & Time,Person Type,Person ID,Person Name,Movement,Guard");
            for (String[] r : records) {
                List<String> cells = new ArrayList<>();
                for (String value : r) cells.add(csvValue(value));
                csv.add(String.join(",", cells));
            }
            Files.write(Paths.get(DIR, "attendance.csv"), csv, StandardCharsets.UTF_8);

            List<String> txt = new ArrayList<>();
            txt.add("AUREX SECURITY SYSTEM - ATTENDANCE REPORT");
            txt.add("Generated: " + now());
            txt.add("Total attendance movements: " + records.size());
            txt.add("============================================================");
            for (String[] r : records) {
                txt.add("Date & Time : " + r[0]);
                txt.add("Person Type : " + r[1]);
                txt.add("Person ID   : " + r[2]);
                txt.add("Person Name : " + r[3]);
                txt.add("Movement    : " + r[4]);
                txt.add("Guard       : " + r[5]);
                txt.add("------------------------------------------------------------");
            }
            Files.write(Paths.get(DIR, "attendance.txt"), txt, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Attendance export sync failed: " + ex.getMessage());
        }
    }

    private String now() {
        return LocalDateTime.now().format(FMT);
    }

    private String id(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    private void showError(String s) {
        JOptionPane.showMessageDialog(this, s, "AUREX SECURITY", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String s) {
        JOptionPane.showMessageDialog(this, s, "AUREX SECURITY", JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= FACE RECOGNITION WINDOW OPENER =================
    private void openFaceRecognition() {
        try {
            FaceRecognition faceWindow = new FaceRecognition(); 
            faceWindow.setLocationRelativeTo(this);
            
            // Window বন্ধ হলেই মেমোরির ডাটা ফাইল থেকে রি-লোড হবে এবং ড্যাশবোর্ড রিফ্রেশ হবে
            faceWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    loadAll();              // ফাইল থেকে নতুন Inside/Outside পড়ে নিবে
                    refreshStats();         // ড্যাশবোর্ডের সংখ্যা (1) বাড়াবে
                    refreshTableData("Employees"); // Employee টেবিল রিফ্রেশ করবে
                }
            });

            faceWindow.setVisible(true);
            log("FACE RECOGNITION", "Opened Face Recognition Window");
        } catch (Exception ex) {
            showError("Unable to open Face Recognition System:\n" + ex.getMessage());
        }
    }
    // ================= MAIN INTERFACE =================

    private void buildUI() {

        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        // ---------- Top bar ----------
        JPanel top = new JPanel(new BorderLayout());

        top.setBackground(new Color(8, 17, 34));
        top.setBorder(new EmptyBorder(10, 18, 10, 20));

        JPanel brandArea = new JPanel(new BorderLayout(12, 0));
        brandArea.setOpaque(false);

        AurexLogo logo = new AurexLogo(true);
        logo.setPreferredSize(new Dimension(300, 82));

        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 3));
        brandText.setOpaque(false);

        JLabel sub = new JLabel("SMART SECURITY & VISITOR TRACKING SYSTEM");
        sub.setForeground(MUTED);
        sub.setFont(bodyBold(11));

        JLabel control = new JLabel("POLICE DIVISIONAL HEADQUARTERS");
        control.setForeground(GREEN);
        control.setFont(italic(10));

        brandText.add(sub);
        brandText.add(control);

        brandArea.add(logo, BorderLayout.WEST);
        brandArea.add(brandText, BorderLayout.SOUTH);

        top.add(brandArea, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 15));
        right.setOpaque(false);

        clock.setForeground(TEXT);
        clock.setFont(mono(14));
        right.add(clock);

        JLabel user = new JLabel("●  " + currentUser + "  |  " + role);
        user.setForeground(GREEN);
        user.setFont(bodyBold(13));
        right.add(user);

        notificationBadge = new JLabel("🔔  " + pendingApprovalTotal());
        notificationBadge.setForeground(GOLD);
        notificationBadge.setFont(bodyBold(13));
        notificationBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notificationBadge.setToolTipText("Open pending approval notifications");
        notificationBadge.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showPage("Notifications"); }
        });
        right.add(notificationBadge);

        NeonButton logout = new NeonButton("⏻ Logout", RED);
        logout.setBorder(new EmptyBorder(7, 14, 7, 14));
        logout.addActionListener(e -> logout());
        right.add(logout);

        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // ---------- Sidebar ----------
        JPanel side = new JPanel();

        side.setBackground(new Color(9, 19, 37));
        side.setPreferredSize(new Dimension(235, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(18, 12, 18, 12));

        JLabel menuTitle = new JLabel("  MAIN MENU");
        menuTitle.setForeground(new Color(90, 125, 165));
        menuTitle.setFont(head(11));
        menuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        side.add(menuTitle);
        side.add(Box.createVerticalStrut(13));

        String[][] nav = {
                {"⌂", "Dashboard"}, {"♙", "Employees"}, {"♧", "Visitors"},
                {"✓", "Authorization"}, {"◉", "Face Recognition"}, {"▣", "Departments"},
                {"⊘", "Blacklist"}, {"◷", "Appointments"}, {"▤", "Vehicles"},
                {"▥", "Materials"}, {"⚑", "Restricted Access"}, {"!", "Incidents"},
                {"⌕", "Lost & Found"}, {"☷", "Duty Roster"}, {"▦", "Reports"},
                {"≡", "Audit Logs"}, {"🚨", "Emergency Alerts"}, {"📊", "Analytics"},
                {"🔔", "Notifications"}, {"⇄", "Entry & Exit Monitoring"}, {"◷", "Attendance Analytics"}
        };

        for (String[] n : nav) {

            final String key = n[1];

            NavButton b = new NavButton(key, colorOf(key), n[0]);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (key.equals("Face Recognition")) {
                b.addActionListener(e -> openFaceRecognition());
            } else {
                b.addActionListener(e -> showPage(key));
            }

            navButtons.put(key, b);

            side.add(b);
            side.add(Box.createVerticalStrut(5));
        }

        side.add(Box.createVerticalGlue());

        JLabel foot = new JLabel("  AUREX  •  SECURE NETWORK");
        foot.setForeground(GREEN);
        foot.setFont(bodyBold(9));
        side.add(foot);

        JScrollPane sideScroll = new JScrollPane(side,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(null);
        sideScroll.getViewport().setBackground(new Color(9, 19, 37));
        sideScroll.getVerticalScrollBar().setUnitIncrement(14);
        sideScroll.setPreferredSize(new Dimension(235, 0));

        add(sideScroll, BorderLayout.WEST);

        // ---------- Content ----------
        content.setBackground(BG);
        add(content, BorderLayout.CENTER);

        content.add(dashboard(), "Dashboard");

        for (String key : schemas.keySet()) {
            content.add(modulePage(key), key);
        }

        content.add(authorizationPage(), "Authorization");
        analyticsPanel = analyticsPage();
        notificationsPanel = notificationsPage();
        content.add(analyticsPanel, "Analytics");
        content.add(notificationsPanel, "Notifications");
        movementPanel = movementMonitoringPage();
        content.add(movementPanel, "Entry & Exit Monitoring");
        JPanel attendancePage = attendanceAnalyticsPage();
        attendancePage.setName("Attendance Analytics");
        content.add(attendancePage, "Attendance Analytics");

        reportsPanel = reportsPage();
        content.add(reportsPanel, "Reports");

        // ---------- Footer ----------
        JPanel bottom = new JPanel(new BorderLayout());

        bottom.setBackground(new Color(8, 17, 34));
        bottom.setBorder(new EmptyBorder(7, 15, 7, 15));

        JLabel status = new JLabel("● AUREX SECURITY NETWORK     |     ALL SYSTEMS OPERATIONAL");
        status.setForeground(GREEN);
        status.setFont(bodyBold(10));

        JLabel storage = new JLabel("TXT STORAGE ENABLED");
        storage.setForeground(MUTED);
        storage.setFont(mono(10));

        bottom.add(status, BorderLayout.WEST);
        bottom.add(storage, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        showPage("Dashboard");
    }

    private void logout() {

        if (JOptionPane.showConfirmDialog(this, "Do you want to log out?",
                "AUREX SECURITY", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        log("LOGOUT", "Role: " + role);
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void showPage(String key) {

        if (key.equals("Dashboard")) {
            refreshStats();
        }
        if (key.equals("Entry & Exit Monitoring")) {
            content.remove(movementPanel);
            movementPanel = movementMonitoringPage();
            content.add(movementPanel, "Entry & Exit Monitoring");
        }

        if (key.equals("Authorization")) {
            refreshTableData("Visitors");
        }
        if (key.equals("Notifications")) {
            content.remove(notificationsPanel);
            notificationsPanel = notificationsPage();
            content.add(notificationsPanel, "Notifications");
            movementPanel = movementMonitoringPage();
            content.add(movementPanel, "Entry & Exit Monitoring");
        }
        if (key.equals("Attendance Analytics")) {
            for (Component component : content.getComponents()) {
                if ("Attendance Analytics".equals(component.getName())) {
                    content.remove(component);
                    break;
                }
            }
            JPanel attendancePage = attendanceAnalyticsPage();
            attendancePage.setName("Attendance Analytics");
            content.add(attendancePage, "Attendance Analytics");
        }
        if (key.equals("Analytics")) {
            content.remove(analyticsPanel);
            analyticsPanel = analyticsPage();
            content.add(analyticsPanel, "Analytics");
        }
        if (key.equals("Emergency Alerts")) {
            refreshTableData("Emergency Alerts");
        }

        if (key.equals("Reports")) {
            content.remove(reportsPanel);
            reportsPanel = reportsPage();
            content.add(reportsPanel, "Reports");
        }

        if (tables.containsKey(key)) {
            refreshTableData(key);
        }

        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setActive(e.getKey().equals(key));
        }

        cards.show(content, key);
    }

    // ================= PAGE HEADER =================

    private JPanel pageBase(String title, String desc, Color accent) {

        JPanel p = new JPanel(new BorderLayout(12, 16));

        p.setBackground(BG);
        p.setBorder(new EmptyBorder(24, 25, 22, 25));

        JPanel h = new JPanel();
        h.setOpaque(false);
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setForeground(accent);
        t.setFont(head(30));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel d = new JLabel(desc);
        d.setForeground(MUTED);
        d.setFont(italic(13));
        d.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, accent, getWidth(), 0,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)));
                g2.fillRoundRect(0, 0, getWidth(), 3, 3, 3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 3));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        h.add(t);
        h.add(Box.createVerticalStrut(3));
        h.add(d);
        h.add(Box.createVerticalStrut(10));
        h.add(bar);

        p.add(h, BorderLayout.NORTH);

        return p;
    }

    // ================= DASHBOARD =================

    private JPanel dashboard() {

        JPanel p = pageBase("Smart Security & Visitor Tracking System",
                "Police Divisional Headquarters — staff movement, visitor access and security operations.",
                colorOf("Dashboard"));

        JPanel body = new JPanel(new BorderLayout(15, 15));
        body.setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);

        empCount = stat(stats, "EMPLOYEES", "0", colorOf("Employees"), "♙");
        visitorCount = stat(stats, "VISITORS TODAY", "0", colorOf("Visitors"), "♧");
        insideCount = stat(stats, "CURRENTLY INSIDE", "0", GOLD, "◷");
        pendingCount = stat(stats, "PENDING APPROVAL", "0", RED, "!");

        JPanel actions = new JPanel(new GridLayout(0, 3, 12, 12));
        actions.setOpaque(false);

        actions.add(actionCard("＋  Register Employee",
                "Add a new employee to the directory",
                colorOf("Employees"), () -> employeeForm()));

        actions.add(actionCard("♧  Register Visitor",
                "Create a visitor authorization request",
                colorOf("Visitors"), () -> visitorForm()));

        actions.add(actionCard("✓  Review Authorization",
                "Approve or reject pending visitors",
                colorOf("Authorization"), () -> showPage("Authorization")));

        actions.add(actionCard("◉  Face Recognition",
                "Launch biometric face registration and verification window",
                colorOf("Face Recognition"), () -> openFaceRecognition()));

        actions.add(actionCard("▦  Generate Report",
                "View system records and summaries",
                colorOf("Reports"), () -> showPage("Reports")));

        actions.add(actionCard("🚨  Emergency Alert",
                "Report fire, medical or security emergencies",
                colorOf("Emergency Alerts"), () -> emergencyForm()));
        actions.add(actionCard("📊  Analytics Dashboard",
                "Explore visitor trends and security statistics",
                colorOf("Analytics"), () -> showPage("Analytics")));
        actions.add(actionCard("⇄  Entry & Exit Monitoring",
                "See every employee and visitor currently inside, plus today's movement history",
                new Color(0, 220, 190), () -> showPage("Entry & Exit Monitoring")));

        JPanel lower = new JPanel(new BorderLayout(10, 10));
        lower.setOpaque(false);

        JLabel tip = new JLabel(
                "ACCESS POLICY: Employees can enter directly. External visitors require department approval.");
        tip.setForeground(GREEN);
        tip.setFont(bodyBold(13));

        JLabel note = new JLabel("TXT storage enabled  •  Data folder: ./" + DIR
                + "  •  Face recognition module available");
        note.setForeground(MUTED);
        note.setFont(italic(12));

        lower.add(tip, BorderLayout.NORTH);
        lower.add(note, BorderLayout.CENTER);

        body.add(stats, BorderLayout.NORTH);
        body.add(actions, BorderLayout.CENTER);
        body.add(lower, BorderLayout.SOUTH);

        p.add(body, BorderLayout.CENTER);

        refreshStats();

        return p;
    }

    // ================= NEW SECURITY FEATURES =================

    private long pendingApprovalTotal() {
        if (!data.containsKey("Visitors")) return 0;
        return data.get("Visitors").stream().filter(r -> r.length > 9 && "Pending".equalsIgnoreCase(r[9])).count();
    }

    private void emergencyForm() {
        JTextField location = field();
        JTextField description = field();
        JComboBox<String> type = new JComboBox<>(new String[]{
                "Fire Emergency", "Medical Emergency", "Unauthorized Entry",
                "Suspicious Activity", "Theft / Security Breach", "Other"});
        JComboBox<String> priority = new JComboBox<>(new String[]{"Critical", "High", "Medium", "Low"});
        JPanel form = new JPanel(new GridLayout(4, 2, 9, 9));
        form.setBackground(PANEL);
        form.add(label("Emergency Type", RED)); form.add(type);
        form.add(label("Building / Location", RED)); form.add(location);
        form.add(label("Priority", RED)); form.add(priority);
        form.add(label("Description", RED)); form.add(description);
        styleCombo(type); styleCombo(priority);
        int result = JOptionPane.showConfirmDialog(this, form, "🚨 Create Emergency Alert",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        if (location.getText().trim().isEmpty() || description.getText().trim().isEmpty()) {
            showError("Location and description are required."); return;
        }
        String[] row = {id("ALT"), String.valueOf(type.getSelectedItem()), location.getText().trim(),
                String.valueOf(priority.getSelectedItem()), description.getText().trim(),
                currentUser, now(), "Open"};
        data.get("Emergency Alerts").add(row);
        save("Emergency Alerts");
        log("EMERGENCY ALERT CREATED", row[1] + " at " + row[2] + " / " + row[3]);
        refreshTableData("Emergency Alerts");
        info("Emergency alert created. It is now visible in Emergency Alerts.");
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(PANEL2);
        combo.setForeground(TEXT);
        combo.setFont(bodyBold(13));
        combo.setFocusable(false);
        combo.setBorder(new LineBorder(new Color(48, 75, 110)));
        combo.setMaximumRowCount(10);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                item.setFont(bodyBold(13));
                item.setOpaque(true);
                item.setBorder(new EmptyBorder(7, 10, 7, 10));
                if (isSelected) {
                    item.setBackground(new Color(0, 105, 130));
                    item.setForeground(Color.WHITE);
                } else {
                    item.setBackground(PANEL2);
                    item.setForeground(TEXT);
                }
                return item;
            }
        });
    }

    private JPanel notificationsPage() {
        JPanel p = pageBase("🔔 Notifications Center",
                "Review pending visitor approvals and important security notices.", colorOf("Notifications"));
        JPanel wrap = new JPanel(new BorderLayout(10, 12)); wrap.setOpaque(false);
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5)); summary.setOpaque(false);
        JLabel count = new JLabel("PENDING APPROVALS: " + pendingApprovalTotal());
        count.setForeground(GOLD); count.setFont(head(18)); summary.add(count);
        NeonButton refresh = new NeonButton("↻ Refresh Notifications", BLUE);
        refresh.addActionListener(e -> showPage("Notifications")); summary.add(refresh);
        NeonButton open = new NeonButton("✓ Open Approval Desk", GREEN);
        open.addActionListener(e -> showPage("Authorization")); summary.add(open);
        wrap.add(summary, BorderLayout.NORTH);
        String[] cols = {"Visitor ID", "Visitor Name", "Department", "Host", "Visit Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r,int c){return false;} };
        for (String[] v : data.get("Visitors")) if (v.length > 9 && "Pending".equalsIgnoreCase(v[9]))
            model.addRow(new Object[]{v[0],v[1],v[5],v[6],v[7],"⏳ Pending Approval"});
        JTable table = new JTable(model); styleTable(table, colorOf("Notifications"));
        wrap.add(scroll(table, colorOf("Notifications")), BorderLayout.CENTER);
        JLabel hint = new JLabel("New visitor requests appear here automatically when registered.");
        hint.setForeground(MUTED); hint.setFont(italic(12)); wrap.add(hint, BorderLayout.SOUTH);
        p.add(wrap, BorderLayout.CENTER); return p;
    }

    private JPanel analyticsPage() {
        JPanel p = pageBase("📊 Advanced Analytics Dashboard",
                "Visual overview generated from the records stored in your AUREX TXT files.", colorOf("Analytics"));
        JPanel all = new JPanel(new BorderLayout(12, 12)); all.setOpaque(false);
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 12, 12)); cardsPanel.setOpaque(false);
        addMetric(cardsPanel, "TOTAL VISITORS", String.valueOf(data.get("Visitors").size()), GREEN, "👥");
        addMetric(cardsPanel, "PENDING APPROVAL", String.valueOf(pendingApprovalTotal()), GOLD, "🔔");
        addMetric(cardsPanel, "EMERGENCY ALERTS", String.valueOf(data.get("Emergency Alerts").size()), RED, "🚨");
        long active = data.get("Emergency Alerts").stream().filter(r -> r.length > 7 && "Open".equalsIgnoreCase(r[7])).count();
        addMetric(cardsPanel, "OPEN EMERGENCIES", String.valueOf(active), RED, "⚠");
        all.add(cardsPanel, BorderLayout.NORTH);
        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 12)); charts.setOpaque(false);
        charts.add(new AnalyticsChart("VISITORS BY MONTH", monthlyVisitorCounts(), BLUE));
        charts.add(new AnalyticsChart("EMERGENCY TYPES", emergencyTypeCounts(), RED));
        all.add(charts, BorderLayout.CENTER);
        NeonButton reload = new NeonButton("↻ Refresh Analytics", colorOf("Analytics"));
        reload.addActionListener(e -> showPage("Analytics"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.setOpaque(false); bottom.add(reload);
        all.add(bottom, BorderLayout.SOUTH); p.add(all, BorderLayout.CENTER); return p;
    }

    private void addMetric(JPanel parent, String title, String value, Color accent, String icon) {
        JPanel box = new JPanel(new BorderLayout(5, 8)); box.setBackground(PANEL);
        box.setBorder(new CompoundBorder(new MatteBorder(3,0,0,0,accent), new EmptyBorder(12,14,12,14)));
        JLabel ic = new JLabel(icon); ic.setFont(head(25)); ic.setForeground(accent);
        JLabel val = new JLabel(value); val.setFont(head(29)); val.setForeground(accent);
        JLabel name = new JLabel(title); name.setFont(bodyBold(10)); name.setForeground(MUTED);
        box.add(ic,BorderLayout.WEST); box.add(val,BorderLayout.CENTER); box.add(name,BorderLayout.SOUTH); parent.add(box);
    }

    private int[] monthlyVisitorCounts() {
        int[] counts = new int[6]; YearMonth current = YearMonth.now();
        for (String[] v : data.get("Visitors")) try {
            YearMonth m = YearMonth.from(LocalDate.parse(v[7])); long diff = java.time.temporal.ChronoUnit.MONTHS.between(m,current);
            if (diff >= 0 && diff < 6) counts[5-(int)diff]++;
        } catch (Exception ignored) { }
        return counts;
    }

    private int[] emergencyTypeCounts() {
        String[] types = {"Fire", "Medical", "Unauthorized", "Suspicious", "Other"}; int[] c = new int[types.length];
        for (String[] a : data.get("Emergency Alerts")) {
            String t = a[1].toLowerCase(Locale.ROOT);
            if (t.contains("fire")) c[0]++; else if (t.contains("medical")) c[1]++;
            else if (t.contains("unauthorized")) c[2]++; else if (t.contains("suspicious")) c[3]++; else c[4]++;
        }
        return c;
    }

    private class AnalyticsChart extends JPanel {
        private final String title; private final int[] values; private final Color accent;
        AnalyticsChart(String title, int[] values, Color accent) { this.title=title; this.values=values; this.accent=accent;
            setBackground(PANEL); setBorder(new CompoundBorder(new LineBorder(new Color(40,65,100)),new EmptyBorder(12,12,12,12))); }
        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(accent); g2.setFont(head(15)); g2.drawString(title,18,28);
            int max=1; for(int v:values) max=Math.max(max,v); int base=getHeight()-42, chartTop=58, gap=18;
            int usable=Math.max(80,getWidth()-50), bw=Math.max(12,(usable-gap*(values.length-1))/values.length);
            String[] labels=title.startsWith("VISITORS")?new String[]{"-5M","-4M","-3M","-2M","-1M","NOW"}:new String[]{"Fire","Medical","Access","Susp.","Other"};
            for(int i=0;i<values.length;i++){ int x=25+i*(bw+gap); int h=(int)((base-chartTop-20)*(values[i]/(double)max));
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),55)); g2.fillRoundRect(x,base-h,bw,h,9,9);
                g2.setColor(accent); g2.fillRoundRect(x,base-h,bw,Math.min(7,h),8,8); g2.setFont(bodyBold(11));
                g2.drawString(String.valueOf(values[i]),x,base-h-7); g2.setColor(MUTED); g2.setFont(body(10)); g2.drawString(labels[i],x,base+17); }
            g2.dispose(); }
    }


    // ================= COMBINED EMPLOYEE & VISITOR MOVEMENT MONITOR =================

    private JPanel movementMonitoringPage() {
        JPanel p = pageBase("Entry & Exit Monitoring",
                "Live view of employees and external visitors — who is inside and who entered or left today.",
                new Color(0, 220, 190));

        JPanel wrap = new JPanel(new BorderLayout(12, 12));
        wrap.setOpaque(false);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);

        long employeeInside = data.get("Employees").stream()
                .filter(r -> r.length > 6 && "Inside".equalsIgnoreCase(r[6])).count();
        long visitorInside = data.get("Visitors").stream()
                .filter(r -> r.length > 9 && "Inside".equalsIgnoreCase(r[9])).count();

        addMetric(metrics, "EMPLOYEES INSIDE", String.valueOf(employeeInside),
                colorOf("Employees"), "♙");
        addMetric(metrics, "VISITORS INSIDE", String.valueOf(visitorInside),
                colorOf("Visitors"), "♧");
        addMetric(metrics, "TOTAL PEOPLE INSIDE", String.valueOf(employeeInside + visitorInside),
                GREEN, "◷");

        String[] insideColumns = {
                "Person Type", "ID", "Name", "Department", "Check In Time", "Check Out Time", "Current Status"
        };
        DefaultTableModel insideModel = new DefaultTableModel(insideColumns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        for (String[] e : data.get("Employees")) {
            if (e.length > 6 && "Inside".equalsIgnoreCase(e[6])) {
                String inTime = latestMovementTime("EMPLOYEE CHECK IN", e[0]);
                insideModel.addRow(new Object[]{"EMPLOYEE", e[0], e[1], e[2],
                        inTime.isEmpty() ? "Not recorded" : inTime, "-", "INSIDE"});
            }
        }

        for (String[] v : data.get("Visitors")) {
            if (v.length > 11 && "Inside".equalsIgnoreCase(v[9])) {
                insideModel.addRow(new Object[]{"VISITOR", v[0], v[1], v[5],
                        v[10], "-", "INSIDE"});
            }
        }

        JTable insideTable = new JTable(insideModel);
        styleTable(insideTable, GREEN);

        String[] historyColumns = {"Date & Time", "Person Type", "Movement", "Person ID", "Recorded By"};
        DefaultTableModel historyModel = new DefaultTableModel(historyColumns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        String today = LocalDate.now().toString();
        List<String[]> todaysMovements = new ArrayList<>();
        for (String[] logRow : data.get("Audit Logs")) {
            if (logRow.length > 3 && logRow[0].startsWith(today)
                    && (logRow[2].equals("EMPLOYEE CHECK IN")
                    || logRow[2].equals("EMPLOYEE CHECK OUT")
                    || logRow[2].equals("VISITOR CHECK IN")
                    || logRow[2].equals("VISITOR CHECK OUT"))) {
                todaysMovements.add(logRow);
            }
        }
        Collections.reverse(todaysMovements);
        for (String[] logRow : todaysMovements) {
            String type = logRow[2].startsWith("EMPLOYEE") ? "EMPLOYEE" : "VISITOR";
            String movement = logRow[2].endsWith("CHECK IN") ? "CHECK IN" : "CHECK OUT";
            historyModel.addRow(new Object[]{logRow[0], type, movement, logRow[3], logRow[1]});
        }

        JTable historyTable = new JTable(historyModel);
        styleTable(historyTable, BLUE);

        JPanel topArea = new JPanel(new BorderLayout(8, 10));
        topArea.setOpaque(false);
        topArea.add(metrics, BorderLayout.NORTH);

        JPanel insidePanel = new JPanel(new BorderLayout(6, 6));
        insidePanel.setOpaque(false);
        JLabel insideTitle = new JLabel("  CURRENTLY INSIDE — EMPLOYEES + VISITORS");
        insideTitle.setForeground(GREEN);
        insideTitle.setFont(head(17));
        insidePanel.add(insideTitle, BorderLayout.NORTH);
        insidePanel.add(scroll(insideTable, GREEN), BorderLayout.CENTER);

        JPanel historyPanel = new JPanel(new BorderLayout(6, 6));
        historyPanel.setOpaque(false);
        JLabel historyTitle = new JLabel("  TODAY'S CHECK IN / CHECK OUT HISTORY");
        historyTitle.setForeground(BLUE);
        historyTitle.setFont(head(17));
        historyPanel.add(historyTitle, BorderLayout.NORTH);
        historyPanel.add(scroll(historyTable, BLUE), BorderLayout.CENTER);

        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 0, 12));
        tablesPanel.setOpaque(false);
        tablesPanel.add(insidePanel);
        tablesPanel.add(historyPanel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        NeonButton employeeIn = new NeonButton("↗ Employee Check In", GREEN);
        employeeIn.addActionListener(e -> {
            employeeMovement(true);
            showPage("Entry & Exit Monitoring");
        });
        NeonButton employeeOut = new NeonButton("↘ Employee Check Out", colorOf("Employees"));
        employeeOut.addActionListener(e -> {
            employeeMovement(false);
            showPage("Entry & Exit Monitoring");
        });
        NeonButton visitorIn = new NeonButton("↗ Visitor Check In", colorOf("Visitors"));
        visitorIn.addActionListener(e -> {
            visitorMovement(true);
            showPage("Entry & Exit Monitoring");
        });
        NeonButton visitorOut = new NeonButton("↘ Visitor Check Out", BLUE);
        visitorOut.addActionListener(e -> {
            visitorMovement(false);
            showPage("Entry & Exit Monitoring");
        });
        NeonButton refresh = new NeonButton("↻ Refresh Movement", new Color(0, 160, 150));
        refresh.addActionListener(e -> showPage("Entry & Exit Monitoring"));

        actions.add(employeeIn);
        actions.add(employeeOut);
        actions.add(visitorIn);
        actions.add(visitorOut);
        actions.add(refresh);

        wrap.add(topArea, BorderLayout.NORTH);
        wrap.add(tablesPanel, BorderLayout.CENTER);
        wrap.add(actions, BorderLayout.SOUTH);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    private String latestMovementTime(String action, String personId) {
        List<String[]> logs = data.get("Audit Logs");
        for (int i = logs.size() - 1; i >= 0; i--) {
            String[] r = logs.get(i);
            if (r.length > 3 && action.equals(r[2]) && personId.equals(r[3])) {
                return r[0];
            }
        }
        return "";
    }

    // ================= ATTENDANCE HISTORY & ANALYTICS =================

    private JPanel attendanceAnalyticsPage() {
        JPanel p = pageBase("AUREX — Attendance History & Analytics",
                "Complete employee and visitor movement history, searchable and exportable from TXT-based records.",
                colorOf("Attendance Analytics"));

        JPanel wrap = new JPanel(new BorderLayout(12, 12));
        wrap.setOpaque(false);

        List<String[]> allRecords = attendanceRecords();
        LocalDate today = LocalDate.now();

        long employeeTotal = allRecords.stream()
                .filter(r -> "Employee".equals(r[1]) && "CHECK-IN".equals(r[4])).count();
        long visitorTotal = allRecords.stream()
                .filter(r -> "Visitor".equals(r[1]) && "CHECK-IN".equals(r[4])).count();
        long todayEntry = allRecords.stream()
                .filter(r -> r[0].startsWith(today.toString()) && "CHECK-IN".equals(r[4])).count();
        long todayExit = allRecords.stream()
                .filter(r -> r[0].startsWith(today.toString()) && "CHECK-OUT".equals(r[4])).count();

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 12));
        metrics.setOpaque(false);
        addMetric(metrics, "EMPLOYEE ATTENDANCE", String.valueOf(employeeTotal), colorOf("Employees"), "♙");
        addMetric(metrics, "VISITOR ATTENDANCE", String.valueOf(visitorTotal), colorOf("Visitors"), "♧");
        addMetric(metrics, "TODAY'S CHECK-IN", String.valueOf(todayEntry), GREEN, "↗");
        addMetric(metrics, "TODAY'S CHECK-OUT", String.valueOf(todayExit), BLUE, "↘");

        JTextField search = field();
        search.setToolTipText("Search Employee ID, Visitor ID, name or guard");
        JTextField dateFilter = field();
        dateFilter.setToolTipText("Optional date filter: yyyy-MM-dd");
        JComboBox<String> guardFilter = combo(attendanceGuardOptions());
        JComboBox<String> typeFilter = combo(new String[]{"All Types", "Employee", "Visitor"});
        JComboBox<String> movementFilter = combo(new String[]{"All Movements", "CHECK-IN", "CHECK-OUT"});

        JPanel filters = new JPanel(new GridLayout(2, 5, 8, 8));
        filters.setOpaque(false);
        filters.add(label("ID / Name / Guard Search", BLUE));
        filters.add(label("Date (yyyy-MM-dd)", BLUE));
        filters.add(label("Guard-wise Filter", BLUE));
        filters.add(label("Person Type", BLUE));
        filters.add(label("Movement", BLUE));
        filters.add(search);
        filters.add(dateFilter);
        filters.add(guardFilter);
        filters.add(typeFilter);
        filters.add(movementFilter);

        String[] columns = {"Date & Time", "Person Type", "Person ID", "Person Name", "Movement", "Guard"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table, colorOf("Attendance Analytics"));

        Runnable applyFilters = () -> {
            model.setRowCount(0);
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            String date = dateFilter.getText().trim();
            String selectedGuard = String.valueOf(guardFilter.getSelectedItem());
            String selectedType = String.valueOf(typeFilter.getSelectedItem());
            String selectedMovement = String.valueOf(movementFilter.getSelectedItem());

            for (String[] r : attendanceRecords()) {
                if (!q.isEmpty()) {
                    String combined = String.join(" ", r).toLowerCase(Locale.ROOT);
                    if (!combined.contains(q)) continue;
                }
                if (!date.isEmpty() && !r[0].startsWith(date)) continue;
                if (!"All Guards".equals(selectedGuard) && !r[5].equalsIgnoreCase(selectedGuard)) continue;
                if (!"All Types".equals(selectedType) && !r[1].equalsIgnoreCase(selectedType)) continue;
                if (!"All Movements".equals(selectedMovement) && !r[4].equalsIgnoreCase(selectedMovement)) continue;
                model.addRow(r);
            }
        };

        NeonButton searchBtn = new NeonButton("⌕ Apply Filters", colorOf("Attendance Analytics"));
        searchBtn.addActionListener(e -> {
            if (!dateFilter.getText().trim().isEmpty()
                    && !dateFilter.getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                showError("Date must use yyyy-MM-dd format.");
                return;
            }
            applyFilters.run();
        });

        NeonButton resetBtn = new NeonButton("↻ Reset", new Color(70, 100, 140));
        resetBtn.addActionListener(e -> {
            search.setText("");
            dateFilter.setText("");
            guardFilter.setSelectedIndex(0);
            typeFilter.setSelectedIndex(0);
            movementFilter.setSelectedIndex(0);
            applyFilters.run();
        });

        NeonButton csvBtn = new NeonButton("⇩ Export CSV", GREEN);
        csvBtn.addActionListener(e -> exportAttendanceTable(model, true));

        NeonButton txtBtn = new NeonButton("▤ Generate TXT Report", BLUE);
        txtBtn.addActionListener(e -> exportAttendanceTable(model, false));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        controls.add(searchBtn);
        controls.add(resetBtn);
        controls.add(csvBtn);
        controls.add(txtBtn);

        JPanel north = new JPanel(new BorderLayout(8, 10));
        north.setOpaque(false);
        north.add(metrics, BorderLayout.NORTH);
        JPanel filterArea = new JPanel(new BorderLayout(6, 8));
        filterArea.setOpaque(false);
        filterArea.add(filters, BorderLayout.CENTER);
        filterArea.add(controls, BorderLayout.SOUTH);
        north.add(filterArea, BorderLayout.SOUTH);

        JLabel countLabel = new JLabel();
        countLabel.setForeground(MUTED);
        countLabel.setFont(bodyBold(12));
        Runnable updateCount = () -> countLabel.setText("Showing " + model.getRowCount()
                + " attendance movement(s)  •  Total stored movements: " + attendanceRecords().size());

        applyFilters.run();
        updateCount.run();

        searchBtn.addActionListener(e -> updateCount.run());
        resetBtn.addActionListener(e -> updateCount.run());

        wrap.add(north, BorderLayout.NORTH);
        JPanel tableArea = new JPanel(new BorderLayout(6, 6));
        tableArea.setOpaque(false);
        tableArea.add(countLabel, BorderLayout.NORTH);
        tableArea.add(scroll(table, colorOf("Attendance Analytics")), BorderLayout.CENTER);
        wrap.add(tableArea, BorderLayout.CENTER);

        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    private String[] attendanceGuardOptions() {
        LinkedHashSet<String> guards = new LinkedHashSet<>();
        guards.add("All Guards");
        for (String[] r : attendanceRecords()) {
            if (r.length > 5 && !r[5].isBlank()) guards.add(r[5]);
        }
        return guards.toArray(new String[0]);
    }

    private void exportAttendanceTable(DefaultTableModel model, boolean csvFormat) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(csvFormat ? "AUREX_Attendance_Report.csv"
                : "AUREX_Attendance_Report.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Path output = chooser.getSelectedFile().toPath();
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                String[] row = new String[model.getColumnCount()];
                for (int j = 0; j < model.getColumnCount(); j++) {
                    row[j] = String.valueOf(model.getValueAt(i, j));
                }
                rows.add(row);
            }

            List<String> lines = new ArrayList<>();
            if (csvFormat) {
                lines.add("Date & Time,Person Type,Person ID,Person Name,Movement,Guard");
                for (String[] row : rows) {
                    List<String> cells = new ArrayList<>();
                    for (String value : row) cells.add(csvValue(value));
                    lines.add(String.join(",", cells));
                }
            } else {
                lines.add("AUREX SECURITY SYSTEM");
                lines.add("ATTENDANCE HISTORY REPORT");
                lines.add("Generated: " + now());
                lines.add("Records in this report: " + rows.size());
                lines.add("============================================================");
                for (String[] row : rows) {
                    lines.add("Date & Time : " + row[0]);
                    lines.add("Person Type : " + row[1]);
                    lines.add("Person ID   : " + row[2]);
                    lines.add("Person Name : " + row[3]);
                    lines.add("Movement    : " + row[4]);
                    lines.add("Guard       : " + row[5]);
                    lines.add("------------------------------------------------------------");
                }
            }
            Files.write(output, lines, StandardCharsets.UTF_8);
            log("ATTENDANCE REPORT EXPORTED", output.getFileName().toString());
            info("Attendance report exported successfully.");
        } catch (IOException ex) {
            showError("Could not export attendance report: " + ex.getMessage());
        }
    }

    // ================= STATISTICS CARDS =================

    private JLabel stat(JPanel parent, String title, String value, Color accent, String icon) {

        JPanel c = new JPanel(new BorderLayout(8, 8));

        c.setBackground(PANEL);
        c.setBorder(new CompoundBorder(
                new CompoundBorder(
                        new MatteBorder(4, 0, 0, 0, accent),
                        new LineBorder(new Color(40, 65, 100), 1, true)),
                new EmptyBorder(15, 17, 15, 17)));

        JLabel i = new JLabel(icon);
        i.setForeground(accent);
        i.setFont(new Font("Segoe UI Symbol", Font.BOLD, 30));

        JLabel v = new JLabel(value);
        v.setForeground(accent);
        v.setFont(head(34));

        JLabel t = new JLabel(title);
        t.setForeground(MUTED);
        t.setFont(bodyBold(10));

        JPanel a = new JPanel(new BorderLayout());
        a.setOpaque(false);
        a.add(i, BorderLayout.WEST);
        a.add(v, BorderLayout.EAST);

        c.add(a, BorderLayout.CENTER);
        c.add(t, BorderLayout.SOUTH);

        parent.add(c);

        return v;
    }

    // ================= ACTION CARDS =================

    private JPanel actionCard(String title, String desc, Color color, Runnable run) {

        JPanel c = new JPanel(new BorderLayout(5, 8));

        c.setBackground(PANEL);
        c.setBorder(new CompoundBorder(
                new CompoundBorder(
                        new MatteBorder(0, 4, 0, 0, color),
                        new LineBorder(new Color(38, 62, 96), 1, true)),
                new EmptyBorder(17, 18, 17, 18)));

        JLabel t = new JLabel(title);
        t.setForeground(color);
        t.setFont(head(19));

        JLabel d = new JLabel("<html><div style='width:260px;color:#91A4C2'>" + desc + "</div></html>");
        d.setFont(italic(12));

        c.add(t, BorderLayout.NORTH);
        c.add(d, BorderLayout.CENTER);

        NeonButton b = new NeonButton("Open Module  →", color);
        b.addActionListener(e -> run.run());

        c.add(b, BorderLayout.SOUTH);

        return c;
    }

    // ================= REFRESH DASHBOARD =================

    private void refreshStats() {

        if (empCount == null) {
            return;
        }

        empCount.setText("" + data.get("Employees").size());

        long today = data.get("Visitors").stream()
                .filter(r -> r.length > 7 && r[7].startsWith(LocalDate.now().toString()))
                .count();

        visitorCount.setText("" + today);

        long inside = data.get("Visitors").stream().filter(r -> r.length > 9 && r[9].equalsIgnoreCase("Inside")).count()
                + data.get("Employees").stream().filter(r -> r.length > 6 && r[6].equalsIgnoreCase("Inside")).count();

        insideCount.setText("" + inside);

        pendingCount.setText("" + pendingApprovalTotal());
        if (notificationBadge != null) {
            notificationBadge.setText("🔔  " + pendingApprovalTotal());
        }
    }

    // ================= MODULE PAGE =================

    private JPanel modulePage(String key) {

        Color accent = colorOf(key);

        JPanel p = pageBase(key,
                "Manage and maintain " + key.toLowerCase(Locale.ROOT) + " records.", accent);

        JPanel wrap = new JPanel(new BorderLayout(10, 12));
        wrap.setOpaque(false);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tools.setOpaque(false);

        NeonButton add = new NeonButton("＋ Add New", accent);

        add.addActionListener(e -> {

            if (key.equals("Employees")) {
                employeeForm();
            } else if (key.equals("Visitors")) {
                visitorForm();
            } else if (key.equals("Emergency Alerts")) {
                emergencyForm();
            } else {
                genericForm(key);
            }

            refreshTableData(key);
            refreshStats();
        });

        NeonButton refresh = new NeonButton("↻ Refresh", new Color(60, 85, 125));
        refresh.addActionListener(e -> refreshTableData(key));

        tools.add(add);
        tools.add(refresh);

        wrap.add(tools, BorderLayout.NORTH);

        JTable table = makeTable(key, accent);

        if (key.equals("Emergency Alerts")) {
            NeonButton resolve = new NeonButton("✓ Mark Selected as Resolved", GREEN);
            resolve.addActionListener(e -> {
                int selected = table.getSelectedRow();
                if (selected < 0) { info("Select an emergency alert first."); return; }
                int modelRow = table.convertRowIndexToModel(selected);
                String alertId = String.valueOf(table.getModel().getValueAt(modelRow, 0));
                String[] alert = find("Emergency Alerts", 0, alertId);
                if (alert != null) { alert[7] = "Resolved"; save("Emergency Alerts"); log("EMERGENCY RESOLVED", alertId); refreshTableData("Emergency Alerts"); }
            });
            tools.add(resolve);
        }

        wrap.add(scroll(table, accent), BorderLayout.CENTER);

        if (key.equals("Employees") || key.equals("Visitors")) {

            JPanel ops = new JPanel(new FlowLayout(FlowLayout.LEFT));
            ops.setOpaque(false);

            boolean emp = key.equals("Employees");

            NeonButton in = new NeonButton(emp ? "↗ Employee Check In" : "↗ Visitor Check In", GREEN);
            NeonButton out = new NeonButton(emp ? "↘ Employee Check Out" : "↘ Visitor Check Out", BLUE);

            in.addActionListener(e -> {
                if (emp) employeeMovement(true); else visitorMovement(true);
            });

            out.addActionListener(e -> {
                if (emp) employeeMovement(false); else visitorMovement(false);
            });

            ops.add(in);
            ops.add(out);

            wrap.add(ops, BorderLayout.SOUTH);
        }

        p.add(wrap, BorderLayout.CENTER);

        return p;
    }

    // ================= TABLE =================

    private JScrollPane scroll(JTable t, Color accent) {

        JScrollPane sp = new JScrollPane(t);

        sp.getViewport().setBackground(PANEL);
        sp.setBorder(new LineBorder(new Color(accent.getRed() / 2, accent.getGreen() / 2,
                accent.getBlue() / 2), 1, true));

        return sp;
    }

    private JTable makeTable(String key, Color accent) {

        DefaultTableModel m = new DefaultTableModel(schemas.get(key), 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (String[] r : data.get(key)) {
            m.addRow(r);
        }

        JTable t = new JTable(m);

        styleTable(t, accent);

        tables.computeIfAbsent(key, k -> new ArrayList<>()).add(t);

        return t;
    }

    private static Color statusColor(String s) {

        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "approved": case "active": case "completed":
            case "resolved": case "closed": case "found": case "on duty":
                return GREEN;
            case "pending": case "scheduled": case "open": case "investigating":
                return GOLD;
            case "inside":
                return BLUE;
            case "rejected": case "inactive": case "denied":
            case "expired": case "lost": case "blocked":
                return RED;
            default:
                return null;
        }
    }

    private void styleTable(JTable t, Color accent) {

        t.setRowHeight(34);
        t.setFont(body(13));
        t.setForeground(TEXT);
        t.setBackground(PANEL);
        t.setGridColor(new Color(30, 48, 76));
        t.setSelectionBackground(new Color(accent.getRed() / 2, accent.getGreen() / 2, accent.getBlue() / 2));
        t.setSelectionForeground(Color.WHITE);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Body cells: striped rows, mono first column, coloured status words
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean sel, boolean foc, int r, int c) {

                super.getTableCellRendererComponent(tb, v, sel, foc, r, c);

                String s = v == null ? "" : v.toString();
                Color sc = statusColor(s);

                setBorder(new EmptyBorder(0, 10, 0, 10));

                if (sel) {
                    setBackground(tb.getSelectionBackground());
                    setForeground(Color.WHITE);
                } else {
                    setBackground(r % 2 == 0 ? PANEL : new Color(19, 33, 57));
                    setForeground(sc != null ? sc : (c == 0 ? accent : TEXT));
                }

                setFont(sc != null ? bodyBold(13) : (c == 0 ? mono(12) : body(13)));

                return this;
            }
        });

        // Header: accent coloured
        JTableHeader header = t.getTableHeader();

        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v,
                    boolean sel, boolean foc, int r, int c) {

                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);

                l.setOpaque(true);
                l.setBackground(new Color(20, 36, 62));
                l.setForeground(accent);
                l.setFont(head(13));
                l.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 3, 1, accent.darker()),
                        new EmptyBorder(0, 10, 0, 10)));

                return l;
            }
        });

        for (int i = 0; i < t.getColumnModel().getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(140);
        }
    }

    private void refreshTableData(String key) {

        List<JTable> list = tables.get(key);

        if (list == null) {
            return;
        }

        for (JTable t : list) {

            DefaultTableModel m = (DefaultTableModel) t.getModel();

            m.setRowCount(0);

            for (String[] r : data.get(key)) {
                m.addRow(r);
            }
        }
    }

    // ================= GENERAL FORM =================

    private void genericForm(String key) {

        String[] fields = schemas.get(key);

        JTextField[] inputs = new JTextField[fields.length];

        JPanel form = new JPanel(new GridLayout(fields.length, 2, 8, 8));
        form.setBackground(PANEL);

        for (int i = 0; i < fields.length; i++) {
            form.add(label(fields[i], colorOf(key)));
            inputs[i] = field();
            form.add(inputs[i]);
        }

        int result = JOptionPane.showConfirmDialog(this, form, "Add " + key,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {

            String[] row = new String[fields.length];

            for (int i = 0; i < fields.length; i++) {
                row[i] = inputs[i].getText().trim();
            }

            if (row[0].isEmpty()) {
                showError(fields[0] + " is required.");
                return;
            }

            data.get(key).add(row);
            save(key);
            log("ADD " + key, row[0]);

            refreshStats();
            refreshTableData(key);

            info(key + " record added successfully.");
        }
    }

    // ================= EMPLOYEE DIRECTORY / REGISTRATION =================

    // Employees module is the institution's staff directory. Every employee
    // registered here becomes selectable in visitor/host forms.
    private String[] departmentOptions() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (data.containsKey("Departments")) {
            for (String[] d : data.get("Departments")) {
                if (d.length > 0 && !d[0].isBlank()) names.add(d[0]);
            }
        }
        if (names.isEmpty()) {
            names.add("Administration");
            names.add("Human Resources");
            names.add("IT Department");
            names.add("Accounts");
        }
        return names.toArray(new String[0]);
    }

    private String[] employeeOptions() {
        List<String> names = new ArrayList<>();
        for (String[] e : data.get("Employees")) {
            if (e.length >= 7 && !e[0].isBlank()) {
                names.add(e[0] + "  |  " + e[1] + "  |  " + e[2]);
            }
        }
        if (names.isEmpty()) names.add("No employees registered yet");
        return names.toArray(new String[0]);
    }

    private JComboBox<String> combo(String[] options) {
        JComboBox<String> c = new JComboBox<>(options);
        styleCombo(c); // Bold text in both the selected value and dropdown list.
        return c;
    }

    private void addFormRow(JPanel panel, String title, JComponent input, Color accent) {
        panel.add(label(title, accent));
        panel.add(input);
    }

    private void employeeForm() {
        JTextField empId = field();
        JTextField name = field();
        JComboBox<String> dept = combo(departmentOptions());
        JComboBox<String> designation = combo(new String[]{
                "Police Commissioner", "Additional Police Commissioner", "Joint Police Commissioner",
                "Deputy Police Commissioner", "Assistant Police Commissioner", "Deputy Commissioner",
                "Additional Deputy Commissioner", "Assistant Commissioner", "Unit Commander",
                "Inspector", "Traffic Inspector", "Sergeant", "Sub-Inspector", "Assistant Sub-Inspector",
                "Head Constable", "Constable", "Intelligence Officer", "Cyber Crime Officer",
                "Digital Forensics Analyst", "Investigation Officer", "Operations Officer",
                "Legal Affairs Officer", "Training Instructor", "IT Officer", "Network Administrator",
                "Accounts Officer", "Finance Officer", "Procurement Officer", "Store Officer",
                "Medical Officer", "Welfare Officer", "Radio Operator", "Driver", "Office Assistant", "Support Staff"});
        JTextField phone = field();
        JComboBox<String> status = combo(new String[]{"Active", "Inactive"});
        JComboBox<String> presence = combo(new String[]{"Outside"});
        presence.setEnabled(false); // Presence changes only through the Check In / Check Out controls.

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBackground(PANEL);
        addFormRow(form, "Employee ID", empId, colorOf("Employees"));
        addFormRow(form, "Full Name", name, colorOf("Employees"));
        addFormRow(form, "Department", dept, colorOf("Employees"));
        addFormRow(form, "Designation", designation, colorOf("Employees"));
        addFormRow(form, "Phone", phone, colorOf("Employees"));
        addFormRow(form, "Employment Status", status, colorOf("Employees"));
        addFormRow(form, "Current Presence", presence, colorOf("Employees"));

        int result = JOptionPane.showConfirmDialog(this, form, "Register Employee — Staff Directory",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (empId.getText().isBlank() || name.getText().isBlank()) {
            showError("Employee ID and Name are required.");
            return;
        }
        if (find("Employees", 0, empId.getText().trim()) != null) {
            showError("Employee ID already exists in the Employee Directory.");
            return;
        }

        String[] row = {empId.getText().trim(), name.getText().trim(),
                String.valueOf(dept.getSelectedItem()), String.valueOf(designation.getSelectedItem()),
                phone.getText().trim(), String.valueOf(status.getSelectedItem()),
                String.valueOf(presence.getSelectedItem())};
        data.get("Employees").add(row);
        save("Employees");
        log("EMPLOYEE REGISTERED", row[0] + " / " + row[1] + " / " + row[2]);
        refreshStats();
        refreshTableData("Employees");
        info("Employee added to the Staff Directory successfully. This person can now be selected as an internal host.");
    }

    // ================= VISITOR REGISTRATION =================

    private void visitorForm() {
        JTextField visitorId = field();
        visitorId.setText(id("VIS"));
        visitorId.setEditable(false);
        JTextField name = field();
        JTextField phone = field();
        JTextField nid = field();
        JTextField purpose = field();
        JComboBox<String> department = combo(departmentOptions());
        JComboBox<String> host = combo(employeeOptions());
        JTextField visitDate = field();
        visitDate.setText(LocalDate.now().toString());
        JTextField expectedExit = field();
        JComboBox<String> status = combo(new String[]{"Pending"});
        status.setEnabled(false); // Approval controls the status; registration cannot bypass it.
        JComboBox<String> personType = combo(new String[]{"External Visitor", "Existing Employee"});
        JComboBox<String> employeeDirectory = combo(employeeOptions());
        JLabel typeHint = new JLabel("External people must receive department approval.");
        typeHint.setForeground(GOLD);
        typeHint.setFont(italic(11));

        JPanel form = new JPanel(new GridLayout(0, 2, 9, 9));
        form.setBackground(PANEL);
        addFormRow(form, "Registration Type", personType, colorOf("Visitors"));
        addFormRow(form, "Select Existing Employee", employeeDirectory, colorOf("Visitors"));
        addFormRow(form, "Visitor ID", visitorId, colorOf("Visitors"));
        addFormRow(form, "Full Name", name, colorOf("Visitors"));
        addFormRow(form, "Phone", phone, colorOf("Visitors"));
        addFormRow(form, "NID / Passport", nid, colorOf("Visitors"));
        addFormRow(form, "Purpose of Visit", purpose, colorOf("Visitors"));
        addFormRow(form, "Department to Visit", department, colorOf("Visitors"));
        addFormRow(form, "Host / Employee", host, colorOf("Visitors"));
        addFormRow(form, "Visit Date", visitDate, colorOf("Visitors"));
        addFormRow(form, "Expected Exit", expectedExit, colorOf("Visitors"));
        addFormRow(form, "Approval Status", status, colorOf("Visitors"));
        form.add(new JLabel(" "));
        form.add(typeHint);

        Runnable fillEmployee = () -> {
            String selected = String.valueOf(employeeDirectory.getSelectedItem());
            if (selected.startsWith("No employees")) return;
            String employeeId = selected.split("\\s+\\|", 2)[0].trim();
            String[] employee = find("Employees", 0, employeeId);
            if (employee != null) {
                name.setText(employee[1]);
                phone.setText(employee[4]);
                department.setSelectedItem(employee[2]);
                purpose.setText("Internal staff record verification");
                typeHint.setText("This person already exists in the Employee Directory.");
                typeHint.setForeground(GREEN);
            }
        };
        employeeDirectory.addActionListener(e -> {
            if ("Existing Employee".equals(personType.getSelectedItem())) fillEmployee.run();
        });
        personType.addActionListener(e -> {
            boolean internal = "Existing Employee".equals(personType.getSelectedItem());
            employeeDirectory.setEnabled(internal);
            name.setEditable(!internal);
            phone.setEditable(!internal);
            nid.setEditable(!internal);
            purpose.setEditable(!internal);
            if (internal) {
                fillEmployee.run();
                typeHint.setText("Choose a person already listed in the Employee Directory.");
                typeHint.setForeground(GREEN);
            } else {
                name.setText(""); phone.setText(""); nid.setText(""); purpose.setText("");
                typeHint.setText("External people must receive department approval.");
                typeHint.setForeground(GOLD);
            }
        });
        employeeDirectory.setEnabled(false);

        int result = JOptionPane.showConfirmDialog(this, form, "Visitor Registration — Staff Verification",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if ("Existing Employee".equals(personType.getSelectedItem())) {
            String selected = String.valueOf(employeeDirectory.getSelectedItem());
            if (selected.startsWith("No employees")) {
                showError("There are no employees in the directory. Register staff first.");
                return;
            }
            String employeeId = selected.split("\\s+\\|", 2)[0].trim();
            String[] employee = find("Employees", 0, employeeId);
            if (employee == null) {
                showError("Employee not found in the Staff Directory.");
                return;
            }
            info("INTERNAL PERSON VERIFIED\nName: " + employee[1] + "\nEmployee ID: " + employee[0]
                    + "\nDepartment: " + employee[2] + "\nStatus: " + employee[5]
                    + "\n\nThis person is already registered as an employee. Use Employee Check In/Out instead of visitor registration.");
            return;
        }

        if (name.getText().isBlank() || purpose.getText().isBlank()) {
            showError("Visitor Name and Purpose are required.");
            return;
        }
        if (isBlacklisted(nid.getText())) {
            showError("ACCESS DENIED: This person is blacklisted.");
            return;
        }

        String hostValue = String.valueOf(host.getSelectedItem());
        if (hostValue.startsWith("No employees")) hostValue = "Not assigned";
        else hostValue = hostValue.split("\\s+\\|", 3).length > 1
                ? hostValue.split("\\s+\\|", 3)[1].trim() : hostValue;

        String[] row = {visitorId.getText(), name.getText().trim(), phone.getText().trim(),
                nid.getText().trim(), purpose.getText().trim(), String.valueOf(department.getSelectedItem()),
                hostValue, visitDate.getText().trim(), expectedExit.getText().trim(), "Pending", "-", "-"};
        data.get("Visitors").add(row);
        save("Visitors");
        log("VISITOR REGISTERED", row[0] + " / Pending approval");
        refreshStats();
        refreshTableData("Visitors");
        info("Visitor registered successfully. Status: Pending Approval.");
    }

    // ================= FORM DESIGN =================

    private JLabel label(String s, Color c) {

        JLabel l = new JLabel(s);
        l.setForeground(c);
        l.setFont(bodyBold(12));

        return l;
    }

    private JTextField field() {

        JTextField t = styledField(new JTextField(18));
        t.setFont(body(13));

        return t;
    }

    // ================= SEARCH HELPERS =================

    private String[] find(String key, int col, String value) {

        for (String[] r : data.get(key)) {
            if (r[col].equalsIgnoreCase(value)) {
                return r;
            }
        }

        return null;
    }

    private boolean isBlacklisted(String nid) {

        if (nid == null || nid.isBlank()) {
            return false;
        }

        for (String[] r : data.get("Blacklist")) {
            if (r[1].equalsIgnoreCase(nid.trim())) {
                return true;
            }
        }

        return false;
    }

    // ================= AUTHORIZATION PAGE =================

    private JPanel authorizationPage() {

        Color accent = colorOf("Authorization");

        JPanel p = pageBase("Visitor Authorization",
                "Only authorized department representatives can approve external visitors.", accent);

        JPanel w = new JPanel(new BorderLayout(10, 10));
        w.setOpaque(false);

        JTable t = makeTable("Visitors", accent);

        w.add(scroll(t, accent), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);

        NeonButton approve = new NeonButton("✓ Approve Selected", GREEN);
        NeonButton reject = new NeonButton("✕ Reject Selected", RED);
        NeonButton refresh = new NeonButton("↻ Refresh", new Color(60, 85, 125));

        approve.addActionListener(e -> decideVisitor(t, true));
        reject.addActionListener(e -> decideVisitor(t, false));
        refresh.addActionListener(e -> refreshTableData("Visitors"));

        actions.add(approve);
        actions.add(reject);
        actions.add(refresh);

        w.add(actions, BorderLayout.SOUTH);

        p.add(w, BorderLayout.CENTER);

        return p;
    }

    // ================= APPROVE / REJECT =================

    private void decideVisitor(JTable t, boolean approve) {

        if (role.equals("Guard")) {
            showError("Only Admin or Department users can approve or reject visitors.");
            return;
        }

        int row = t.getSelectedRow();

        if (row < 0) {
            info("Select a visitor first.");
            return;
        }

        row = t.convertRowIndexToModel(row);

        String vid = String.valueOf(t.getModel().getValueAt(row, 0));

        String[] v = find("Visitors", 0, vid);

        if (v == null) {
            return;
        }

        if (!v[9].equals("Pending")) {
            info("This visitor is not pending approval.");
            return;
        }

        if (approve) {

            if (find("Departments", 0, v[5]) == null) {
                showError("Department not found. Add the department and authorized person first.");
                return;
            }

            v[9] = "Approved";

        } else {
            v[9] = "Rejected";
        }

        save("Visitors");
        log(approve ? "VISITOR APPROVED" : "VISITOR REJECTED", vid + " / " + v[5]);

        refreshStats();
        refreshTableData("Visitors");

        info(approve ? "Visitor approved. Guard can now check in this visitor."
                : "Visitor request rejected.");
    }

    // ================= EMPLOYEE CHECK IN / OUT =================

    private void employeeMovement(boolean checkIn) {

        String empId = JOptionPane.showInputDialog(this, "Enter Employee ID:");

        if (empId == null || empId.isBlank()) {
            return;
        }

        String[] e = find("Employees", 0, empId.trim());

        if (e == null) {
            showError("Employee not found. External persons must register as visitors.");
            return;
        }

        if (!e[5].equalsIgnoreCase("Active")) {
            showError("Employee is inactive. Access denied.");
            return;
        }

        if (checkIn) {

            if (e[6].equals("Inside")) {
                info("Employee is already inside.");
                return;
            }

            e[6] = "Inside";
            log("EMPLOYEE CHECK IN", empId);

        } else {

            if (!e[6].equals("Inside")) {
                info("Employee is not currently inside.");
                return;
            }

            e[6] = "Outside";
            log("EMPLOYEE CHECK OUT", empId);
        }

        save("Employees");

        refreshStats();
        refreshTableData("Employees");

        info(checkIn ? "Employee entry recorded successfully."
                : "Employee exit recorded successfully.");
    }

    // ================= VISITOR CHECK IN / OUT =================

    private void visitorMovement(boolean checkIn) {

        String visitorId = JOptionPane.showInputDialog(this, "Enter Visitor ID:");

        if (visitorId == null || visitorId.isBlank()) {
            return;
        }

        String[] v = find("Visitors", 0, visitorId.trim());

        if (v == null) {
            showError("Visitor not found.");
            return;
        }

        if (isBlacklisted(v[3])) {
            showError("ACCESS DENIED: Blacklisted person.");
            return;
        }

        if (checkIn) {

            if (!v[9].equals("Approved")) {
                showError("ACCESS DENIED: Visitor must be approved first. Current status: " + v[9]);
                return;
            }

            v[9] = "Inside";
            v[10] = now();
            log("VISITOR CHECK IN", visitorId);

        } else {

            if (!v[9].equals("Inside")) {
                info("Visitor is not currently inside.");
                return;
            }

            v[9] = "Completed";
            v[11] = now();
            log("VISITOR CHECK OUT", visitorId);
        }

        save("Visitors");

        refreshStats();
        refreshTableData("Visitors");

        info(checkIn ? "Visitor checked in successfully." : "Visitor checked out successfully.");
    }

    // ================= REPORTS PAGE =================

    private JPanel reportsPage() {

        JPanel p = pageBase("Reports & Analytics",
                "Review totals and export system data as CSV files.", colorOf("Reports"));

        JPanel w = new JPanel(new GridLayout(0, 2, 12, 12));
        w.setOpaque(false);

        for (String k : schemas.keySet()) {
            w.add(actionCard("▦  " + k, "Records: " + data.get(k).size(),
                    colorOf(k), () -> exportCsv(k)));
        }

        JScrollPane sp = new JScrollPane(w);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        p.add(sp, BorderLayout.CENTER);

        return p;
    }

    // ================= CSV EXPORT =================

    private void exportCsv(String key) {

        JFileChooser fc = new JFileChooser();

        fc.setSelectedFile(new File(key.replace(" ", "_") + "_report.csv"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

            try (PrintWriter out = new PrintWriter(
                    Files.newBufferedWriter(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8))) {

                out.println(String.join(",", schemas.get(key)));

                for (String[] r : data.get(key)) {

                    List<String> q = new ArrayList<>();

                    for (String v : r) {
                        q.add("\"" + v.replace("\"", "\"\"") + "\"");
                    }

                    out.println(String.join(",", q));
                }

                log("REPORT EXPORTED", key);
                info("Report exported successfully.");

            } catch (IOException ex) {
                showError(ex.getMessage());
            }
        }
    }

    // ================= NAVIGATION BUTTON (coloured per module) =================

    // ================= SIDEBAR VECTOR-STYLE ICON =================
    // Uses the icon glyph already assigned to each menu item.
    // The icon is drawn separately from the button text for consistent alignment.
    private static class NavGlyphIcon implements Icon {

        private final String glyph;
        private final Color accent;

        NavGlyphIcon(String glyph, Color accent) {
            this.glyph = glyph;
            this.accent = accent;
        }

        @Override
        public int getIconWidth() {
            return 23;
        }

        @Override
        public int getIconHeight() {
            return 23;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Soft coloured icon background
            g2.setColor(new Color(accent.getRed(), accent.getGreen(),
                    accent.getBlue(), 25));
            g2.fillRoundRect(x + 1, y + 1, 21, 21, 8, 8);

            g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
            g2.setColor(accent);

            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (23 - fm.stringWidth(glyph)) / 2;
            int ty = y + (23 - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(glyph, tx, ty);

            g2.dispose();
        }
    }

    private static class NavButton extends JButton {

        private final Color accent;
        private boolean hover = false;
        private boolean selected = false;

        NavButton(String s, Color accent, String iconGlyph) {

            super(s);

            this.accent = accent;
            setIcon(new NavGlyphIcon(iconGlyph, accent));
            setIconTextGap(13);
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setVerticalTextPosition(SwingConstants.CENTER);

            setMaximumSize(new Dimension(210, 43));
            setPreferredSize(new Dimension(210, 43));
            setHorizontalAlignment(LEFT);
            setFont(bodyBold(12));

            // Text is a soft tint of the module colour
            setForeground(new Color(
                    (accent.getRed() + 255 * 2) / 3,
                    (accent.getGreen() + 255 * 2) / 3,
                    (accent.getBlue() + 255 * 2) / 3));

            setBorder(new EmptyBorder(0, 15, 0, 10));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        public void setActive(boolean s) {
            selected = s;
            setForeground(s ? Color.WHITE : new Color(
                    (accent.getRed() + 255 * 2) / 3,
                    (accent.getGreen() + 255 * 2) / 3,
                    (accent.getBlue() + 255 * 2) / 3));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (hover || selected) {

                int alpha = selected ? 110 : 55;

                g2.setPaint(new GradientPaint(0, 0,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha),
                        getWidth(), 0,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 8)));

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(accent);
                g2.fillRoundRect(0, 7, 4, getHeight() - 14, 5, 5);
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // ================= NEON BUTTON =================

    private static class NeonButton extends JButton {

        private final Color accent;
        private boolean hover = false;
        private boolean pressed = false;

        NeonButton(String text, Color accent) {

            super(text);

            this.accent = accent;

            setForeground(Color.WHITE);
            setFont(head(13));
            setBorder(new EmptyBorder(11, 18, 11, 18));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Color a = accent.darker();
            Color b = accent;

            if (!isEnabled()) {
                a = new Color(50, 60, 80);
                b = new Color(70, 80, 100);
            } else if (pressed) {
                a = accent.darker().darker();
                b = accent.darker();
            } else if (hover) {
                a = accent;
                b = accent.brighter();
            }

            g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
            g2.fillRoundRect(0, 0, w, h, 15, 15);

            if (hover && isEnabled()) {
                g2.setColor(new Color(255, 255, 255, 85));
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }

    // ================= MAIN METHOD =================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            new LoginFrame().setVisible(true);
        });
    }
}