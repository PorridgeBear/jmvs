package com.adcworks.jmvs.core.config;

/**
 * Invalid or missing configuration (e.g. a malformed properties entry).
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
