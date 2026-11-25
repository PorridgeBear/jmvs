package com.adcworks.jmvs.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Loads classpath resources.
 */
public final class Resources {

    private Resources() {
    }

    /**
     * Loads a UTF-8 properties file located relative to {@code anchor}'s package.
     *
     * @throws ConfigurationException if the resource is missing or unreadable
     */
    public static Properties loadProperties(Class<?> anchor, String name) {
        try (InputStream in = anchor.getResourceAsStream(name)) {
            if (in == null) {
                throw new ConfigurationException("Missing resource " + name + " next to " + anchor.getName());
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        } catch (IOException e) {
            throw new ConfigurationException("Cannot read resource " + name, e);
        }
    }
}
