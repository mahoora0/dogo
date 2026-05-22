import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ClearPublicMissingData {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/dogo?serverTimezone=Asia/Seoul";
        String user = "root";
        String pass = "1234";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {
            
            int deletedRows = stmt.executeUpdate("DELETE FROM MISSING_PERSON_REPORT WHERE SOURCE_TYPE='PUBLIC_API'");
            System.out.println("Successfully deleted " + deletedRows + " rows from MISSING_PERSON_REPORT.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
