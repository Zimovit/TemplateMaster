package org.example;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    private static ResourceBundle bundle;

    public static void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle("messages", locale, new UTF8Control());
    }

    public static String get(String key) {
        ensureInitialized();
        return bundle.getString(key);
    }

    public static ResourceBundle getBundle() {
        ensureInitialized();
        return bundle;
    }

    private static void ensureInitialized() {
        if (bundle == null) {
            // fallback на английский
            bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH, new UTF8Control());
        }
    }
}
