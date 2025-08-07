package org.example.factories;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class FileFactory {

    private static final String userDesktop = System.getProperty("user.home") + File.separator + "Desktop";
    private static final FileChooser.ExtensionFilter extensionFilterDocx = new FileChooser.ExtensionFilter("DOCX файлы (*.docx)", "*.docx");
    private static final FileChooser.ExtensionFilter extensionFilterTable = new FileChooser.ExtensionFilter("XLSX or ODS", "*.xlsx", "*.ods");

    private static final FileChooser.ExtensionFilter extensionFilterDocument = new FileChooser.ExtensionFilter("DOCX or ODT", "*.docx", "*.odt");

    private FileFactory () {}

    private static FileChooser getFileChooser (String title, File initialDir, FileChooser.ExtensionFilter filter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        // Фильтр по расширению
        fileChooser.getExtensionFilters().add(filter);
        fileChooser.setSelectedExtensionFilter(filter);

        //Стартовая папка
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        return fileChooser;
    }

    public static File getFileToSave (Stage stage, File initialDir) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить как...");

        // Фильтр по расширению DOCX
        fileChooser.getExtensionFilters().add(extensionFilterDocx);
        fileChooser.setSelectedExtensionFilter(extensionFilterDocx);

        //Стартовая папка
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        // Предлагаемое имя файла
        fileChooser.setInitialFileName("Сгенерированный документ.docx");

        File targetFile = fileChooser.showSaveDialog(stage);
        if (targetFile != null && !targetFile.getName().toLowerCase().matches(".*\\.docx$")) {
            targetFile = new File(targetFile.getParent(), targetFile.getName() + ".docx");
        }

        return targetFile;
    }

    public static File getFileToSave (Stage stage) {
        File saveDir = new File(userDesktop);
        return getFileToSave(stage, saveDir);
    }

    public static File getDirectoryToSave (Stage stage, String title, File initialDir) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(initialDir);
        return directoryChooser.showDialog(stage);
    }

    public static File getDirectoryToSave (Stage stage) {
        return getDirectoryToSave(stage, "Сохранить в...", new File(userDesktop));
    }

    public static File getTableFile (Stage stage, File initialDirectory) {
        String title = "Выберите таблицу с данными для заполнения шаблона.";
        return getFileChooser(title, initialDirectory, extensionFilterTable).showOpenDialog(stage);

    }

    public static File getTableFile (Stage stage) {
        File initialDirectory = new File(userDesktop);
        return getTableFile(stage, initialDirectory);
    }

    public static File getDocumentFile (Stage stage, File initialDirectory) {
        String title = "Выберите документ Word или OpenOffice";
        return getFileChooser(title, initialDirectory, extensionFilterDocument).showOpenDialog(stage);
    }

    public static File getDocumentFile (Stage stage) {
        File initialDirectory = new File(userDesktop);
        return getDocumentFile(stage, initialDirectory);
    }

}
