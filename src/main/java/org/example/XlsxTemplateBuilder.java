package org.example;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

public class XlsxTemplateBuilder {

    public static void createTemplateFromPlaceholders(Set<String> placeholders, File outputFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template");

            Row headerRow = sheet.createRow(0);
            int col = 0;
            for (String placeholder : placeholders) {
                Cell cell = headerRow.createCell(col++);
                cell.setCellValue(placeholder);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
}
