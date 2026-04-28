import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

public class ProfessorDashboard extends JFrame {

    private Professor prof;

    // Shared UI state
    private JComboBox<String>  semesterDropdown;
    private JComboBox<String>  courseDropdown;
    private Map<String, Integer> currentCourseMap; // courseName -> courseId

    // Marks panel
    private JTable            marksTable;
    private DefaultTableModel marksModel;
    private JTextField        midF, sessF, finalF;

    // Attendance panel
    private JTable            attTable;
    private DefaultTableModel attModel;
    private JTextField        dateField;

    // View attendance panel
    private JTable            attSummaryTable;
    private DefaultTableModel attSummaryModel;

    private CardLayout cardLayout;
    private JPanel     contentArea;

    public ProfessorDashboard(Professor prof) {
        this.prof = prof;
        setTitle("Professor Portal – " + prof.getName());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        buildHeader();
        buildSidebar();
        buildContentArea();

        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // HEADER
    // ─────────────────────────────────────────────────────────────
    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(22, 160, 133));
        header.setPreferredSize(new Dimension(100, 60));

        JLabel lbl = new JLabel("   Welcome, Prof. " + prof.getName());
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Arial", Font.BOLD, 18));
        header.add(lbl, BorderLayout.WEST);

        JButton logout = new JButton("Logout");
        logout.setBackground(new Color(192, 57, 43));
        logout.setForeground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.addActionListener(e -> { new LoginPage(); dispose(); });
        header.add(logout, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    // ─────────────────────────────────────────────────────────────
    // SIDEBAR
    // ─────────────────────────────────────────────────────────────
    private void buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        // ── Semester Dropdown ────────────────────────────────────
        JLabel semLbl = makeWhiteLabel("Semester:");
        ArrayList<Integer> sems = prof.getMySemesters();
        String[] semLabels = new String[sems.size()];
        for (int i = 0; i < sems.size(); i++) semLabels[i] = "Semester " + sems.get(i);

        semesterDropdown = new JComboBox<>(semLabels);
        semesterDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // ── Course Dropdown ──────────────────────────────────────
        JLabel crsLbl = makeWhiteLabel("Course:");
        courseDropdown = new JComboBox<>();
        courseDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // ── Nav Buttons ──────────────────────────────────────────
        JButton btnMarks   = makeSideBtn("📋  Marks");
        JButton btnAtt     = makeSideBtn("✅  Mark Attendance");
        JButton btnViewAtt = makeSideBtn("📊  View Attendance");

        sidebar.add(semLbl);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
        sidebar.add(semesterDropdown);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(crsLbl);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
        sidebar.add(courseDropdown);
        sidebar.add(Box.createRigidArea(new Dimension(0, 25)));
        sidebar.add(btnMarks);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnAtt);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnViewAtt);

        add(sidebar, BorderLayout.WEST);

        // ── Wire dropdown events ─────────────────────────────────
        semesterDropdown.addActionListener(e -> refreshCourseDropdown());
        courseDropdown.addActionListener(e -> refreshAllTables());

        // Initial load
        refreshCourseDropdown();

        // ── Button actions ───────────────────────────────────────
        btnMarks.addActionListener(e -> {
            refreshAllTables();
            cardLayout.show(contentArea, "MARKS");
        });
        btnAtt.addActionListener(e -> {
            refreshAttendanceTable();
            cardLayout.show(contentArea, "ATTENDANCE");
        });
        btnViewAtt.addActionListener(e -> {
            refreshAttendanceSummary();
            cardLayout.show(contentArea, "VIEW_ATTENDANCE");
        });
    }

    // ─────────────────────────────────────────────────────────────
    // CONTENT AREA (CardLayout)
    // ─────────────────────────────────────────────────────────────
    private void buildContentArea() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);

        contentArea.add(buildMarksPanel(),          "MARKS");
        contentArea.add(buildAttendancePanel(),     "ATTENDANCE");
        contentArea.add(buildViewAttendancePanel(), "VIEW_ATTENDANCE");

        add(contentArea, BorderLayout.CENTER);
    }

    // ── Marks Panel ──────────────────────────────────────────────
    private JPanel buildMarksPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Student Marks – Select a student and enter marks below"));

        marksModel = new DefaultTableModel(
                new String[]{"Student ID", "Name", "Mids", "Sessionals", "Finals"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        marksTable = new JTable(marksModel);
        marksTable.setRowHeight(30);
        marksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(marksTable), BorderLayout.CENTER);

        // Bottom: input + submit
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        midF  = new JTextField(6); sessF = new JTextField(6); finalF = new JTextField(6);
        JButton submit = new JButton("Update Marks");
        submit.setBackground(new Color(41, 128, 185));
        submit.setForeground(Color.WHITE);
        submit.setFocusPainted(false);

        inputRow.add(new JLabel("Mids:")); inputRow.add(midF);
        inputRow.add(new JLabel("Sessionals:")); inputRow.add(sessF);
        inputRow.add(new JLabel("Finals:")); inputRow.add(finalF);
        inputRow.add(submit);
        p.add(inputRow, BorderLayout.SOUTH);

        submit.addActionListener(e -> submitMarksAction());
        return p;
    }

    // ── Attendance Panel ─────────────────────────────────────────
    private JPanel buildAttendancePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                "Mark Attendance – check Present, uncheck for Absent, then Save"));

        // Table with a checkbox column
        attModel = new DefaultTableModel(
                new String[]{"Student ID", "Name", "Present"}, 0) {
            @Override public Class<?> getColumnClass(int c) {
                return c == 2 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int r, int c) { return c == 2; }
        };
        attTable = new JTable(attModel);
        attTable.setRowHeight(32);
        // Make checkbox column wider
        TableColumn col = attTable.getColumnModel().getColumn(2);
        col.setPreferredWidth(80);
        p.add(new JScrollPane(attTable), BorderLayout.CENTER);

        // Bottom: date + save
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        dateField = new JTextField(java.time.LocalDate.now().toString(), 12);
        JButton save = new JButton("Save Attendance");
        save.setBackground(new Color(39, 174, 96));
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);

        bottom.add(new JLabel("Date (YYYY-MM-DD):"));
        bottom.add(dateField);
        bottom.add(save);
        p.add(bottom, BorderLayout.SOUTH);

        save.addActionListener(e -> saveAttendanceAction());
        return p;
    }

    // ── View Attendance Summary Panel ────────────────────────────
    private JPanel buildViewAttendancePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Attendance Summary for Selected Course"));

        attSummaryModel = new DefaultTableModel(
                new String[]{"Student ID", "Name", "Present", "Total", "Percentage"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        attSummaryTable = new JTable(attSummaryModel);
        attSummaryTable.setRowHeight(30);
        p.add(new JScrollPane(attSummaryTable), BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshAttendanceSummary());
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bot.add(refresh);
        p.add(bot, BorderLayout.SOUTH);

        return p;
    }

    // ─────────────────────────────────────────────────────────────
    // DATA REFRESH HELPERS
    // ─────────────────────────────────────────────────────────────
    private int getSelectedSemester() {
        if (semesterDropdown.getSelectedItem() == null) return -1;
        String sel = semesterDropdown.getSelectedItem().toString();
        try { return Integer.parseInt(sel.replace("Semester ", "").trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private Integer getSelectedCourseId() {
        if (courseDropdown.getSelectedItem() == null || currentCourseMap == null) return null;
        String sel = courseDropdown.getSelectedItem().toString();
        return currentCourseMap.get(sel);
    }

    private void refreshCourseDropdown() {
        int sem = getSelectedSemester();
        currentCourseMap = prof.getCoursesForSemester(sem);
        courseDropdown.removeAllItems();
        for (String name : currentCourseMap.keySet()) {
            courseDropdown.addItem(name);
        }
        refreshAllTables();
    }

    private void refreshAllTables() {
        refreshMarksTable();
        refreshAttendanceSummary();
    }

    private void refreshMarksTable() {
        if (marksModel == null) return;
        marksModel.setRowCount(0);
        Integer cId = getSelectedCourseId();
        if (cId == null) return;
        DefaultTableModel fetched = prof.getEnrolledStudents(cId);
        for (int r = 0; r < fetched.getRowCount(); r++) {
            marksModel.addRow(new Object[]{
                    fetched.getValueAt(r, 0), fetched.getValueAt(r, 1),
                    fetched.getValueAt(r, 2), fetched.getValueAt(r, 3),
                    fetched.getValueAt(r, 4)
            });
        }
    }

    private void refreshAttendanceTable() {
        if (attModel == null) return;
        attModel.setRowCount(0);
        Integer cId = getSelectedCourseId();
        if (cId == null) return;
        String date = (dateField != null) ? dateField.getText().trim()
                                           : java.time.LocalDate.now().toString();
        DefaultTableModel fetched = prof.getStudentsForAttendance(cId, date);
        for (int r = 0; r < fetched.getRowCount(); r++) {
            String status = fetched.getValueAt(r, 2).toString();
            boolean present = status.equals("P");
            attModel.addRow(new Object[]{
                    fetched.getValueAt(r, 0),
                    fetched.getValueAt(r, 1),
                    present
            });
        }
    }

    private void refreshAttendanceSummary() {
        if (attSummaryModel == null) return;
        attSummaryModel.setRowCount(0);
        Integer cId = getSelectedCourseId();
        if (cId == null) return;
        DefaultTableModel fetched = prof.getAttendanceSummary(cId);
        for (int r = 0; r < fetched.getRowCount(); r++) {
            attSummaryModel.addRow(new Object[]{
                    fetched.getValueAt(r, 0), fetched.getValueAt(r, 1),
                    fetched.getValueAt(r, 2), fetched.getValueAt(r, 3),
                    fetched.getValueAt(r, 4)
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ACTION HANDLERS
    // ─────────────────────────────────────────────────────────────
    private void submitMarksAction() {
        int row = marksTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student from the table first.");
            return;
        }
        Integer cId = getSelectedCourseId();
        if (cId == null) { JOptionPane.showMessageDialog(this, "Select a course first."); return; }

        try {
            int    sId  = (int) marksModel.getValueAt(row, 0);
            double mid  = Double.parseDouble(midF.getText().trim());
            double sess = Double.parseDouble(sessF.getText().trim());
            double fin  = Double.parseDouble(finalF.getText().trim());

            if (mid < 0 || mid > 30 || sess < 0 || sess > 20 || fin < 0 || fin > 50) {
                JOptionPane.showMessageDialog(this,
                        "Marks out of range!\nMids: 0-30 | Sessionals: 0-20 | Finals: 0-50");
                return;
            }

            prof.submitMarks(sId, cId, mid, sess, fin);
            refreshMarksTable();
            JOptionPane.showMessageDialog(this, "Marks updated successfully!");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric marks.");
        }
    }

    private void saveAttendanceAction() {
        Integer cId  = getSelectedCourseId();
        String  date = dateField.getText().trim();

        if (cId == null) { JOptionPane.showMessageDialog(this, "Select a course first."); return; }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Date format must be YYYY-MM-DD."); return;
        }
        if (attModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No students to save attendance for."); return;
        }

        int[] studentIds = new int[attModel.getRowCount()];
        String[] statuses = new String[attModel.getRowCount()];
        for (int r = 0; r < attModel.getRowCount(); r++) {
            studentIds[r] = (int) attModel.getValueAt(r, 0);
            statuses[r]   = (Boolean) attModel.getValueAt(r, 2) ? "P" : "A";
        }

        prof.saveAttendanceBatch(cId, studentIds, statuses, date);
        JOptionPane.showMessageDialog(this,
                "Attendance saved for " + studentIds.length + " students on " + date + ".");
        refreshAttendanceSummary();
    }

    // ─────────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────────
    private JLabel makeWhiteLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, 13));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton makeSideBtn(String text) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setBackground(new Color(52, 73, 94));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("Arial", Font.PLAIN, 14));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        return b;
    }
}
