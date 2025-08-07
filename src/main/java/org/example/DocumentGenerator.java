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
            System.out.println("Шаблон или таблица не выбраны");
            return;
        }

        File outputDir = FileFactory.getDirectoryToSave(stage);
        File targetDir = new File(outputDir, "Generated_" + System.currentTimeMillis());
        if (!targetDir.mkdir()) {
            System.out.println("Не удалось создать папку для результатов");
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
            System.out.println(e.getMessage() + "--" + e.toString());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось сгенерировать документы");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

    }

    public static void generateSingleDocument(File templateFile, File targetFile){
        TemplateProcessor templateProcessor = TemplateProcessorFactory.fromFile(templateFile);
        try {
            templateProcessor.generateSingleDocument(templateFile, targetFile);
        } catch (IOException e) {
            System.out.println(e.getMessage() + "--" + e.toString());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось сгенерировать документ");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
