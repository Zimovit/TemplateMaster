package org.example.processors;

import org.example.interfaces.TemplateProcessor;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.pkg.OdfFileDom;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import org.w3c.dom.*;

public class OdtProcessor implements TemplateProcessor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[(.+?)]");

    @Override
    public void process(File templateFile, List<Map<String, String>> tableData, File targetDir) throws IOException {
        if (!targetDir.exists()) targetDir.mkdirs();

        for (int i = 0; i < tableData.size(); i++) {
            Map<String, String> row = tableData.get(i);
            try {
                OdfTextDocument document = OdfTextDocument.loadDocument(templateFile);
                OdfFileDom contentDom = document.getContentDom();

                // Заменяем в параграфах
                NodeList paragraphs = contentDom.getElementsByTagName("text:p");
                for (int j = 0; j < paragraphs.getLength(); j++) {
                    Node p = paragraphs.item(j);
                    replaceInNode(p, row);
                }

                // Заменяем в ячейках таблиц
                NodeList cells = contentDom.getElementsByTagName("table:table-cell");
                for (int j = 0; j < cells.getLength(); j++) {
                    Node cell = cells.item(j);
                    NodeList children = cell.getChildNodes();
                    for (int k = 0; k < children.getLength(); k++) {
                        Node item = children.item(k);
                        if (item.getNodeType() == Node.ELEMENT_NODE && "text:p".equals(item.getNodeName())) {
                            replaceInNode(item, row);
                        }
                    }
                }

                File outputFile = new File(targetDir, "document_" + (i + 1) + ".odt");
                document.save(outputFile);

            } catch (Exception e) {
                throw new IOException("Ошибка обработки документа ODT", e);
            }
        }
    }

    private void replaceInNode(Node node, Map<String, String> data) {
        String text = node.getTextContent();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = data.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        node.setTextContent(result.toString());
    }

    @Override
    public Set<String> extractPlaceholders(File templateFile) throws IOException {
        Set<String> placeholders = new HashSet<>();
        try {
            OdfTextDocument document = OdfTextDocument.loadDocument(templateFile);
            OdfFileDom contentDom = document.getContentDom();

            // Параграфы
            NodeList paragraphs = contentDom.getElementsByTagName("text:p");
            for (int i = 0; i < paragraphs.getLength(); i++) {
                Node p = paragraphs.item(i);
                extractFromText(p.getTextContent(), placeholders);
            }

            // Ячейки таблиц
            NodeList cells = contentDom.getElementsByTagName("table:table-cell");
            for (int i = 0; i < cells.getLength(); i++) {
                Node cell = cells.item(i);
                NodeList children = cell.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node item = children.item(j);
                    if (item.getNodeType() == Node.ELEMENT_NODE && "text:p".equals(item.getNodeName())) {
                        extractFromText(item.getTextContent(), placeholders);
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Ошибка извлечения плейсхолдеров", e);
        }
        return placeholders;
    }

    private void extractFromText(String text, Set<String> placeholders) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
    }
}

