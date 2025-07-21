package org.example.factories;

import org.example.interfaces.TemplateProcessor;
import org.example.processors.DocxProcessor;
import org.example.processors.OdtProcessor;

import java.io.File;

public class TemplateProcessorFactory {
    public static TemplateProcessor fromFile(File file) {
        if (file.getName().toLowerCase().endsWith(".odt")) return new OdtProcessor();
        if (file.getName().toLowerCase().endsWith(".docx")) return new DocxProcessor();
        throw new IllegalArgumentException("Unsupported template format");
    }
}
