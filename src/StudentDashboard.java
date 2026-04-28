import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class StudentDashboard extends JFrame {

    private Student  student;
    private CardLayout cardLayout;
    private JPanel     contentPanel;
    private JLabel     frozenStatusLbl;

    public StudentDashboard(Student s) {
        this.student = s;
        setTitle("Student Portal – " + s.getName());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        buildHeader();
        buildSidebar();
        buildContent();

        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // HEADER
    // ─────────────────────────────────────────────────────────────
    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 152, 219));
        header.setPreferredSize(new Dimension(100, 70));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        JLabel welcome = new JLabel("SIBA PORTAL  |  Welcome, " + student.getName());
        welcome.setForeground(Color.WHITE);
        welcome.setFont(new Font("Arial", Font.BOLD, 20));

        frozenStatusLbl = new JLabel(getFrozenText());
        frozenStatusLbl.setForeground(student.isFrozen()
                ? new Color(255, 200, 100) : new Color(200, 255, 200));
        frozenStatusLbl.setFont(new Font("Arial", Font.PLAIN, 13));

        left.add(welcome);
        left.add(frozenStatusLbl);
        header.add(left, BorderLayout.WEST);

        JLabel semLbl = new JLabel("Semester: " + student.getCurrentSemester() + "   ");
        semLbl.setForeground(Color.WHITE);
        semLbl.setFont(new Font("Arial", Font.BOLD, 14));
        header.add(semLbl, BorderLayout.CENTER);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(new Color(192, 57, 43));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> { new LoginPage(); dispose(); });
        header.add(btnLogout, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    // ─────────────────────────────────────────────────────────────
    // SIDEBAR
    // ─────────────────────────────────────────────────────────────
    private void buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JButton btnAtt      = makeSideBtn("📋  Attendance");
        JButton btnMarks    = makeSideBtn("📊  Marks");
        JButton btnWithdraw = makeSideBtn("❌  Withdraw Course");
        JButton btnFreeze   = makeSideBtn(student.isFrozen()
                ? "🔓  Unfreeze Semester" : "🔒  Freeze Semester");

        sidebar.add(btnAtt);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnMarks);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnWithdraw);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnFreeze);

        add(sidebar, BorderLayout.WEST);

        // ── Actions ──────────────────────────────────────────────
        btnAtt.addActionListener(e -> {
            refreshAttendance();
            cardLayout.show(contentPanel, "ATTENDANCE");
        });
        btnMarks.addActionListener(e -> {
            refreshMarks();
            cardLayout.show(contentPanel, "MARKS");
        });
        btnWithdraw.addActionListener(e -> handleWithdraw());
        btnFreeze.addActionListener(e -> {
            handleFreeze(btnFreeze);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // CONTENT CARDS
    // ─────────────────────────────────────────────────────────────
    private JTable attTable, marksTable;

    private void buildContent() {
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // ── Attendance Card ───────────────────────────────────────
        JPanel attCard = new JPanel(new BorderLayout());
        attCard.setBorder(BorderFactory.createTitledBorder("Your Attendance Report"));
        attTable = new JTable();
        styleTable(attTable);
        attCard.add(new JScrollPane(attTable), BorderLayout.CENTER);

        // ── Marks Card ────────────────────────────────────────────
        JPanel marksCard = new JPanel(new BorderLayout());
        marksCard.setBorder(BorderFactory.createTitledBorder("Your Marks Report"));
        marksTable = new JTable();
        styleTable(marksTable);
        marksCard.add(new JScrollPane(marksTable), BorderLayout.CENTER);

        // ── Welcome Card ──────────────────────────────────────────
        JPanel welcomeCard = new JPanel(new GridBagLayout());
        welcomeCard.setBackground(new Color(236, 240, 241));
        JLabel lbl = new JLabel("Select an option from the sidebar");
        lbl.setFont(new Font("Arial", Font.PLAIN, 20));
        lbl.setForeground(new Color(127, 140, 141));
        welcomeCard.add(lbl);

        contentPanel.add(welcomeCard, "WELCOME");
        contentPanel.add(attCard,    "ATTENDANCE");
        contentPanel.add(marksCard,  "MARKS");

        add(contentPanel, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    // REFRESH HELPERS
    // ─────────────────────────────────────────────────────────────
    private void refreshAttendance() {
        attTable.setModel(student.getAttendanceReport());
        styleTable(attTable);
    }

    private void refreshMarks() {
        marksTable.setModel(student.getMarksReport());
        styleTable(marksTable);
    }

    // ─────────────────────────────────────────────────────────────
    // WITHDRAW COURSE
    // ─────────────────────────────────────────────────────────────
    private void handleWithdraw() {
        if (student.isFrozen()) {
            JOptionPane.showMessageDialog(this,
                    "Your semester is currently frozen. You cannot withdraw courses.");
            return;
        }

        if (student.hasWithdrawnThisSemester()) {
            JOptionPane.showMessageDialog(this,
                    "You have already withdrawn one course this semester.\n" +
                    "Only 1 withdrawal is allowed per semester.");
            return;
        }

        Map<String, Integer> courses = student.getWithdrawableCourses();
        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no enrolled courses to withdraw from.");
            return;
        }

        String[] courseNames = courses.keySet().toArray(new String[0]);
        JComboBox<String> picker = new JComboBox<>(courseNames);

        int opt = JOptionPane.showConfirmDialog(this,
                new Object[]{
                    "<html><b>⚠ Course Withdrawal</b><br>" +
                    "You can only withdraw 1 course per semester.<br>" +
                    "This action cannot be undone.<br><br>" +
                    "Select course to withdraw:</html>",
                    picker
                },
                "Withdraw Course", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt != JOptionPane.OK_OPTION) return;

        String selected  = picker.getSelectedItem().toString();
        int    courseId  = courses.get(selected);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you absolutely sure you want to withdraw from:\n\"" + selected + "\"?",
                "Confirm Withdrawal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        boolean success = student.withdrawCourse(courseId);
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "You have successfully withdrawn from \"" + selected + "\".\n" +
                    "This course will no longer appear in your reports.");
            refreshAttendance();
            refreshMarks();
        } else {
            JOptionPane.showMessageDialog(this, "Withdrawal failed. Please try again.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // FREEZE / UNFREEZE SEMESTER
    // ─────────────────────────────────────────────────────────────
    private void handleFreeze(JButton btn) {
        String action = student.isFrozen() ? "unfreeze" : "freeze";
        String msg    = student.isFrozen()
                ? "Unfreeze your semester?\nYou will be able to enroll and withdraw courses again."
                : "Freeze your semester?\nYou will not be able to withdraw courses while frozen.\n" +
                  "Your attendance and marks will still be recorded.";

        int confirm = JOptionPane.showConfirmDialog(this, msg,
                "Confirm " + action.substring(0, 1).toUpperCase() + action.substring(1),
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        boolean nowFrozen = student.toggleFreeze();
        btn.setText(nowFrozen ? "🔓  Unfreeze Semester" : "🔒  Freeze Semester");
        frozenStatusLbl.setText(getFrozenText());
        frozenStatusLbl.setForeground(nowFrozen
                ? new Color(255, 200, 100) : new Color(200, 255, 200));

        JOptionPane.showMessageDialog(this,
                "Semester " + (nowFrozen ? "frozen" : "unfrozen") + " successfully.");
    }

    // ─────────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────────
    private String getFrozenText() {
        return student.isFrozen()
                ? "🔒 Semester Frozen"
                : "🔓 Semester Active";
    }

    private JButton makeSideBtn(String text) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        b.setBackground(new Color(52, 73, 94));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("Arial", Font.PLAIN, 15));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMargin(new Insets(0, 15, 0, 0));
        return b;
    }

    private void styleTable(JTable t) {
        t.setRowHeight(35);
        t.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        t.setFont(new Font("Arial", Font.PLAIN, 14));
        t.setGridColor(new Color(189, 195, 199));
    }
}
