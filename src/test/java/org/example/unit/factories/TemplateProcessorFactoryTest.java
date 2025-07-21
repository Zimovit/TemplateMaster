package org.example.unit.factories;

import org.example.factories.TemplateProcessorFactory;
import org.example.factories.TableReaderFactory;
import org.example.interfaces.TableReader;
import org.example.interfaces.TemplateProcessor;
import org.example.processors.DocxProcessor;
import org.example.processors.OdtProcessor;
import org.example.readers.OdsTableReader;
import org.example.readers.XlsxTableReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

class TemplateProcessorFactoryTest {

    @Test
    @DisplayName("Должен создавать DocxProcessor для .docx файлов")
    void shouldCreateDocxProcessorForDocxFiles() {
        // Given
        File docxFile = new File("test.docx");

        // When
        TemplateProcessor processor = TemplateProcessorFactory.fromFile(docxFile);

        // Then
        assertThat(processor).isInstanceOf(DocxProcessor.class);
    }

    @Test
    @DisplayName("Должен создавать OdtProcessor для .odt файлов")
    void shouldCreateOdtProcessorForOdtFiles() {
        // Given
        File odtFile = new File("test.odt");

        // When
        TemplateProcessor processor = TemplateProcessorFactory.fromFile(odtFile);

        // Then
        assertThat(processor).isInstanceOf(OdtProcessor.class);
    }

    @ParameterizedTest
    @CsvSource({
            "test.DOCX, org.example.processors.DocxProcessor",
            "test.ODT, org.example.processors.OdtProcessor",
            "My Document.docx, org.example.processors.DocxProcessor",
            "Документ.odt, org.example.processors.OdtProcessor"
    })
    @DisplayName("Должен корректно обрабатывать разные регистры и имена файлов")
    void shouldHandleDifferentCasesAndFileNames(String fileName, String expectedClassName) {
        // Given
        File file = new File(fileName);

        // When
        TemplateProcessor processor = TemplateProcessorFactory.fromFile(file);

        // Then
        assertThat(processor.getClass().getName()).isEqualTo(expectedClassName);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для неподдерживаемых форматов")
    void shouldThrowExceptionForUnsupportedFormats() {
        // Given
        File unsupportedFile = new File("test.txt");

        // When & Then
        assertThatThrownBy(() -> TemplateProcessorFactory.fromFile(unsupportedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported template format");
    }
}

class TableReaderFactoryTest {

    @Test
    @DisplayName("Должен создавать XlsxTableReader для .xlsx файлов")
    void shouldCreateXlsxReaderForXlsxFiles() {
        // Given
        File xlsxFile = new File("test.xlsx");

        // When
        TableReader reader = TableReaderFactory.fromFile(xlsxFile);

        // Then
        assertThat(reader).isInstanceOf(XlsxTableReader.class);
    }

    @Test
    @DisplayName("Должен создавать OdsTableReader для .ods файлов")
    void shouldCreateOdsReaderForOdsFiles() {
        // Given
        File odsFile = new File("test.ods");

        // When
        TableReader reader = TableReaderFactory.fromFile(odsFile);

        // Then
        assertThat(reader).isInstanceOf(OdsTableReader.class);
    }

    @ParameterizedTest
    @CsvSource({
            "data.XLSX, org.example.readers.XlsxTableReader",
            "data.ODS, org.example.readers.OdsTableReader",
            "Таблица данных.xlsx, org.example.readers.XlsxTableReader",
            "Данные.ods, org.example.readers.OdsTableReader"
    })
    @DisplayName("Должен корректно обрабатывать разные регистры и имена файлов")
    void shouldHandleDifferentCasesAndFileNames(String fileName, String expectedClassName) {
        // Given
        File file = new File(fileName);

        // When
        TableReader reader = TableReaderFactory.fromFile(file);

        // Then
        assertThat(reader.getClass().getName()).isEqualTo(expectedClassName);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение для неподдерживаемых форматов")
    void shouldThrowExceptionForUnsupportedFormats() {
        // Given
        File unsupportedFile = new File("data.csv");

        // When & Then
        assertThatThrownBy(() -> TableReaderFactory.fromFile(unsupportedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported table format");
    }
}
