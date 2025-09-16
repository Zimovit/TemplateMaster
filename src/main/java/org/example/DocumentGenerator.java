package org.example;

import javafx.scene.control.Alert;
import javafx.stage.*;
import org.example.factories.FileFactory;
import org.example.factories.TableReaderFactory;
import org.example.factories.TemplateProcessorFactory;
import org.example.interfaces.TableReader;
import org.example.interfaces.TemplateProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DocumentGenerator {
    public static void generateDocuments(Stage stage, File template) {

        File table = FileFactory.getTableFile(stage);
        if (table == null) return;

        if (template == null) {
            alert(I18n.get("alert.templateNotChosen"));
            return;
        }

        File outputDir = FileFactory.getDirectoryToSave(stage, "fileChooser.title.save", table.getParentFile());
        File targetDir = new File(outputDir, "Generated_" + System.currentTimeMillis());
        if (!targetDir.mkdir()) {
            alert(I18n.get("alert.cannotCreateResultFolder"));
            return;
        }
        if (outputDir == null) return;

        // Загрузка таблицы
        try {
            TableReader tableReader = TableReaderFactory.fromFile(table);
            List<Map<String, String>> parsedTable = tableReader.read(table);

            TemplateProcessor templateProcessor = TemplateProcessorFactory.fromFile(template);
            templateProcessor.process(template, parsedTable, targetDir);
        } catch (Exception e) {
            alert(I18n.get("alert.cannotGenerateDocuments"));
        }

    }

    public static void generateSingleDocument(File templateFile, File targetFile){
        TemplateProcessor templateProcessor = TemplateProcessorFactory.fromFile(templateFile);
        try {
            templateProcessor.generateSingleDocument(templateFile, targetFile);
        } catch (IOException e) {
            alert(I18n.get("alert.cannotGenerateDocuments"));
        }
    }

    private static void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("alert.title.information"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
