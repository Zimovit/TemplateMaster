package org.example.interfaces;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TableReader {
    List<Map<String, String>> read(File file);
}
