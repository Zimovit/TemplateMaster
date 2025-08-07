package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;


public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/MainView.fxml")));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setStage(primaryStage);

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Генератор документов");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}

//TODO Запросив таблицу для генерации документов, приложение должно предлагать место для сохранения папки с документами, начиная с папки с таблицей
//TODO поиск вообще должен начинаться с рабочего стола
