package org.example.unit.processors;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.example.processors.DocxProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class DocxProcessorTest {

    private DocxProcessor processor;
    private File templateFile;
    private File outputDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        processor = new DocxProcessor();
        templateFile = tempDir.resolve("test-template.docx").toFile();
        outputDir = tempDir.resolve("output").toFile();
        outputDir.mkdirs();

        // Создаем тестовый DOCX файл с плейсхолдерами
        createTestTemplate();
    }

    @Test
    @DisplayName("Должен извлекать плейсхолдеры из простого документа")
    void shouldExtractPlaceholdersFromSimpleDocument() throws IOException {
        // When
        Set<String> placeholders = processor.extractPlaceholders(templateFile);

        // Then
        assertThat(placeholders)
                .containsExactlyInAnyOrder("name", "date", "amount");
    }

    @Test
    @DisplayName("Должен обрабатывать пустой документ без ошибок")
    void shouldHandleEmptyDocumentWithoutErrors(@TempDir Path tempDir) throws IOException {
        // Given
        File emptyTemplate = createEmptyTemplate(tempDir);

        // When
        Set<String> placeholders = processor.extractPlaceholders(emptyTemplate);

        // Then
        assertThat(placeholders).isEmpty();
    }

    @Test
    @DisplayName("Должен генерировать документы для каждой строки данных")
    void shouldGenerateDocumentForEachDataRow() throws IOException {
        // Given
        List<Map<String, String>> testData = Arrays.asList(
                Map.of("name", "Иванов И.И.", "date", "01.01.2024", "amount", "1000"),
                Map.of("name", "Петров П.П.", "date", "02.01.2024", "amount", "2000"),
                Map.of("name", "Сидоров С.С.", "date", "03.01.2024", "amount", "3000")
        );

        // When
        processor.process(templateFile, testData, outputDir);

        // Then
        File[] generatedFiles = outputDir.listFiles((dir, name) -> name.endsWith(".docx"));
        assertThat(generatedFiles)
                .hasSize(3)
                .allSatisfy(file -> assertThat(file).exists().isFile());

        // Проверяем имена файлов
        List<String> fileNames = Arrays.stream(generatedFiles)
                .map(File::getName)
                .sorted()
                .toList();

        assertThat(fileNames)
                .containsExactly("document_1.docx", "document_2.docx", "document_3.docx");
    }

    @Test
    @DisplayName("Должен корректно заменять плейсхолдеры в документе")
    void shouldCorrectlyReplacePlaceholders() throws IOException {
        // Given
        List<Map<String, String>> testData = List.of(
                Map.of("name", "Тестовое Имя", "date", "15.05.2024", "amount", "5000")
        );

        // When
        processor.process(templateFile, testData, outputDir);

        // Then
        File generatedFile = new File(outputDir, "document_1.docx");
        assertThat(generatedFile).exists();

        // Проверяем содержимое документа
        try (XWPFDocument doc = new XWPFDocument(generatedFile.toURI().toURL().openStream())) {
            String documentText = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .reduce("", String::concat);

            assertThat(documentText)
                    .contains("Тестовое Имя")
                    .contains("15.05.2024")
                    .contains("5000")
                    .doesNotContain("[name]")
                    .doesNotContain("[date]")
                    .doesNotContain("[amount]");
        }
    }

    @Test
    @DisplayName("Должен обрабатывать недостающие значения как пустые строки")
    void shouldHandleMissingValuesAsEmptyStrings() throws IOException {
        // Given - данные без поля "amount"
        List<Map<String, String>> incompleteData = List.of(
                Map.of("name", "Неполное Имя", "date", "10.10.2024")
        );

        // When
        processor.process(templateFile, incompleteData, outputDir);

        // Then
        File generatedFile = new File(outputDir, "document_1.docx");
        assertThat(generatedFile).exists();

        try (XWPFDocument doc = new XWPFDocument(generatedFile.toURI().toURL().openStream())) {
            String documentText = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .reduce("", String::concat);

            assertThat(documentText)
                    .contains("Неполное Имя")
                    .contains("10.10.2024")
                    .doesNotContain("[amount]"); // плейсхолдер должен быть удален
        }
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для несуществующего файла")
    void shouldThrowExceptionForNonExistentFile() {
        // Given
        File nonExistentFile = new File("non-existent-file.docx");
        List<Map<String, String>> testData = List.of(Map.of("key", "value"));

        // When & Then
        assertThatThrownBy(() -> processor.extractPlaceholders(nonExistentFile))
                .isInstanceOf(IOException.class);

        assertThatThrownBy(() -> processor.process(nonExistentFile, testData, outputDir))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Должен создавать выходную директорию если она не существует")
    void shouldCreateOutputDirectoryIfNotExists(@TempDir Path tempDir) throws IOException {
        // Given
        File nonExistentOutputDir = tempDir.resolve("new-output-dir").toFile();
        assertThat(nonExistentOutputDir).doesNotExist();

        List<Map<String, String>> testData = List.of(
                Map.of("name", "Тест", "date", "01.01.2024", "amount", "100")
        );

        // When
        processor.process(templateFile, testData, nonExistentOutputDir);

        // Then
        assertThat(nonExistentOutputDir)
                .exists()
                .isDirectory();

        assertThat(new File(nonExistentOutputDir, "document_1.docx"))
                .exists()
                .isFile();
    }

    /**
     * Создает тестовый DOCX шаблон с плейсхолдерами
     */
    private void createTestTemplate() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(templateFile)) {

            XWPFParagraph paragraph1 = document.createParagraph();
            paragraph1.createRun().setText("Имя: [name]");

            XWPFParagraph paragraph2 = document.createParagraph();
            paragraph2.createRun().setText("Дата: [date]");

            XWPFParagraph paragraph3 = document.createParagraph();
            paragraph3.createRun().setText("Сумма: [amount] руб.");

            document.write(fos);
        }
    }

    /**
     * Создает пустой DOCX документ для тестирования
     */
    private File createEmptyTemplate(Path tempDir) throws IOException {
        File emptyFile = tempDir.resolve("empty-template.docx").toFile();

        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(emptyFile)) {
            document.write(fos);
        }

        return emptyFile;
    }
}
