package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;


public class App extends Application {

    private static Stage primaryStage;
    private static Locale currentLocale = new Locale("ru");

    @Override
    public void start (Stage stage) throws IOException {
        primaryStage = stage;
        loadMainView();
    }

    public static void switchLanguage (Locale locale) throws IOException {
        currentLocale = locale;
        loadMainView();
    }

    private static void loadMainView () throws IOException {
        I18n.setLocale(currentLocale);
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/MainView.fxml"), I18n.getBundle());
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle(I18n.get("app.title"));
        primaryStage.getIcons().add(
                new Image(Objects.requireNonNull(App.class.getResourceAsStream("/icon.png")))
        );
        primaryStage.show();
    }

    public static Locale getCurrentLocale () {
        return currentLocale;
    }

    public static void main(String[] args) {
        launch(args);
    }

}

//TODO Запросив таблицу для генерации документов, приложение должно предлагать место для сохранения папки с документами, начиная с папки с таблицей