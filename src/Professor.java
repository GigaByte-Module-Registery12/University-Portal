import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

public class Professor extends User {

    public Professor(int id, String name) { super(id, name); }

    // -------------------------------------------------------
    // Returns map:  "1 - Programming Fundamentals" -> c_id
    // for courses assigned to this professor in given semester
    // -------------------------------------------------------
    public Map<String, Integer> getCoursesForSemester(int semester) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c_id, course_name FROM Courses " +
                    "WHERE prof_id = ? AND offered_semester = ?");
            ps.setInt(1, this.id);
            ps.setInt(2, semester);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("course_name"), rs.getInt("c_id"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    // All semesters that have at least one course for this professor
    public ArrayList<Integer> getMySemesters() {
        ArrayList<Integer> sems = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT DISTINCT offered_semester FROM Courses " +
                    "WHERE prof_id = ? ORDER BY offered_semester");
            ps.setInt(1, this.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) sems.add(rs.getInt("offered_semester"));
        } catch (Exception e) { e.printStackTrace(); }
        return sems;
    }

    // Students enrolled in a specific course (not withdrawn)
    public DefaultTableModel getEnrolledStudents(int courseId) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Student ID", "Student Name", "Mids", "Sess", "Finals"}, 0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.u_id, u.name, " +
                    "COALESCE(m.mids,0) AS mids, " +
                    "COALESCE(m.sessionals,0) AS sessionals, " +
                    "COALESCE(m.finals,0) AS finals " +
                    "FROM Enrollments e " +
                    "JOIN Users u ON e.student_id = u.u_id " +
                    "LEFT JOIN Marks m ON m.student_id = u.u_id AND m.course_id = e.course_id " +
                    "WHERE e.course_id = ? AND e.withdrawn = 0");
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("u_id"), rs.getString("name"),
                        rs.getDouble("mids"), rs.getDouble("sessionals"), rs.getDouble("finals")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return model;
    }

    // Students enrolled in a specific course for attendance marking
    public DefaultTableModel getStudentsForAttendance(int courseId, String date) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Student ID", "Student Name", "Status"}, 0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.u_id, u.name, " +
                    "COALESCE((SELECT status FROM Attendance " +
                    "          WHERE student_id = u.u_id AND course_id = ? AND date = ?), '') AS status " +
                    "FROM Enrollments e " +
                    "JOIN Users u ON e.student_id = u.u_id " +
                    "WHERE e.course_id = ? AND e.withdrawn = 0");
            ps.setInt(1, courseId);
            ps.setString(2, date);
            ps.setInt(3, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("u_id"), rs.getString("name"), rs.getString("status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return model;
    }

    // View attendance summary for all students in a course
    public DefaultTableModel getAttendanceSummary(int courseId) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Student ID", "Name", "Present", "Total", "Percentage"}, 0);
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.u_id, u.name, " +
                    "SUM(CASE WHEN a.status='P' THEN 1 ELSE 0 END) AS present_days, " +
                    "COUNT(a.a_id) AS total_days " +
                    "FROM Enrollments e " +
                    "JOIN Users u ON e.student_id = u.u_id " +
                    "LEFT JOIN Attendance a ON a.student_id = u.u_id AND a.course_id = e.course_id " +
                    "WHERE e.course_id = ? AND e.withdrawn = 0 " +
                    "GROUP BY u.u_id, u.name");
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int present = rs.getInt("present_days");
                int total   = rs.getInt("total_days");
                double pct  = (total > 0) ? (present * 100.0 / total) : 0.0;
                model.addRow(new Object[]{
                        rs.getInt("u_id"), rs.getString("name"),
                        present, total, String.format("%.1f%%", pct)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return model;
    }

    // Save attendance for multiple students at once
    public void saveAttendanceBatch(int courseId, int[] studentIds, String[] statuses, String date) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "INSERT INTO Attendance (student_id, course_id, date, status) " +
                         "VALUES (?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE status = VALUES(status)";
            // We need a unique constraint on (student_id, course_id, date) for this to work
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < studentIds.length; i++) {
                ps.setInt(1, studentIds[i]);
                ps.setInt(2, courseId);
                ps.setString(3, date);
                ps.setString(4, statuses[i]);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Update marks for a student
    public void submitMarks(int studentID, int courseID, double mid, double sess, double fin) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "INSERT INTO Marks (student_id, course_id, mids, sessionals, finals) " +
                         "VALUES (?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE mids=?, sessionals=?, finals=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentID); ps.setInt(2, courseID);
            ps.setDouble(3, mid); ps.setDouble(4, sess); ps.setDouble(5, fin);
            ps.setDouble(6, mid); ps.setDouble(7, sess); ps.setDouble(8, fin);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
