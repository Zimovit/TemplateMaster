package org.example.unit.readers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.readers.XlsxTableReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class XlsxTableReaderTest {

    private XlsxTableReader reader;
    private File testFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        reader = new XlsxTableReader();
        testFile = tempDir.resolve("test-data.xlsx").toFile();
        createTestXlsxFile();
    }

    @Test
    @DisplayName("Должен читать простые строковые данные")
    void shouldReadSimpleStringData() {
        // When
        List<Map<String, String>> data = reader.read(testFile);

        // Then
        assertThat(data).hasSize(2);

        Map<String, String> firstRow = data.get(0);
        assertThat(firstRow)
                .containsEntry("Имя", "Иванов Иван")
                .containsEntry("Должность", "Менеджер")
                .containsEntry("Зарплата", "50000");

        Map<String, String> secondRow = data.get(1);
        assertThat(secondRow)
                .containsEntry("Имя", "Петрова Анна")
                .containsEntry("Должность", "Бухгалтер")
                .containsEntry("Зарплата", "45000");
    }

    @Test
    @DisplayName("Должен правильно форматировать даты")
    void shouldFormatDatesCorrectly(@TempDir Path tempDir) throws IOException {
        // Given
        File dateFile = createFileWithDates(tempDir);

        // When
        List<Map<String, String>> data = reader.read(dateFile);

        // Then
        assertThat(data).hasSize(1);
        assertThat(data.get(0))
                .containsEntry("ФИО", "Сидоров С.С.")
                .containsEntry("Дата рождения", "15.03.1990");
    }

    @Test
    @DisplayName("Должен обрабатывать разные типы числовых данных")
    void shouldHandleDifferentNumericTypes(@TempDir Path tempDir) throws IOException {
        // Given
        File numericFile = createFileWithNumbers(tempDir);

        // When
        List<Map<String, String>> data = reader.read(numericFile);

        // Then
        assertThat(data).hasSize(1);
        Map<String, String> row = data.get(0);

        assertThat(row)
                .containsEntry("Целое", "100")
                .containsEntry("Дробное", "99.99")
                .containsEntry("Булево", "true");
    }

    @Test
    @DisplayName("Должен обрабатывать пустые ячейки")
    void shouldHandleEmptyCells(@TempDir Path tempDir) throws IOException {
        // Given
        File emptyFile = createFileWithEmptyCells(tempDir);

        // When
        List<Map<String, String>> data = reader.read(emptyFile);

        // Then
        assertThat(data).hasSize(2);

        Map<String, String> firstRow = data.get(0);
        assertThat(firstRow)
                .containsEntry("Колонка1", "Значение1")
                .containsEntry("Колонка2", "")
                .containsEntry("Колонка3", "Значение3");

        Map<String, String> secondRow = data.get(1);
        assertThat(secondRow)
                .containsEntry("Колонка1", "")
                .containsEntry("Колонка2", "Значение2")
                .containsEntry("Колонка3", "");
    }

    @Test
    @DisplayName("Должен возвращать пустой список для файла без данных")
    void shouldReturnEmptyListForFileWithoutData(@TempDir Path tempDir) throws IOException {
        // Given
        File emptyFile = createEmptyXlsxFile(tempDir);

        // When
        List<Map<String, String>> data = reader.read(emptyFile);

        // Then
        assertThat(data).isEmpty();
    }

    @Test
    @DisplayName("Должен обрабатывать файлы только с заголовками")
    void shouldHandleFileWithOnlyHeaders(@TempDir Path tempDir) throws IOException {
        // Given
        File headersOnlyFile = createFileWithOnlyHeaders(tempDir);

        // When
        List<Map<String, String>> data = reader.read(headersOnlyFile);

        // Then
        assertThat(data).isEmpty();
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для несуществующего файла")
    void shouldThrowExceptionForNonExistentFile() {
        // Given
        File nonExistentFile = new File("non-existent.xlsx");

        // When & Then
        assertThatThrownBy(() -> reader.read(nonExistentFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка чтения XLSX-файла");
    }

    /**
     * Создает тестовый XLSX файл с данными сотрудников
     */
    private void createTestXlsxFile() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(testFile)) {

            Sheet sheet = workbook.createSheet("Сотрудники");

            // Заголовки
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Имя");
            headerRow.createCell(1).setCellValue("Должность");
            headerRow.createCell(2).setCellValue("Зарплата");

            // Данные
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Иванов Иван");
            row1.createCell(1).setCellValue("Менеджер");
            row1.createCell(2).setCellValue(50000);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Петрова Анна");
            row2.createCell(1).setCellValue("Бухгалтер");
            row2.createCell(2).setCellValue(45000);

            workbook.write(fos);
        }
    }

    /**
     * Создает файл с датами для тестирования форматирования
     */
    private File createFileWithDates(Path tempDir) throws IOException {
        File dateFile = tempDir.resolve("dates.xlsx").toFile();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(dateFile)) {

            Sheet sheet = workbook.createSheet("Даты");

            // Заголовки
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ФИО");
            headerRow.createCell(1).setCellValue("Дата рождения");

            // Данные с датой
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("Сидоров С.С.");

            Cell dateCell = dataRow.createCell(1);
            LocalDate birthDate = LocalDate.of(1990, 3, 15);
            Date date = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            dateCell.setCellValue(date);

            // Устанавливаем формат даты
            CreationHelper createHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy"));
            dateCell.setCellStyle(dateStyle);

            workbook.write(fos);
        }

        return dateFile;
    }

    /**
     * Создает файл с разными типами числовых данных
     */
    private File createFileWithNumbers(Path tempDir) throws IOException {
        File numericFile = tempDir.resolve("numbers.xlsx").toFile();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(numericFile)) {

            Sheet sheet = workbook.createSheet("Числа");

            // Заголовки
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Целое");
            headerRow.createCell(1).setCellValue("Дробное");
            headerRow.createCell(2).setCellValue("Булево");

            // Данные
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(100);
            dataRow.createCell(1).setCellValue(99.99);
            dataRow.createCell(2).setCellValue(true);

            workbook.write(fos);
        }

        return numericFile;
    }

    /**
     * Создает файл с пустыми ячейками
     */
    private File createFileWithEmptyCells(Path tempDir) throws IOException {
        File emptyFile = tempDir.resolve("empty-cells.xlsx").toFile();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(emptyFile)) {

            Sheet sheet = workbook.createSheet("Пустые");

            // Заголовки
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Колонка1");
            headerRow.createCell(1).setCellValue("Колонка2");
            headerRow.createCell(2).setCellValue("Колонка3");

            // Первая строка: заполнена частично
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Значение1");
            // Вторая колонка остается пустой
            row1.createCell(2).setCellValue("Значение3");

            // Вторая строка: заполнена по-другому
            Row row2 = sheet.createRow(2);
            // Первая колонка остается пустой
            row2.createCell(1).setCellValue("Значение2");
            // Третья колонка остается пустой

            workbook.write(fos);
        }

        return emptyFile;
    }

    /**
     * Создает пустой XLSX файл
     */
    private File createEmptyXlsxFile(Path tempDir) throws IOException {
        File emptyFile = tempDir.resolve("empty.xlsx").toFile();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(emptyFile)) {
            workbook.createSheet("Empty");
            workbook.write(fos);
        }

        return emptyFile;
    }

    /**
     * Создает файл только с заголовками
     */
    private File createFileWithOnlyHeaders(Path tempDir) throws IOException {
        File headersFile = tempDir.resolve("headers-only.xlsx").toFile();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(headersFile)) {

            Sheet sheet = workbook.createSheet("Headers");

            // Только заголовки
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Заголовок1");
            headerRow.createCell(1).setCellValue("Заголовок2");

            workbook.write(fos);
        }

        return headersFile;
    }
}
