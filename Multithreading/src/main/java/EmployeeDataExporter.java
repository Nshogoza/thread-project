import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class EmployeeDataExporter {
    private static final String QUERY = "SELECT id, name, position, salary FROM employees";
    private static final ReentrantLock lock = new ReentrantLock();
    private static final HikariDataSource dataSource = createDataSource();

    // Create HikariCP Connection Pool
    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/narasha");
        config.setUsername("postgres");
        config.setPassword("Narasha");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    // Export employee data to Excel file (Thread-safe)
    public static void exportToExcel(String fileName) {
        lock.lock(); // Prevent simultaneous writes to the file
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(QUERY);
             ResultSet rs = stmt.executeQuery();
             Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(fileName)) {

            Sheet sheet = workbook.createSheet("Employees");
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Name", "Position", "Salary"};
            for (int i = 0; i < columns.length; i++) {
                headerRow.createCell(i).setCellValue(columns[i]);
            }

            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rs.getInt("id"));
                row.createCell(1).setCellValue(rs.getString("name"));
                row.createCell(2).setCellValue(rs.getString("position"));
                row.createCell(3).setCellValue(rs.getDouble("salary"));
            }

            workbook.write(fileOut);
            System.out.println("Exported successfully to " + fileName);

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    // Simulating multiple export requests
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(5);


            String fileName = "EmployeeData_" + ".xlsx";
            executor.submit(() -> exportToExcel(fileName));


        executor.shutdown();
    }
}
