import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.LinkedHashMap;

public class AdminDashboard extends JFrame {

    private JTable            table;
    private DefaultTableModel model;
    private JButton           addUserBtn, editUserBtn, removeUserBtn,
            addCourseBtn, viewCoursesBtn, logoutBtn;

    public AdminDashboard() {
        setTitle("Admin Management System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        buildHeader();
        buildTable();
        buildButtonBar();

        refreshTable();
        wireListeners();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // UI BUILD
    // ─────────────────────────────────────────────────────────────
    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 39, 46));
        header.setPreferredSize(new Dimension(100, 65));

        JLabel title = new JLabel("   UNIVERSITY ADMINISTRATION CONTROL PANEL");
        title.setFont(new Font("Serif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(192, 57, 43));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        header.add(logoutBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void buildTable() {
        model = new DefaultTableModel(
                new String[]{"ID", "Name", "Username", "Role", "Semester", "Frozen"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setGridColor(new Color(220, 225, 230));

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder("All Users  — click a row, then use a button below"));
        add(sp, BorderLayout.CENTER);
    }

    private void buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 14));
        bar.setBackground(new Color(236, 240, 241));

        addUserBtn    = makeBtn("➕  Add User",      new Color(39, 174, 96));
        editUserBtn   = makeBtn("✏  Edit User",      new Color(41, 128, 185));
        removeUserBtn = makeBtn("🗑  Remove User",   new Color(192, 57, 43));
        addCourseBtn  = makeBtn("📚  Add Course",    new Color(142, 68, 173));
        viewCoursesBtn = makeBtn("📋  View Courses", new Color(22, 160, 133));

