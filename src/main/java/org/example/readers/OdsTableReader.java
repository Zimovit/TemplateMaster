package org.example.readers;

import org.example.interfaces.TableReader;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;

import java.io.File;
import java.util.*;

public class OdsTableReader implements TableReader {
    @Override
    public List<Map<String, String>> read(File file) {
        List<Map<String, String>> result = new ArrayList<>();
        try (OdfSpreadsheetDocument document = OdfSpreadsheetDocument.loadDocument(file)) {
            OdfTable table = document.getTableList().get(0); // первая таблица
            List<OdfTableRow> rows = table.getRowList();

            if (rows.isEmpty()) return result;

            // заголовки
            OdfTableRow headerRow = rows.get(0);
            int cellCount = headerRow.getCellCount();
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < cellCount; i++) {
                headers.add(headerRow.getCellByIndex(i).getDisplayText());
            }

            // строки
            for (int r = 1; r < rows.size(); r++) {
                OdfTableRow row = rows.get(r);
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < cellCount; c++) {
                    String header = headers.get(c);
                    String value = row.getCellByIndex(c).getDisplayText();
                    map.put(header, value);
                }
                result.add(map);
            }
        } catch (Exception e) {
            throw new RuntimeException("ODS reading error", e);
        }

        return result;
    }
}
