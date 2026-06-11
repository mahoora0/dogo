package scratch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class ExtractCategories {
    public static void main(String[] args) {
        File gzFile = new File("seed/dogo_seed_recent15.sql.gz");
        if (!gzFile.exists()) {
            gzFile = new File("../seed/dogo_seed_recent15.sql.gz");
        }
        if (!gzFile.exists()) {
            System.out.println("GZ file not found.");
            return;
        }

        Set<String> lostCategories = new TreeSet<>();
        Set<String> foundCategories = new TreeSet<>();

        try (InputStream fileStream = new FileInputStream(gzFile);
             InputStream gzipStream = new GZIPInputStream(fileStream);
             Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(decoder)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("INSERT INTO LOST_ITEM") || line.contains("INSERT INTO lost_item")) {
                    extractCategoryMain(line, lostCategories);
                }
                if (line.contains("INSERT INTO FOUND_ITEM") || line.contains("INSERT INTO found_item")) {
                    extractCategoryMain(line, foundCategories);
                }
            }

            System.out.println("====== Lost Categories in SQL Seed ======");
            for (String cat : lostCategories) {
                System.out.println(cat);
            }
            System.out.println("\n====== Found Categories in SQL Seed ======");
            for (String cat : foundCategories) {
                System.out.println(cat);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extractCategoryMain(String line, Set<String> categories) {
        // A simple heuristic parser for INSERT statements.
        // e.g., INSERT INTO FOUND_ITEM (..., CATEGORY_MAIN, ...) VALUES (..., '지갑', ...)
        // Let's find single-quoted values and see if they match common categories or just extract all potential strings.
        // Better: let's print all values inserted in the CATEGORY_MAIN column if possible.
        // Let's just find values in the statement.
        // Since we know the schema:
        // LOST_ITEM columns: LOST_ID, USER_NO, SOURCE_TYPE, ATC_ID, TITLE, CONTENT, ITEM_NAME, CATEGORY_ID, CATEGORY_MAIN, CATEGORY_SUB, COLOR_NAME, LOST_AT, LOST_AREA, LOST_PLACE, CONTACT, STATUS, IS_DELETED, REGDATE, MODDATE
        // FOUND_ITEM columns: FOUND_ID, USER_NO, SOURCE_TYPE, ATC_ID, FD_SN, TITLE, CONTENT, ITEM_NAME, CATEGORY_ID, CATEGORY_MAIN, CATEGORY_SUB, COLOR_NAME, FOUND_AT, FOUND_AREA, FOUND_PLACE, KEEP_PLACE, CONTACT, CUSTODY_STATUS, RECEIVE_TYPE, MANAGE_NO, SERIAL_NO, MODEL_CODE, IMEI, OWNER_NAME, STATUS, IS_DELETED, REGDATE, MODDATE
        
        // Let's do a simple regex or string split to extract quoted terms.
        int valIndex = line.indexOf("VALUES");
        if (valIndex == -1) valIndex = line.indexOf("values");
        if (valIndex == -1) return;
        
        String valuesPart = line.substring(valIndex);
        // Extract all single-quoted strings
        int start = -1;
        List<String> tokens = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valuesPart.length(); i++) {
            char c = valuesPart.charAt(i);
            if (c == '\'') {
                if (inQuote) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                    inQuote = false;
                } else {
                    inQuote = true;
                }
            } else if (inQuote) {
                sb.append(c);
            }
        }
        
        // For FOUND_ITEM, columns in seed insert:
        // Let's look at the columns in the insert statement:
        // INSERT INTO FOUND_ITEM (..., CATEGORY_MAIN, ...)
        // Let's find the position of CATEGORY_MAIN (or category_main) in the columns list.
        int colStart = line.indexOf('(');
        int colEnd = line.indexOf(')', colStart);
        if (colStart != -1 && colEnd != -1) {
            String colsStr = line.substring(colStart + 1, colEnd);
            String[] cols = colsStr.split(",");
            int catMainIdx = -1;
            for (int i = 0; i < cols.length; i++) {
                String col = cols[i].trim().toUpperCase();
                if (col.equals("CATEGORY_MAIN")) {
                    catMainIdx = i;
                    break;
                }
            }
            if (catMainIdx != -1) {
                // Now parse values
                // Values can be nested in parentheses like: VALUES (val1, val2, ...), (val1, val2, ...)
                int currentPos = valIndex;
                while (true) {
                    int openParen = valuesPart.indexOf('(', currentPos - valIndex);
                    if (openParen == -1) break;
                    int closeParen = valuesPart.indexOf(')', openParen);
                    if (closeParen == -1) break;
                    String valsStr = valuesPart.substring(openParen + 1, closeParen);
                    String[] vals = splitValues(valsStr);
                    if (catMainIdx < vals.length) {
                        String val = vals[catMainIdx].trim();
                        if (val.startsWith("'") && val.endsWith("'")) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (!val.equalsIgnoreCase("NULL") && !val.isEmpty()) {
                            categories.add(val);
                        }
                    }
                    currentPos = valIndex + closeParen + 1;
                }
            }
        }
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
