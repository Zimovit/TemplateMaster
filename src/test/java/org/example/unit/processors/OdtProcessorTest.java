package org.example.unit.processors;

import org.example.interfaces.TemplateProcessor;
import org.example.processors.OdtProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.dom.OdfContentDom;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OdtProcessorTest {

    private OdtProcessor processor;
    private File templateFile;
    private File targetDir;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        processor = new OdtProcessor();
        templateFile = new File(tempDir, "template.odt");
        targetDir = new File(tempDir, "output");
    }

    @Test
    void testProcessCreatesTargetDirectoryIfNotExists() throws Exception {
        // Given
        List<Map<String, String>> tableData = Arrays.asList(
                Map.of("name", "John", "age", "25")
        );

        // Mock ODT document and dependencies
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(0);
            when(mockCells.getLength()).thenReturn(0);
            doNothing().when(mockDocument).save(any(File.class));

            // When
            processor.process(templateFile, tableData, targetDir);

            // Then
            assertThat(targetDir).exists().isDirectory();
        }
    }

    @Test
    void testProcessReplacesPlaceholdersInParagraphs() throws Exception {
        // Given
        List<Map<String, String>> tableData = Arrays.asList(
                Map.of("name", "John", "age", "25")
        );

        // Mock ODT document and dependencies
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);
            Node mockParagraph = mock(Node.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(1);
            when(mockParagraphs.item(0)).thenReturn(mockParagraph);
            when(mockParagraph.getTextContent()).thenReturn("Hello [name], you are [age] years old.");
            when(mockCells.getLength()).thenReturn(0);
            doNothing().when(mockDocument).save(any(File.class));

            // When
            processor.process(templateFile, tableData, targetDir);

            // Then
            verify(mockParagraph).setTextContent("Hello John, you are 25 years old.");
        }
    }

    @Test
    void testProcessReplacesPlaceholdersInTableCells() throws Exception {
        // Given
        List<Map<String, String>> tableData = Arrays.asList(
                Map.of("name", "John", "city", "New York")
        );

        // Mock ODT document and dependencies
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);
            NodeList mockCellChildren = mock(NodeList.class);
            Node mockCell = mock(Node.class);
            Node mockCellParagraph = mock(Node.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(0);
            when(mockCells.getLength()).thenReturn(1);
            when(mockCells.item(0)).thenReturn(mockCell);
            when(mockCell.getChildNodes()).thenReturn(mockCellChildren);
            when(mockCellChildren.getLength()).thenReturn(1);
            when(mockCellChildren.item(0)).thenReturn(mockCellParagraph);
            when(mockCellParagraph.getNodeType()).thenReturn(Node.ELEMENT_NODE);
            when(mockCellParagraph.getNodeName()).thenReturn("text:p");
            when(mockCellParagraph.getTextContent()).thenReturn("Name: [name], City: [city]");
            doNothing().when(mockDocument).save(any(File.class));

            // When
            processor.process(templateFile, tableData, targetDir);

            // Then
            verify(mockCellParagraph).setTextContent("Name: John, City: New York");
        }
    }

    @Test
    void testProcessHandlesEmptyTableData() throws Exception {
        // Given
        List<Map<String, String>> tableData = new ArrayList<>();

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> processor.process(templateFile, tableData, targetDir));

        // Verify no files were created
        if (targetDir.exists()) {
            File[] files = targetDir.listFiles();
            assertThat(files == null || files.length == 0).isTrue();
        }
    }

    @Test
    void testProcessHandlesMissingPlaceholderValues() throws Exception {
        // Given
        List<Map<String, String>> tableData = Arrays.asList(
                Map.of("name", "John") // missing "age" key
        );

        // Mock ODT document and dependencies
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);
            Node mockParagraph = mock(Node.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(1);
            when(mockParagraphs.item(0)).thenReturn(mockParagraph);
            when(mockParagraph.getTextContent()).thenReturn("Hello [name], you are [age] years old.");
            when(mockCells.getLength()).thenReturn(0);
            doNothing().when(mockDocument).save(any(File.class));

            // When
            processor.process(templateFile, tableData, targetDir);

            // Then - missing placeholder should be replaced with empty string
            verify(mockParagraph).setTextContent("Hello John, you are  years old.");
        }
    }

    @Test
    void testProcessThrowsIOExceptionOnDocumentLoadFailure() throws Exception {
        // Given
        List<Map<String, String>> tableData = Arrays.asList(
                Map.of("name", "John")
        );

        // Mock ODT document loading to throw exception
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenThrow(new RuntimeException("Failed to load document"));

            // When & Then
            IOException exception = assertThrows(IOException.class,
                    () -> processor.process(templateFile, tableData, targetDir));

            assertThat(exception.getMessage()).contains("Ошибка обработки документа ODT");
            assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void testProcessThrowsIOExceptionOnContentDomFailure() throws Exception {
        // Given
        List<Map<String, String>> tableData = Arrays.asList(
                Map.of("name", "John")
        );

        // Mock ODT document and make getContentDom throw exception
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doThrow(new org.xml.sax.SAXException("Failed to get content DOM"))
                    .when(mockDocument).getContentDom();

            // When & Then
            IOException exception = assertThrows(IOException.class,
                    () -> processor.process(templateFile, tableData, targetDir));

            assertThat(exception.getMessage()).contains("Ошибка обработки документа ODT");
            assertThat(exception.getCause()).isInstanceOf(org.xml.sax.SAXException.class);
        }
    }

    @Test
    void testExtractPlaceholdersFromParagraphs() throws Exception {
        // Given
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);
            Node mockParagraph1 = mock(Node.class);
            Node mockParagraph2 = mock(Node.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(2);
            when(mockParagraphs.item(0)).thenReturn(mockParagraph1);
            when(mockParagraphs.item(1)).thenReturn(mockParagraph2);
            when(mockParagraph1.getTextContent()).thenReturn("Hello [name], your age is [age]");
            when(mockParagraph2.getTextContent()).thenReturn("Your city is [city] and country is [country]");
            when(mockCells.getLength()).thenReturn(0);

            // When
            Set<String> placeholders = processor.extractPlaceholders(templateFile);

            // Then
            assertThat(placeholders).containsExactlyInAnyOrder("name", "age", "city", "country");
        }
    }

    @Test
    void testExtractPlaceholdersFromTableCells() throws Exception {
        // Given
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);
            NodeList mockCellChildren = mock(NodeList.class);
            Node mockCell = mock(Node.class);
            Node mockCellParagraph = mock(Node.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(0);
            when(mockCells.getLength()).thenReturn(1);
            when(mockCells.item(0)).thenReturn(mockCell);
            when(mockCell.getChildNodes()).thenReturn(mockCellChildren);
            when(mockCellChildren.getLength()).thenReturn(1);
            when(mockCellChildren.item(0)).thenReturn(mockCellParagraph);
            when(mockCellParagraph.getNodeType()).thenReturn(Node.ELEMENT_NODE);
            when(mockCellParagraph.getNodeName()).thenReturn("text:p");
            when(mockCellParagraph.getTextContent()).thenReturn("Employee: [employee_name] works in [department]");

            // When
            Set<String> placeholders = processor.extractPlaceholders(templateFile);

            // Then
            assertThat(placeholders).containsExactlyInAnyOrder("employee_name", "department");
        }
    }

    @Test
    void testExtractPlaceholdersReturnsEmptySetWhenNoPlaceholders() throws Exception {
        // Given
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);
            Node mockParagraph = mock(Node.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(1);
            when(mockParagraphs.item(0)).thenReturn(mockParagraph);
            when(mockParagraph.getTextContent()).thenReturn("This is plain text without placeholders");
            when(mockCells.getLength()).thenReturn(0);

            // When
            Set<String> placeholders = processor.extractPlaceholders(templateFile);

            // Then
            assertThat(placeholders).isEmpty();
        }
    }

    @Test
    void testExtractPlaceholdersThrowsIOExceptionOnFailure() throws Exception {
        // Given
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenThrow(new RuntimeException("Failed to load document"));

            // When & Then
            IOException exception = assertThrows(IOException.class,
                    () -> processor.extractPlaceholders(templateFile));

            assertThat(exception.getMessage()).contains("Ошибка извлечения плейсхолдеров");
            assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void testExtractPlaceholdersThrowsIOExceptionOnContentDomFailure() throws Exception {
        // Given
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doThrow(new org.xml.sax.SAXException("Failed to get content DOM"))
                    .when(mockDocument).getContentDom();

            // When & Then
            IOException exception = assertThrows(IOException.class,
                    () -> processor.extractPlaceholders(templateFile));

            assertThat(exception.getMessage()).contains("Ошибка извлечения плейсхолдеров");
            assertThat(exception.getCause()).isInstanceOf(org.xml.sax.SAXException.class);
        }
    }

    @Test
    void testProcessorImplementsTemplateProcessorInterface() {
        // Then
        assertThat(processor).isInstanceOf(TemplateProcessor.class);
    }

    @Test
    void testExtractPlaceholdersHandlesDuplicatePlaceholders() throws Exception {
        // Given
        try (MockedStatic<OdfTextDocument> mockedStatic = mockStatic(OdfTextDocument.class)) {
            OdfTextDocument mockDocument = mock(OdfTextDocument.class);
            OdfContentDom mockContentDom = mock(OdfContentDom.class);
            NodeList mockParagraphs = mock(NodeList.class);
            NodeList mockCells = mock(NodeList.class);
            Node mockParagraph = mock(Node.class);

            mockedStatic.when(() -> OdfTextDocument.loadDocument(templateFile))
                    .thenReturn(mockDocument);
            doReturn(mockContentDom).when(mockDocument).getContentDom();
            when(mockContentDom.getElementsByTagName("text:p")).thenReturn(mockParagraphs);
            when(mockContentDom.getElementsByTagName("table:table-cell")).thenReturn(mockCells);
            when(mockParagraphs.getLength()).thenReturn(1);
            when(mockParagraphs.item(0)).thenReturn(mockParagraph);
            when(mockParagraph.getTextContent()).thenReturn("[name] is great, [name] is awesome, [age] is fine");
            when(mockCells.getLength()).thenReturn(0);

            // When
            Set<String> placeholders = processor.extractPlaceholders(templateFile);

            // Then - Set should contain unique placeholders only
            assertThat(placeholders).containsExactlyInAnyOrder("name", "age");
            assertThat(placeholders).hasSize(2);
        }
    }
}