package org.example.interfaces;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TemplateProcessor {
    void process(File templateFile, List<Map<String, String>> tableData, File targetDir) throws IOException;
    Set<String> extractPlaceholders(File templateFile) throws IOException;
    void generateSingleDocument(File templateFile, File targetFile) throws IOException;
}
