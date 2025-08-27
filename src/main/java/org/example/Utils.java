package org.example;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.*;

public class Utils {

    /**
     * Собирает значения для плейсхолдеров от пользователя через GUI диалог
     *
     * @param placeholders множество найденных плейсхолдеров
     * @return карта с парами плейсхолдер-значение, или null если пользователь отменил ввод
     */
    public static Map<String, String> collectUserInputGUI(Set<String> placeholders) {
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

