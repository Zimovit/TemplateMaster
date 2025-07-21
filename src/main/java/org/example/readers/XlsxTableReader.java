package org.example.readers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.interfaces.TableReader;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class XlsxTableReader implements TableReader {
    @Override
    public List<Map<String, String>> read(File file) {
        List<Map<String, String>> result = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) return result;

            // заголовки
            List<String> headers = new ArrayList<>();
            Row headerRow = rowIterator.next();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }

            // строки
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowMap.put(headers.get(i), getCellValueAsString(cell));
                }
                result.add(rowMap);
            }

        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения XLSX-файла", e);
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                    yield sdf.format(date);
                }
                double value = cell.getNumericCellValue();
                yield (value == Math.floor(value)) ? Long.toString((long) value) : Double.toString(value);
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> getCellValueAsString(evaluateFormula(cell));
            default -> "";
        };
    }

    private Cell evaluateFormula(Cell cell) {
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        return evaluator.evaluateInCell(cell);
    }
}
