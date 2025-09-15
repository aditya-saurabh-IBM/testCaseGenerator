package utils;

import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelReader {

    public static List<Map<String, String>> readTestDataFromExcel(String filePath, String sheetName) {
        List<Map<String, String>> testDataList = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            Workbook workbook = WorkbookFactory.create(fileInputStream);
            Sheet sheet = workbook.getSheet(sheetName);

            Row headerRow = sheet.getRow(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                Map<String, String> rowData = new HashMap<>();

                System.out.println(" count : " + 1);
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    Cell headerCell = headerRow.getCell(j);
                    Cell currentCell = currentRow.getCell(j);

                    String header = headerCell.getStringCellValue();
                    String value;

                    // Check the cell type
                    if (currentCell.getCellType() == CellType.NUMERIC) {
                        // Handle numeric values
                        double numericValue = currentCell.getNumericCellValue();
                        if (numericValue % 1 == 0) {
                            // If it's a whole number, convert to string without decimal part
                            value = String.valueOf((long) numericValue);
                        } else {
                            // If it's not a whole number, convert to string as before
                            value = String.valueOf(numericValue);
                        }
                    } else if (currentCell.getCellType() == CellType.STRING) {
                        // Handle string values
                        value = currentCell.getStringCellValue();
                    } else {
                        // Handle other cell types if needed
                        value = ""; // For example, set to an empty string
                    }

                    rowData.put(header, value);
                }
                System.out.println("rowData : " + rowData);
                testDataList.add(rowData);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Test data list : " + testDataList);
        return testDataList;
    }

    public static List<Map<String, String>> readTestDataFromExcel(String testDataCompletePath, String sheetName, String s) throws IOException {

        List<Map<String, String>> testDataList = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(testDataCompletePath)) {
            Workbook workbook = WorkbookFactory.create(fileInputStream);
            Sheet sheet = workbook.getSheet(sheetName);

            Row headerRow = sheet.getRow(0);

//            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row currentRow = sheet.getRow(Integer.parseInt(s));
            Map<String, String> rowData = new HashMap<>();

            System.out.println(" count : " + 1);
            for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                Cell headerCell = headerRow.getCell(j);
                Cell currentCell = currentRow.getCell(j);

                if (currentCell != null) {
                    String header = headerCell.getStringCellValue();
                    String value;

                    // Check the cell type
                    if (currentCell.getCellType() == CellType.NUMERIC) {
                        // Handle numeric values
                        double numericValue = currentCell.getNumericCellValue();
                        if (numericValue % 1 == 0) {
                            // If it's a whole number, convert to string without decimal part
                            value = String.valueOf((long) numericValue);
                        } else {
                            // If it's not a whole number, convert to string as before
                            value = String.valueOf(numericValue);
                        }
                    } else if (currentCell.getCellType() == CellType.STRING) {
                        // Handle string values
                        value = currentCell.getStringCellValue();
                    } else {
                        // Handle other cell types if needed
                        value = ""; // For example, set to an empty string
                    }

                    rowData.put(header, value);
                }
                System.out.println("rowData : " + rowData);
                testDataList.add(rowData);


            }
        }

        System.out.println("Test data list : " + testDataList);
        return testDataList;
    }
}
