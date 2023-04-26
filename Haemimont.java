/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package haemimont;

/**
 *
 * @author amb20
 */
import java.sql.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Haemimont {

    public static void main(String[] args) throws Exception {
        // Read input arguments

        // load the SQLite JDBC driver
        Class.forName("org.sqlite.JDBC");

        // specify the path to the SQLite .db file
        String url = "jdbc:sqlite:C:/Users/amb20/Desktop/mydatabase.db";

        args = new String[]{"10", "2000-11-23", "2011-11-23", "txt", "C:/reports"};

        try ( Connection conn = DriverManager.getConnection(url);) {
            List<String> pins = null;
            int minCredit = Integer.parseInt(args[0]);
            java.sql.Date startDate = java.sql.Date.valueOf(args[1]);
            java.sql.Date endDate = java.sql.Date.valueOf(args[2]);
            String outputFormat = args.length > 3 ? args[3] : null;
            String outputDir = args[4];

            if (args.length > 5) {
                pins = Arrays.asList(args[5].split(","));
            }

            // Execute SQL query to get the report data
            String sql = "SELECT s.name AS student_name, SUM(c.credit) AS total_credit, \n"
                    + "    group_concat(concat(c.name, ',', c.duration, ',', c.credit, ',', i.name), ';') AS courses\n"
                    + "    FROM courses_completed cc\n"
                    + "    JOIN students s ON cc.student_id = s.id\n"
                    + "    JOIN courses c ON cc.course_id = c.id\n"
                    + "    JOIN instructors i ON c.instructor_id = i.id\n"
                    + "    WHERE cc.completed_date BETWEEN ? AND ?\n"
                    + "    GROUP BY cc.student_id\n"
                    + "    HAVING total_credit >= ?";

            if (pins != null) {
                String pinPlaceholders = String.join(",", Collections.nCopies(pins.size(), "?"));
                sql += " AND s.pin IN (" + pinPlaceholders + ")";
            }

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDate(1, new java.sql.Date(startDate.getTime()));
            stmt.setDate(2, new java.sql.Date(endDate.getTime()));
            stmt.setInt(3, minCredit);

            if (pins != null) {
                for (int i = 0; i < pins.size(); i++) {
                    stmt.setString(i + 4, pins.get(i));
                }
            }

            ResultSet rs = stmt.executeQuery();

            // Generate reports
            while (rs.next()) {
                String studentName = rs.getString("student_name");
                int totalCredit = rs.getInt("total_credit");
                String coursesStr = rs.getString("courses");

                // Parse courses data and generate report lines
                List<String> lines = new ArrayList<>();
                for (String course : coursesStr.split(";")) {
                    String[] courseData = course.split(",");
                    String courseName = courseData[0];
                    int duration = Integer.parseInt(courseData[1]);
                    int credit = Integer.parseInt(courseData[2]);
                    String instructorName = courseData[3];

                    lines.add("\t" + courseName + ", " + duration + ", " + credit + ", " + instructorName);
                }

                // Generate report file
                String fileName = outputDir + "/report_" + studentName + "." + (outputFormat != null ? outputFormat : "html");
                FileWriter fw = new FileWriter(fileName);
                BufferedWriter bw = new BufferedWriter(fw);

                bw.write(studentName + ", " + totalCredit);
                bw.newLine();
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }

                bw.close();
            }

            rs.close();
            stmt.close();
            conn.close();
        }
    }

}
