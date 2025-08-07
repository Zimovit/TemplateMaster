package org.example;

import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.example.factories.FileFactory;

public class TemplateManager {
    private static final Path templateDir = Paths.get(System.getProperty("user.home"), "LawyerHelper", "templates");

    static {
        try {
            Files.createDirectories(templateDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create template directory", e);
        }
    }

    public static void loadTemplate(Stage stage) {
        File selected = FileFactory.getDocumentFile(stage);
        if (selected != null) {
            Path target = templateDir.resolve(selected.getName());
            try {

                if (Files.exists(target)) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.initOwner(stage);
                    confirm.setTitle("Подтверждение замены");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Файл " + selected.getName() + " уже существует. Заменить?");

                    Optional<ButtonType> answer = confirm.showAndWait();
                    if (answer.isEmpty() || answer.get() != ButtonType.OK) {
                        return; // отмена копирования
                    }
                }

                Files.copy(selected.toPath(), target, StandardCopyOption.REPLACE_EXISTING);


                Alert alert = new Alert(AlertType.INFORMATION);
                alert.initOwner(stage);
                alert.setTitle("Успех");
                alert.setHeaderText(null);
                alert.setContentText("Шаблон успешно загружен: " + selected.getName());
                alert.showAndWait();

            } catch (IOException e) {
                throw new RuntimeException("Failed to save template", e);
            }
        }
    }

    public static File getTemplate(Stage stage) {
        List<File> templates;
        try (Stream<Path> files = Files.list(templateDir)) {
            templates = files
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .sorted(Comparator.comparing(File::getName))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list templates", e);
        }

        if (templates.isEmpty()) return null;

        ChoiceDialog<File> dialog = new ChoiceDialog<>(templates.get(0), templates);
        dialog.setTitle("Выбор шаблона");
        dialog.setHeaderText("Выберите шаблон из сохранённых");
        dialog.setContentText("Шаблон:");

        Optional<File> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static Path getTemplateDir() {
        return templateDir;
    }
}
