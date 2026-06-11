package scratch;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UpdateCategories {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/dogo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
        String user = "root";
        String password = "1234"; // from .env

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // Count '기타' in LOST_ITEM
            int lostCount = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM LOST_ITEM WHERE CATEGORY_MAIN = '기타'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lostCount = rs.getInt(1);
                    }
                }
            }

            // Count '기타' in FOUND_ITEM
            int foundCount = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM FOUND_ITEM WHERE CATEGORY_MAIN = '기타'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        foundCount = rs.getInt(1);
                    }
                }
            }

            System.out.println("Current '기타' count - Lost: " + lostCount + ", Found: " + foundCount);

            if (lostCount > 0 || foundCount > 0) {
                // Update '기타' to '기타물품'
                try (PreparedStatement ps = conn.prepareStatement("UPDATE LOST_ITEM SET CATEGORY_MAIN = '기타물품' WHERE CATEGORY_MAIN = '기타'")) {
                    int updated = ps.executeUpdate();
                    System.out.println("Updated " + updated + " rows in LOST_ITEM.");
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE FOUND_ITEM SET CATEGORY_MAIN = '기타물품' WHERE CATEGORY_MAIN = '기타'")) {
                    int updated = ps.executeUpdate();
                    System.out.println("Updated " + updated + " rows in FOUND_ITEM.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
