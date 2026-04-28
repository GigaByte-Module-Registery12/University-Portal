import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.sql.*;

public class LoginPage extends JFrame {

    private JTextField     userTxt  = new JTextField(20);
    private JPasswordField passTxt  = new JPasswordField(20);
    private JButton        loginBtn = new JButton("Login");

    public LoginPage() {
        setTitle("SIBA Student Management Portal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // ── Dark background fills the whole window ───────────────
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(30, 39, 46));
        setContentPane(bg);

        // ── White card ───────────────────────────────────────────
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(45, 55, 45, 55)));

        GridBagConstraints g = new GridBagConstraints();

        // ── Title block (spans 2 cols) ───────────────────────────
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        g.fill  = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 5, 0);

        JLabel icon = new JLabel("\uD83C\uDF93", SwingConstants.CENTER); // graduation cap
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
        card.add(icon, g);

        g.gridy = 1; g.insets = new Insets(0, 0, 3, 0);
        JLabel title = new JLabel("SIBA PORTAL", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(30, 39, 46));
        card.add(title, g);

        g.gridy = 2; g.insets = new Insets(0, 0, 28, 0);
        JLabel sub = new JLabel("Student Management System", SwingConstants.CENTER);
        sub.setFont(new Font("Arial", Font.PLAIN, 13));
        sub.setForeground(new Color(130, 140, 150));
        card.add(sub, g);

        g.gridy = 3; g.insets = new Insets(0, 0, 24, 0);
        card.add(new JSeparator(), g);

        // ── Username row ─────────────────────────────────────────
        g.gridwidth = 1; g.gridy = 4;
        g.gridx = 0; g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 0; g.insets = new Insets(0, 0, 8, 14);
        JLabel uLbl = new JLabel("Username");
        uLbl.setFont(new Font("Arial", Font.BOLD, 13));
        uLbl.setForeground(new Color(55, 65, 75));
        card.add(uLbl, g);

        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.CENTER; g.weightx = 1.0;
        g.insets = new Insets(0, 0, 8, 0);
        styleField(userTxt);
        card.add(userTxt, g);

        // ── Password row ─────────────────────────────────────────
        g.gridy = 5;
        g.gridx = 0; g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 0; g.insets = new Insets(0, 0, 28, 14);
        JLabel pLbl = new JLabel("Password");
        pLbl.setFont(new Font("Arial", Font.BOLD, 13));
        pLbl.setForeground(new Color(55, 65, 75));
        card.add(pLbl, g);

        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.CENTER; g.weightx = 1.0;
        g.insets = new Insets(0, 0, 28, 0);
        styleField(passTxt);
        card.add(passTxt, g);

        // ── Login button (full width) ─────────────────────────────
        g.gridx = 0; g.gridy = 6; g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        loginBtn.setBackground(new Color(52, 152, 219));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setOpaque(true);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 15));
        loginBtn.setPreferredSize(new Dimension(0, 44));
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(loginBtn, g);

        // ── Place card centred on background ─────────────────────
        bg.add(card);

        loginBtn.addActionListener(e -> handleLogin());
        getRootPane().setDefaultButton(loginBtn);
        setVisible(true);
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("Arial", Font.PLAIN, 14));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 210, 220), 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        f.setPreferredSize(new Dimension(250, 38));
    }

    private void handleLogin() {
        String user = userTxt.getText().trim();
        String pass = new String(passTxt.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both Username and Password.",
                    "Login Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM Users WHERE username=? AND password=?");
            ps.setString(1, user);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String  role   = rs.getString("role");
                int     id     = rs.getInt("u_id");
                String  name   = rs.getString("name");
                int     sem    = rs.getInt("current_semester");
                boolean frozen = rs.getInt("semester_frozen") == 1;

                if      (role.equalsIgnoreCase("Admin"))     new AdminDashboard();
                else if (role.equalsIgnoreCase("Professor")) new ProfessorDashboard(new Professor(id, name));
                else if (role.equalsIgnoreCase("Student"))   new StudentDashboard(new Student(id, name, sem, frozen));

                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Invalid Username or Password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database Error:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}