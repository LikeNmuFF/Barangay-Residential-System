import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DBConnection {
    // JDBC URL, username, and password of MySQL server
    private static final String URL = "jdbc:mysql://localhost:3306/barangay_mini_system";
    private static final String USER = "root";   // CHANGE THIS TO YOUR DB USER
    private static final String PASSWORD = ""; // CHANGE THIS TO YOUR DB PASSWORD

    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            
            // Establish the connection
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // System.out.println("Database connection successful!"); // Optional: for testing
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "MySQL Driver not found. Add the Connector/J library.", "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Connection failed: Check URL, username, or password.\nError: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return conn;
    }
}