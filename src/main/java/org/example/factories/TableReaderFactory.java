package org.example.factories;

import org.example.interfaces.TableReader;
import org.example.readers.OdsTableReader;
import org.example.readers.XlsxTableReader;

import java.io.File;

public class TableReaderFactory {
    public static TableReader fromFile(File file) {
        if (file.getName().toLowerCase().endsWith(".ods")) return new OdsTableReader();
        if (file.getName().toLowerCase().endsWith(".xlsx")) return new XlsxTableReader();
        throw new IllegalArgumentException("Unsupported table format");
    }
}
