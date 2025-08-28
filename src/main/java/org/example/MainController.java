package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.factories.FileFactory;
import org.example.factories.TemplateProcessorFactory;
import org.example.interfaces.TemplateProcessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class MainController {

    @FXML
    private ListView<String> templateListView;

    private final ObservableList<String> templates = FXCollections.observableArrayList();

    private Stage stage;

    @FXML private ComboBox<String> languageComboBox;


    @FXML
    public void initialize() {
        templateListView.setItems(templates);
        loadTemplatesFromDisk();
        languageComboBox.setItems(FXCollections.observableArrayList("Русский", "English", "Italiano"));
        if (App.getCurrentLocale().getLanguage().equals("en")) {
            languageComboBox.getSelectionModel().select("English");
        } else if (App.getCurrentLocale().getLanguage().equals("it")) {
            languageComboBox.getSelectionModel().select("Italiano");
        } else {
            languageComboBox.getSelectionModel().select("Русский");
        }

        languageComboBox.setOnAction(e -> {
            try {
                if ("English".equals(languageComboBox.getValue())) {
                    App.switchLanguage(new Locale("en"));
                } else if ("Italiano".equals(languageComboBox.getValue())) {
                    App.switchLanguage(new Locale("it"));
                }else {
                    App.switchLanguage(new Locale("ru"));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))));
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
                alert(I18n.get("alert.errRemovingFile") + e.getMessage());
            }
        } else {
            alert(I18n.get("alert.templateNotChosen"));
        }
    }

    @FXML
    private void onGenerateDocuments() {
        String selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            File templateFile = TemplateManager.getTemplateDir().resolve(selected).toFile();
            DocumentGenerator.generateDocuments(stage, templateFile);
            alert(I18n.get("alert.documentGeneration") + selected);
        } else {
            alert(I18n.get("alert.templateNotChosen"));
        }
    }

    @FXML
    private void onCreateTableFromTemplate() {
        File outputDir = FileFactory.getDirectoryToSave(stage);
        if (outputDir == null) return;

        String templateName = templateListView.getSelectionModel().getSelectedItem();
        if (templateName == null || templateName.isBlank()) {
            alert(I18n.get("alert.templateNotChosen"));
            return;
        }

        File templateFile = TemplateManager.getTemplateDir().resolve(templateName).toFile();
        TemplateProcessor processor = TemplateProcessorFactory.fromFile(templateFile);
        Set<String> placeholders;
        try {
            placeholders = processor.extractPlaceholders(templateFile);
        } catch (IOException e) {
            alert(I18n.get("alert.cannotExtractHeadings"));
            e.printStackTrace();
            return;
        }

        String outputFileName = I18n.get("name.table") + templateName.replaceAll("\\.[^.]+$", "") + ".xlsx";
        File outputFile = new File(outputDir, outputFileName);
        try {
            XlsxTemplateBuilder.createTemplateFromPlaceholders(placeholders, outputFile);
        } catch (IOException e) {
            alert(I18n.get("alert.cannotCreateTable"));
            e.printStackTrace();
            return;
        }

        alert(I18n.get("alert.tableGenerated"));
    }

    @FXML
    private void onCreateSingleDocument() {
        String selected = templateListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            File templateFile = TemplateManager.getTemplateDir().resolve(selected).toFile();

            File targetFile = FileFactory.getFileToSave(stage);

            DocumentGenerator.generateSingleDocument(templateFile, targetFile);
            alert(I18n.get("alert.generatingDocuments")+ selected);
        } else {
            alert(I18n.get("alert.templateNotChosen"));
        }
    }

    @FXML
    private void onInstruction() {
        String lang = App.getCurrentLocale().getLanguage();
        String fileName = "/instruction_" + lang + ".txt";
        String content;
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            if (is == null) {
                alert(I18n.get("alert.instructionNotFound"));
                return;
            }
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            alert(I18n.get("alert.instructionReadingError")+ e.getMessage());
            return;
        }

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        Button closeButton = new Button(I18n.get("button.close"));
        closeButton.setOnAction(e -> ((Stage) closeButton.getScene().getWindow()).close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.setAlignment(Pos.BOTTOM_CENTER);

        VBox root = new VBox(textArea, buttonBox);
        root.setPadding(new Insets(10));
        root.setSpacing(10);

        Scene scene = new Scene(root, 500, 300);

        Stage instructionStage = new Stage();
        instructionStage.setTitle(I18n.get("button.instruction"));
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
            alert(I18n.get("alert.cannotLoadTemplates")+ e.getMessage());
        }
    }

    private void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("alert.title.information"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
