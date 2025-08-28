package org.example.processors;

import org.apache.poi.xwpf.usermodel.*;
import org.example.I18n;
import org.example.Utils;
import org.example.interfaces.TemplateProcessor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocxProcessor implements TemplateProcessor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[(.+?)]");

    @Override
    public void process(File templateFile, List<Map<String, String>> tableData, File targetDir) throws IOException {
        if (!targetDir.exists()) Files.createDirectories(targetDir.toPath());

        byte[] templateBytes = Files.readAllBytes(templateFile.toPath());

        for (int i = 0; i < tableData.size(); i++) {
            Map<String, String> row = tableData.get(i);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(templateBytes);
                 XWPFDocument document = new XWPFDocument(bais)) {

                replacePlaceholders(document, row);

                File outputFile = new File(targetDir, I18n.get("file.name.document") + (i + 1) + ".docx");
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    document.write(fos);
                }
            }
        }
    }

    private void replacePlaceholders(XWPFDocument document, Map<String, String> data) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceInParagraph(paragraph, data);
        }

        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceInParagraph(paragraph, data);
                    }
                }
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> data) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : runs) {
            if (run.getCTR().getTabList().size() > 0) {
                paragraphText.append('\t');
            }
            String text = run.getText(0);
            if (text != null) {
                paragraphText.append(text);
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(paragraphText.toString());
        StringBuffer replacedText = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = data.getOrDefault(key, "");
            matcher.appendReplacement(replacedText, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(replacedText);

        CTRPr formatting = null;
        XWPFRun referenceRun = runs.get(0);
        if (referenceRun.getCTR().isSetRPr()) {
            formatting = CTRPr.Factory.newInstance();
            formatting.set(referenceRun.getCTR().getRPr());
        }

        for (int i = runs.size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }

        String[] parts = replacedText.toString().split("(?<=\t)|(?=\t)");
        for (String part : parts) {
            if (part.equals("\t")) {
                paragraph.createRun().addTab();
            } else {
                XWPFRun newRun = paragraph.createRun();
                if (formatting != null) {
                    newRun.getCTR().setRPr(formatting);
                }
                newRun.setText(part);
            }
        }
    }

    public Set<String> extractPlaceholders(File templateFile) throws IOException {
        Set<String> placeholders = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(templateFile);
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                extractFromParagraph(paragraph, placeholders);
            }

            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            extractFromParagraph(paragraph, placeholders);
                        }
                    }
                }
            }
        }

        return placeholders;
    }

    private void extractFromParagraph(XWPFParagraph paragraph, Set<String> placeholders) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : runs) {
            if (run.getCTR().getTabList().size() > 0) {
                paragraphText.append('\t');
            }
            String text = run.getText(0);
            if (text != null) {
                paragraphText.append(text);
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(paragraphText.toString());
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
    }

    /**
     * Генерирует один документ на основе шаблона с интерактивным вводом значений для плейсхолдеров через GUI
     *
     * @param templateFile файл шаблона DOCX
     * @param targetFile целевой файл для сохранения результата
     * @throws IOException если произошла ошибка при работе с файлами
     */
    public void generateSingleDocument(File templateFile, File targetFile) throws IOException {
        // Извлекаем плейсхолдеры из шаблона
        Set<String> placeholders = extractPlaceholders(templateFile);

        if (placeholders.isEmpty()) {
            // Просто копируем исходный файл, если плейсхолдеры не найдены
            Files.copy(templateFile.toPath(), targetFile.toPath());
            return;
        }

        // Собираем значения от пользователя через GUI
        Map<String, String> values = Utils.collectUserInputGUI(placeholders);

        // Если пользователь отменил ввод, выходим
        if (values == null) {
            return;
        }

        // Создаем документ и заменяем плейсхолдеры
        try (FileInputStream fis = new FileInputStream(templateFile);
             XWPFDocument document = new XWPFDocument(fis)) {

            replacePlaceholders(document, values);

            // Создаем директорию для целевого файла, если она не существует
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                Files.createDirectories(parentDir.toPath());
            }

            // Сохраняем результат
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                document.write(fos);
            }
        }
    }

}
