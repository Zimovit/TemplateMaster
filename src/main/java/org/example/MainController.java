package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.factories.TemplateProcessorFactory;
import org.example.interfaces.TemplateProcessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public class MainController {

    @FXML
    private ListView<String> templateListView;

    private final ObservableList<String> templates = FXCollections.observableArrayList();

    private Stage stage;


    @FXML
    public void initialize() {
        templateListView.setItems(templates);
        loadTemplatesFromDisk();

    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void onAddTemplate() {
        TemplateManager.loadTemplate(stage);
        loadTemplatesFromDisk();
    }

    @FXML
    private void onDeleteTemplate() {
        String selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Path file = TemplateManager.getTemplateDir().resolve(selected);
            try {
                Files.deleteIfExists(file);
                templates.remove(selected);
            } catch (IOException e) {
                alert("Ошибка при удалении файла: " + e.getMessage());
            }
        } else {
            alert("Шаблон не выбран.");
        }
    }

    @FXML
    private void onGenerateDocuments() {
        String selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            File templateFile = TemplateManager.getTemplateDir().resolve(selected).toFile();
            DocumentGenerator.generateDocuments(stage, templateFile);
            alert("Генерация документов для: " + selected);
        } else {
            alert("Шаблон не выбран.");
        }
    }

    @FXML
    private void onCreateTableFromTemplate() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File outputDir = directoryChooser.showDialog(stage);
        if (outputDir == null) return;

        String templateName = templateListView.getSelectionModel().getSelectedItem();
        if (templateName == null || templateName.isBlank()) {
            alert("Шаблон не выбран.");
            return;
        }

        File templateFile = TemplateManager.getTemplateDir().resolve(templateName).toFile();
        TemplateProcessor processor = TemplateProcessorFactory.fromFile(templateFile);
        Set<String> placeholders;
        try {
            placeholders = processor.extractPlaceholders(templateFile);
        } catch (IOException e) {
            alert("Не удалось извлечь заголовки из шаблона.");
            e.printStackTrace();
            return;
        }

        String outputFileName = "Таблица_" + templateName.replaceAll("\\.[^.]+$", "") + ".docx";
        File outputFile = new File(outputDir, outputFileName);
        try {
            XlsxTemplateBuilder.createTemplateFromPlaceholders(placeholders, outputFile);
        } catch (IOException e) {
            alert("Не удалось создать таблицу по шаблону.");
            e.printStackTrace();
            return;
        }

        alert("Успешно сгенерирована таблица по шаблону.");
    }

    @FXML
    private void onInstruction() {
        String content;
        try (InputStream is = getClass().getResourceAsStream("/instruction.txt")) {
            if (is == null) {
                alert("Файл инструкции не найден.");
                return;
            }
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            alert("Ошибка при чтении инструкции: " + e.getMessage());
            return;
        }

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(Pos.BOTTOM_CENTER);

        VBox root = new VBox(textArea, buttonBox);
        root.setPadding(new Insets(10));
        root.setSpacing(10);

        Scene scene = new Scene(root, 500, 300);

        Stage instructionStage = new Stage();
        instructionStage.setTitle("Инструкция");
        instructionStage.setScene(scene);
        instructionStage.initModality(Modality.APPLICATION_MODAL);
        instructionStage.initOwner(templateListView.getScene().getWindow());
        instructionStage.showAndWait();
    }

    private void loadTemplatesFromDisk() {
        templates.clear();
        try (Stream<Path> files = Files.list(TemplateManager.getTemplateDir())) {
            files.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .forEach(templates::add);
        } catch (IOException e) {
            alert("Не удалось загрузить шаблоны: " + e.getMessage());
        }
    }

    private void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
