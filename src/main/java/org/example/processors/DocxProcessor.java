package org.example.processors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.apache.poi.xwpf.usermodel.*;
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

                File outputFile = new File(targetDir, "document_" + (i + 1) + ".docx");
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
        Map<String, String> values = collectUserInputGUI(placeholders);

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

    /**
     * Собирает значения для плейсхолдеров от пользователя через GUI диалог
     *
     * @param placeholders множество найденных плейсхолдеров
     * @return карта с парами плейсхолдер-значение, или null если пользователь отменил ввод
     */
    private Map<String, String> collectUserInputGUI(Set<String> placeholders) {
        Map<String, String> values = new HashMap<>();

        // Сортируем плейсхолдеры для удобства
        List<String> sortedPlaceholders = new ArrayList<>(placeholders);
        Collections.sort(sortedPlaceholders);

        // Создаем диалог для ввода значений
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Заполнение шаблона");
        dialog.setHeaderText("Введите значения для найденных плейсхолдеров:");

        // Создаем кнопки
        ButtonType okButtonType = new ButtonType("ОК", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

        // Создаем содержимое диалога
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Map<String, TextField> textFields = new HashMap<>();

        for (int i = 0; i < sortedPlaceholders.size(); i++) {
            String placeholder = sortedPlaceholders.get(i);

            Label label = new Label("[" + placeholder + "]:");
            TextField textField = new TextField();
            textField.setPromptText("Введите значение...");

            grid.add(label, 0, i);
            grid.add(textField, 1, i);

            textFields.put(placeholder, textField);
        }

        dialog.getDialogPane().setContent(grid);

        // Фокус на первое поле
        if (!textFields.isEmpty()) {
            Platform.runLater(() -> textFields.values().iterator().next().requestFocus());
        }

        // Конвертер результата
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Map<String, String> result = new HashMap<>();
                for (Map.Entry<String, TextField> entry : textFields.entrySet()) {
                    String value = entry.getValue().getText();
                    result.put(entry.getKey(), value != null ? value : "");
                }
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        return result.orElse(null);
    }
}
