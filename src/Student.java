import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

public class Student extends User {

    private int     currentSemester;
    private boolean frozen;

    public Student(int id, String name, int currentSemester, boolean frozen) {
        super(id, name);
        this.currentSemester = currentSemester;
        this.frozen          = frozen;
    }

    // Legacy constructor (for backward compat)
    public Student(int id, String name) { this(id, name, 1, false); }

    public int     getCurrentSemester() { return currentSemester; }
    public boolean isFrozen()           { return frozen; }

    // -------------------------------------------------------
    // Attendance report for this student
    // -------------------------------------------------------
    public DefaultTableModel getAttendanceReport() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Subject", "Present", "Total", "Percentage"}, 0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.course_name, " +
                    "SUM(CASE WHEN a.status='P' THEN 1 ELSE 0 END) AS present_days, " +
                    "COUNT(a.a_id) AS total_days " +
                    "FROM Enrollments e " +
                    "JOIN Courses c ON e.course_id = c.c_id " +
                    "LEFT JOIN Attendance a ON a.student_id = ? AND a.course_id = e.course_id " +
                    "WHERE e.student_id = ? AND e.withdrawn = 0 " +
                    "GROUP BY c.c_id, c.course_name");
            ps.setInt(1, this.id);
            ps.setInt(2, this.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int present = rs.getInt("present_days");
                int total   = rs.getInt("total_days");
                double pct  = (total > 0) ? (present * 100.0 / total) : 0.0;
                model.addRow(new Object[]{
                        rs.getString("course_name"), present, total,
                        String.format("%.2f%%", pct)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return model;
    }

    // -------------------------------------------------------
    // Marks report
    // -------------------------------------------------------
    public DefaultTableModel getMarksReport() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Subject", "Mids", "Sessionals", "Finals", "Total"}, 0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.course_name, " +
                    "COALESCE(m.mids,0) AS mids, " +
                    "COALESCE(m.sessionals,0) AS sessionals, " +
                    "COALESCE(m.finals,0) AS finals " +
                    "FROM Enrollments e " +
                    "JOIN Courses c ON e.course_id = c.c_id " +
                    "LEFT JOIN Marks m ON m.student_id = ? AND m.course_id = e.course_id " +
                    "WHERE e.student_id = ? AND e.withdrawn = 0");
            ps.setInt(1, this.id);
            ps.setInt(2, this.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double mid  = rs.getDouble("mids");
                double sess = rs.getDouble("sessionals");
                double fin  = rs.getDouble("finals");
                double tot  = mid + sess + fin;
                model.addRow(new Object[]{
                        rs.getString("course_name"),
                        mid, sess, fin, String.format("%.1f", tot)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return model;
    }

    // -------------------------------------------------------
    // Enrolled courses this student can withdraw from
    // Returns map: "Course Name" -> course_id
    // -------------------------------------------------------
    public Map<String, Integer> getWithdrawableCourses() {
        Map<String, Integer> map = new LinkedHashMap<>();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.c_id, c.course_name FROM Enrollments e " +
                    "JOIN Courses c ON e.course_id = c.c_id " +
                    "WHERE e.student_id = ? AND e.withdrawn = 0 AND e.semester = ?");
            ps.setInt(1, this.id);
            ps.setInt(2, this.currentSemester);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("course_name"), rs.getInt("c_id"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    // -------------------------------------------------------
    // Check if student has already withdrawn a course this semester
    // -------------------------------------------------------
    public boolean hasWithdrawnThisSemester() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Enrollments " +
                    "WHERE student_id = ? AND semester = ? AND withdrawn = 1");
            ps.setInt(1, this.id);
            ps.setInt(2, this.currentSemester);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // -------------------------------------------------------
    // Withdraw from a course (only 1 per semester allowed)
    // -------------------------------------------------------
    public boolean withdrawCourse(int courseId) {
        if (hasWithdrawnThisSemester()) return false;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Enrollments SET withdrawn = 1 " +
                    "WHERE student_id = ? AND course_id = ? AND semester = ?");
            ps.setInt(1, this.id);
            ps.setInt(2, courseId);
            ps.setInt(3, this.currentSemester);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // -------------------------------------------------------
    // Freeze / Unfreeze semester
    // -------------------------------------------------------
    public boolean toggleFreeze() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            int newVal = frozen ? 0 : 1;
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Users SET semester_frozen = ? WHERE u_id = ?");
            ps.setInt(1, newVal);
            ps.setInt(2, this.id);
            ps.executeUpdate();
            this.frozen = !this.frozen;
            return this.frozen;
        } catch (Exception e) { e.printStackTrace(); }
        return this.frozen;
    }
}
