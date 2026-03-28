package Project._6.demo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixDb {
    public static void main(String[] args) {
        String url = "jdbc:sqlserver://localhost;databaseName=msdb;encrypt=true;trustServerCertificate=true;";
        String user = "sa";
        String pass = "Cha2558535585";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE Student_Community_Reply ALTER COLUMN StudentID_FK INT NULL");
                System.out.println("Altered StudentID_FK to NULL");
            } catch (Exception e) {
                System.out.println("Warn: " + e.getMessage());
            }
            try {
                stmt.execute("ALTER TABLE Student_Community_Reply ADD AdminName VARCHAR(100) NULL");
                System.out.println("Added AdminName column");
            } catch (Exception e) {
                System.out.println("Warn: " + e.getMessage());
            }
            System.out.println("Database fix completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
