package org.example.processors;

import org.apache.poi.xwpf.usermodel.*;
import org.example.interfaces.TemplateProcessor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocxProcessor implements TemplateProcessor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[(.+?)]");

    @Override
    public void process(File templateFile, List<Map<String, String>> tableData, File targetDir) throws IOException {
        if (!targetDir.exists()) Files.createDirectories(targetDir.toPath());

        for (int i = 0; i < tableData.size(); i++) {
            Map<String, String> row = tableData.get(i);

            try (FileInputStream fis = new FileInputStream(templateFile);
                 XWPFDocument document = new XWPFDocument(fis)) {

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
}