        bar.add(addUserBtn);
        bar.add(editUserBtn);
        bar.add(removeUserBtn);
        bar.add(addCourseBtn);
        bar.add(viewCoursesBtn);
        add(bar, BorderLayout.SOUTH);
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(165, 40));
        return b;
    }

    // ─────────────────────────────────────────────────────────────
    // LISTENERS
    // ─────────────────────────────────────────────────────────────
    private void wireListeners() {
        logoutBtn.addActionListener(e -> { new LoginPage(); dispose(); });
        addUserBtn.addActionListener(e -> addUser());
        editUserBtn.addActionListener(e -> editUser());
        removeUserBtn.addActionListener(e -> removeUser());
        addCourseBtn.addActionListener(e -> addCourse());
        viewCoursesBtn.addActionListener(e -> viewCourses());
    }

    // ─────────────────────────────────────────────────────────────
    // ADD USER
    // ─────────────────────────────────────────────────────────────
    private void addUser() {
        JTextField nameF = new JTextField();
        JTextField userF = new JTextField();
        JTextField passF = new JTextField();
        JComboBox<String>  roleBox = new JComboBox<>(new String[]{"Student","Professor","Admin"});
        JComboBox<Integer> semBox  = new JComboBox<>(new Integer[]{1,2,3,4,5,6,7,8});

        int opt = JOptionPane.showConfirmDialog(this,
                new Object[]{"Full Name:", nameF, "Username:", userF,
                        "Password:", passF, "Role:", roleBox, "Semester:", semBox},
                "Register New User", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        String nm = nameF.getText().trim();
        String un = userF.getText().trim();
        String pw = passF.getText().trim();
        if (nm.isEmpty() || un.isEmpty() || pw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required."); return;
        }
        try {
            PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO Users (name,username,password,role,current_semester) VALUES (?,?,?,?,?)");
            ps.setString(1, nm); ps.setString(2, un); ps.setString(3, pw);
            ps.setString(4, roleBox.getSelectedItem().toString());
            ps.setInt(5, (Integer) semBox.getSelectedItem());
            ps.executeUpdate();
            refreshTable();
            JOptionPane.showMessageDialog(this, "User added successfully!");
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this, "Error: Username already exists.");
        } catch (Exception ex) { err(ex); }
    }

    // ─────────────────────────────────────────────────────────────
    // EDIT USER
    // ─────────────────────────────────────────────────────────────
    private void editUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table first.");
            return;
        }

        int    uid      = (int)    model.getValueAt(row, 0);
        String curName  = (String) model.getValueAt(row, 1);
        String curUser  = (String) model.getValueAt(row, 2);
        String curRole  = (String) model.getValueAt(row, 3);
        int    curSem   = (int)    model.getValueAt(row, 4);

        // ── Tab 1: Basic Info ────────────────────────────────────
        JTextField nameF = new JTextField(curName);
        JTextField userF = new JTextField(curUser);
        JTextField passF = new JTextField();
        passF.setToolTipText("Leave blank to keep existing password");

        JComboBox<String>  roleBox = new JComboBox<>(new String[]{"Student","Professor","Admin"});
        roleBox.setSelectedItem(curRole);

        JComboBox<Integer> semBox = new JComboBox<>(new Integer[]{1,2,3,4,5,6,7,8});
        semBox.setSelectedItem(curSem);

        // ── Tab 2: Course Enrollment (students only) ─────────────
        JPanel coursePanel = buildCourseEnrollPanel(uid, curSem, roleBox);

        // ── Tabbed dialog ────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();

        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        infoPanel.add(new JLabel("Full Name:")); infoPanel.add(nameF);
        infoPanel.add(new JLabel("Username:")); infoPanel.add(userF);
        infoPanel.add(new JLabel("New Password:")); infoPanel.add(passF);
        infoPanel.add(new JLabel("(leave blank = unchanged)", SwingConstants.RIGHT));
        infoPanel.add(new JLabel(""));
        infoPanel.add(new JLabel("Role:")); infoPanel.add(roleBox);
        infoPanel.add(new JLabel("Semester:")); infoPanel.add(semBox);

        tabs.addTab("Basic Info", infoPanel);
        tabs.addTab("Course Enrollment", coursePanel);

        // Reload course panel when semester changes
        semBox.addActionListener(e -> {
            int newSem = (Integer) semBox.getSelectedItem();
            tabs.setComponentAt(1, buildCourseEnrollPanel(uid, newSem, roleBox));
        });

        int opt = JOptionPane.showConfirmDialog(this, tabs,
                "Edit User – ID: " + uid, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        // ── Save basic info ──────────────────────────────────────
        String nm = nameF.getText().trim();
        String un = userF.getText().trim();
        String pw = passF.getText().trim();
        int    sem = (Integer) semBox.getSelectedItem();
        String rol = roleBox.getSelectedItem().toString();

        if (nm.isEmpty() || un.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and username cannot be empty."); return;
        }

        try {
            if (pw.isEmpty()) {
                PreparedStatement ps = conn().prepareStatement(
                        "UPDATE Users SET name=?, username=?, role=?, current_semester=? WHERE u_id=?");
                ps.setString(1, nm); ps.setString(2, un);
                ps.setString(3, rol); ps.setInt(4, sem); ps.setInt(5, uid);
                ps.executeUpdate();
            } else {
                PreparedStatement ps = conn().prepareStatement(
                        "UPDATE Users SET name=?, username=?, password=?, role=?, current_semester=? WHERE u_id=?");
                ps.setString(1, nm); ps.setString(2, un); ps.setString(3, pw);
                ps.setString(4, rol); ps.setInt(5, sem); ps.setInt(6, uid);
                ps.executeUpdate();
            }

            // ── Save course enrollments ──────────────────────────
            if (rol.equalsIgnoreCase("Student")) {
                saveCourseEnrollments(uid, sem, coursePanel);
            }

            refreshTable();
            JOptionPane.showMessageDialog(this, "User updated successfully!");
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this, "Error: Username already taken by another user.");
        } catch (Exception ex) { err(ex); }
    }

    // Builds a scrollable checklist of courses for the given semester
    private JPanel buildCourseEnrollPanel(int uid, int semester, JComboBox<String> roleBox) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        String role = roleBox.getSelectedItem() != null ? roleBox.getSelectedItem().toString() : "";
        if (!role.equalsIgnoreCase("Student")) {
            JLabel note = new JLabel("Course enrollment is only applicable for Students.", SwingConstants.CENTER);
            note.setForeground(Color.GRAY);
            wrapper.add(note, BorderLayout.CENTER);
            return wrapper;
        }

        try {
            // Get all courses for this semester
            PreparedStatement all = conn().prepareStatement(
                    "SELECT c_id, course_name, " +
                            "(SELECT COUNT(*) FROM Enrollments e WHERE e.course_id = c.c_id " +
                            " AND e.student_id = ? AND e.withdrawn = 0) AS enrolled " +
                            "FROM Courses c WHERE c.offered_semester = ? ORDER BY course_name");
            all.setInt(1, uid); all.setInt(2, semester);
            ResultSet rs = all.executeQuery();

            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            boolean any = false;
            while (rs.next()) {
                any = true;
                int     cid     = rs.getInt("c_id");
                String  cname   = rs.getString("course_name");
                boolean checked = rs.getInt("enrolled") > 0;

                JCheckBox cb = new JCheckBox(cname, checked);
                cb.setFont(new Font("Arial", Font.PLAIN, 13));
                cb.setName(String.valueOf(cid));   // store course id in component name
                listPanel.add(cb);
                listPanel.add(Box.createRigidArea(new Dimension(0, 6)));
            }

            if (!any) {
                JLabel none = new JLabel("No courses found for Semester " + semester + ".",
                        SwingConstants.CENTER);
                none.setForeground(Color.GRAY);
                wrapper.add(none, BorderLayout.CENTER);
                return wrapper;
            }

            JScrollPane sp = new JScrollPane(listPanel);
            sp.setPreferredSize(new Dimension(400, 220));
            wrapper.add(new JLabel("  Check to enroll / Uncheck to withdraw:"), BorderLayout.NORTH);
            wrapper.add(sp, BorderLayout.CENTER);

        } catch (Exception ex) { err(ex); }
        return wrapper;
    }

    // Reads the checkboxes and syncs enrollments with the database
    private void saveCourseEnrollments(int uid, int semester, JPanel coursePanel) throws Exception {
        // Walk the component tree to find all JCheckBoxes
        JScrollPane sp = findScrollPane(coursePanel);
        if (sp == null) return;

        JPanel listPanel = (JPanel) sp.getViewport().getView();
        for (Component comp : listPanel.getComponents()) {
            if (!(comp instanceof JCheckBox)) continue;
            JCheckBox cb = (JCheckBox) comp;
            int courseId = Integer.parseInt(cb.getName());
            boolean shouldEnroll = cb.isSelected();

            // Check current enrollment state
            PreparedStatement check = conn().prepareStatement(
                    "SELECT enroll_id, withdrawn FROM Enrollments " +
                            "WHERE student_id=? AND course_id=?");
            check.setInt(1, uid); check.setInt(2, courseId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // Row exists — update withdrawn flag
                boolean currentlyWithdrawn = rs.getInt("withdrawn") == 1;
                if (shouldEnroll && currentlyWithdrawn) {
                    // Re-enroll
                    PreparedStatement up = conn().prepareStatement(
                            "UPDATE Enrollments SET withdrawn=0 WHERE student_id=? AND course_id=?");
                    up.setInt(1, uid); up.setInt(2, courseId); up.executeUpdate();
                } else if (!shouldEnroll && !currentlyWithdrawn) {
                    // Withdraw
                    PreparedStatement up = conn().prepareStatement(
                            "UPDATE Enrollments SET withdrawn=1 WHERE student_id=? AND course_id=?");
                    up.setInt(1, uid); up.setInt(2, courseId); up.executeUpdate();
                }
            } else if (shouldEnroll) {
                // No row yet — create enrollment
                PreparedStatement ins = conn().prepareStatement(
                        "INSERT INTO Enrollments (student_id, course_id, semester) VALUES (?,?,?)");
                ins.setInt(1, uid); ins.setInt(2, courseId); ins.setInt(3, semester);
                ins.executeUpdate();
            }
        }
    }

    // Recursively find the first JScrollPane inside a panel
    private JScrollPane findScrollPane(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JScrollPane) return (JScrollPane) c;
            if (c instanceof Container) {
                JScrollPane found = findScrollPane((Container) c);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // REMOVE USER
    // ─────────────────────────────────────────────────────────────
    private void removeUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table first.");
            return;
        }
        int    uid  = (int)    model.getValueAt(row, 0);
        String name = (String) model.getValueAt(row, 1);
        String role = (String) model.getValueAt(row, 3);

        if (role.equalsIgnoreCase("Admin")) {
            JOptionPane.showMessageDialog(this, "Admin accounts cannot be removed."); return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete user:\n" + name + " (ID: " + uid + ")?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            PreparedStatement ps = conn().prepareStatement("DELETE FROM Users WHERE u_id=?");
            ps.setInt(1, uid);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                refreshTable();
                JOptionPane.showMessageDialog(this, "User removed successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "User not found in database.");
            }
        } catch (Exception ex) { err(ex); }
    }

    // ─────────────────────────────────────────────────────────────
    // ADD COURSE
    // ─────────────────────────────────────────────────────────────
    private void addCourse() {
        JTextField cName = new JTextField();
        JComboBox<Integer> semBox = new JComboBox<>(new Integer[]{1,2,3,4,5,6,7,8});

        int opt = JOptionPane.showConfirmDialog(this,
                new Object[]{"Course Name:", cName, "Semester:", semBox},
                "Step 1: Course Details", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return;

        String courseName = cName.getText().trim();
        int    semester   = (Integer) semBox.getSelectedItem();
        if (courseName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Course name cannot be empty."); return;
        }

        try {
            LinkedHashMap<String, Integer> profMap = new LinkedHashMap<>();
            PreparedStatement ps = conn().prepareStatement(
                    "SELECT u_id, name FROM Users WHERE role='Professor' ORDER BY name");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                profMap.put(rs.getString("name") + " (ID:" + rs.getInt("u_id") + ")", rs.getInt("u_id"));

            if (profMap.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No professors found. Add a professor first."); return;
            }

            JComboBox<String> profBox = new JComboBox<>(profMap.keySet().toArray(new String[0]));
            int opt2 = JOptionPane.showConfirmDialog(this,
                    new Object[]{"Assign Professor:", profBox},
                    "Step 2: Assign Professor", JOptionPane.OK_CANCEL_OPTION);
            if (opt2 != JOptionPane.OK_OPTION) return;

            int profId = profMap.get(profBox.getSelectedItem().toString());
            PreparedStatement ins = conn().prepareStatement(
                    "INSERT INTO Courses (course_name, prof_id, offered_semester) VALUES (?,?,?)");
            ins.setString(1, courseName); ins.setInt(2, profId); ins.setInt(3, semester);
            ins.executeUpdate();
            JOptionPane.showMessageDialog(this, "Course added successfully!");

        } catch (Exception ex) { err(ex); }
    }

    // ─────────────────────────────────────────────────────────────
    // VIEW COURSES
    // ─────────────────────────────────────────────────────────────
    private void viewCourses() {
        try {
            ResultSet rs = conn().createStatement().executeQuery(
                    "SELECT c.c_id, c.course_name, u.name AS prof_name, c.offered_semester " +
                            "FROM Courses c LEFT JOIN Users u ON c.prof_id = u.u_id " +
                            "ORDER BY c.offered_semester, c.c_id");

            DefaultTableModel cm = new DefaultTableModel(
                    new String[]{"Course ID","Course Name","Professor","Semester"}, 0);
            while (rs.next())
                cm.addRow(new Object[]{rs.getInt("c_id"), rs.getString("course_name"),
                        rs.getString("prof_name"), rs.getInt("offered_semester")});

            JTable ct = new JTable(cm);
            ct.setRowHeight(28);
            ct.setFont(new Font("Arial", Font.PLAIN, 13));
            JScrollPane csp = new JScrollPane(ct);
            csp.setPreferredSize(new Dimension(720, 400));
            JOptionPane.showMessageDialog(this, csp, "All Courses", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception ex) { err(ex); }
    }

    // ─────────────────────────────────────────────────────────────
    // REFRESH TABLE
    // ─────────────────────────────────────────────────────────────
    public void refreshTable() {
        model.setRowCount(0);
        try {
            ResultSet rs = conn().createStatement().executeQuery(
                    "SELECT u_id, name, username, role, current_semester, semester_frozen FROM Users ORDER BY u_id");
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("u_id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getInt("current_semester"),
                        rs.getInt("semester_frozen") == 1 ? "Yes" : "No"
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────
    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }

    private void err(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
    }
}