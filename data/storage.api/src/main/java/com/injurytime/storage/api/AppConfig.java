/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.injurytime.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Properties;

/**
 * Resolves config values in this order: 1) JVM system property (-DKEY=value) 2)
 * Environment variable (KEY) 3) User file: ~/injurytime.local.properties
 */
public final class AppConfig {

    private static final Properties FILE_PROPS = new Properties();

    static
    {
        try
        {
            Path p = Paths.get(System.getProperty("user.home"), "injurytime.local.properties");
            if (Files.isRegularFile(p))
            {
                try (InputStream in = Files.newInputStream(p))
                {
                    FILE_PROPS.load(in);
                }
            }
        } catch (IOException ignored)
        {
        }
    }

    private AppConfig()
    {
    }

    public static String get(String key)
    {
        String v = System.getProperty(key);
        if (v != null && !v.isBlank())
        {
            return v;
        }

        v = System.getenv(key);
        if (v != null && !v.isBlank())
        {
            return v;
        }

        v = FILE_PROPS.getProperty(key);
        return (v != null && !v.isBlank()) ? v : null;
    }
}
