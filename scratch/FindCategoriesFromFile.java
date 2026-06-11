package scratch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;

public class FindCategoriesFromFile {
    public static void main(String[] args) {
        File gzFile = new File("seed/dogo_seed_recent15.sql.gz");
        if (!gzFile.exists()) {
            System.out.println("GZ file not found.");
            return;
        }

        Set<String> lostMains = new TreeSet<>();
        Set<String> foundMains = new TreeSet<>();
        Map<String, Set<String>> lostMap = new TreeMap<>();
        Map<String, Set<String>> foundMap = new TreeMap<>();

        // Match insert statements like:
        // INSERT INTO LOST_ITEM (...) VALUES (...);
        // Or multi-line values. Since it's a seed file, it's probably standard MySQL dump.
        // Let's search for "INSERT INTO `LOST_ITEM`" or "INSERT INTO `FOUND_ITEM`".
        try (InputStream fileStream = new FileInputStream(gzFile);
             InputStream gzipStream = new GZIPInputStream(fileStream);
             Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(decoder)) {

            String line;
            String currentTable = null;
            List<String> columns = new ArrayList<>();
            int catMainIdx = -1;
            int catSubIdx = -1;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("INSERT INTO")) {
                    currentTable = null;
                    columns.clear();
                    catMainIdx = -1;
                    catSubIdx = -1;

                    if (line.contains("`LOST_ITEM`") || line.contains(" LOST_ITEM ")) {
                        currentTable = "LOST_ITEM";
                    } else if (line.contains("`FOUND_ITEM`") || line.contains(" FOUND_ITEM ")) {
                        currentTable = "FOUND_ITEM";
                    }

                    if (currentTable != null) {
                        int colStart = line.indexOf('(');
                        int colEnd = line.indexOf(')', colStart);
                        if (colStart != -1 && colEnd != -1) {
                            String colsStr = line.substring(colStart + 1, colEnd);
                            String[] cols = colsStr.split(",");
                            for (int i = 0; i < cols.length; i++) {
                                String col = cols[i].replace("`", "").trim().toUpperCase();
                                columns.add(col);
                                if (col.equals("CATEGORY_MAIN")) {
                                    catMainIdx = i;
                                } else if (col.equals("CATEGORY_SUB")) {
                                    catSubIdx = i;
                                }
                            }
                        }
                    }
                }

                if (currentTable != null && (line.startsWith("(") || line.contains("VALUES") || line.contains("values"))) {
                    // Extract row values between parentheses
                    // Since it could be multi-row INSERT like: VALUES (row1), (row2), ...
                    // Let's parse each row inside parentheses.
                    int startIdx = 0;
                    while (true) {
                        int openP = line.indexOf('(', startIdx);
                        if (openP == -1) break;
                        int closeP = findClosingParenthesis(line, openP);
                        if (closeP == -1) break;

                        String valsStr = line.substring(openP + 1, closeP);
                        String[] vals = splitValues(valsStr);
                        if (catMainIdx != -1 && catMainIdx < vals.length) {
                            String main = cleanVal(vals[catMainIdx]);
                            String sub = (catSubIdx != -1 && catSubIdx < vals.length) ? cleanVal(vals[catSubIdx]) : null;

                            if (main != null) {
                                if (currentTable.equals("LOST_ITEM")) {
                                    lostMains.add(main);
                                    lostMap.computeIfAbsent(main, k -> new TreeSet<>()).add(sub != null ? sub : "");
                                } else {
                                    foundMains.add(main);
                                    foundMap.computeIfAbsent(main, k -> new TreeSet<>()).add(sub != null ? sub : "");
                                }
                            }
                        }
                        startIdx = closeP + 1;
                    }
                }
            }

            // Write results to a file using UTF-8
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream("scratch/categories_report.txt"), StandardCharsets.UTF_8))) {
                out.println("====== LOST_ITEM CATEGORIES ======");
                for (Map.Entry<String, Set<String>> entry : lostMap.entrySet()) {
                    out.println("- Main: " + entry.getKey());
                    for (String sub : entry.getValue()) {
                        if (!sub.isEmpty()) {
                            out.println("  * Sub: " + sub);
                        }
                    }
                }

                out.println("\n====== FOUND_ITEM CATEGORIES ======");
                for (Map.Entry<String, Set<String>> entry : foundMap.entrySet()) {
                    out.println("- Main: " + entry.getKey());
                    for (String sub : entry.getValue()) {
                        if (!sub.isEmpty()) {
                            out.println("  * Sub: " + sub);
                        }
                    }
                }
                System.out.println("Report written to scratch/categories_report.txt");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int findClosingParenthesis(String line, int openPos) {
        boolean inQuote = false;
        for (int i = openPos + 1; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'') {
                inQuote = !inQuote;
            } else if (c == ')' && !inQuote) {
                return i;
            }
        }
        return -1;
    }

    private static String cleanVal(String val) {
        val = val.trim();
        if (val.equalsIgnoreCase("NULL")) return null;
        if (val.startsWith("'") && val.endsWith("'")) {
            val = val.substring(1, val.length() - 1);
        }
        val = val.replace("''", "'").trim();
        return val.isEmpty() ? null : val;
    }

    private static String[] splitValues(String valsStr) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < valsStr.length(); i++) {
            char c = valsStr.charAt(i);
            if (c == '\'') {
                inQuote = !inQuote;
                sb.append(c);
            } else if (c == ',' && !inQuote) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }
}
