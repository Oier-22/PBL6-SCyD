package com.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties props = new Properties();

    private ConfigLoader() {
        // Evita la instanciación
    }

    static {
        try (InputStream input = new FileInputStream("config/config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("❌ No se pudo cargar el archivo de configuración", e);
        }
    }

    public static String getDbUrl() {
        return props.getProperty("db.url");
    }

    public static String getDbUser() {
        return props.getProperty("db.user");
    }

    public static String getDbPassword() {
        return props.getProperty("db.password");
    }

    public static String getRabbitHost() {
        return props.getProperty("rabbitmq.host");
    }
    
    public static String getRabbitUsername() {
        return props.getProperty("rabbitmq.username");
    }
    
    public static String getRabbitPassword() {
        return props.getProperty("rabbitmq.password");
    }
    
    public static int getRabbitPort() {
        return Integer.parseInt(props.getProperty("rabbitmq.port", "5671"));
    }
    
    public static String getTruststorePath() {
        return props.getProperty("truststore.path");
    }
    
    public static char[] getTruststorePassword() {
        String pwd = props.getProperty("truststore.password");
        return pwd != null ? pwd.toCharArray() : null;
    }
    
}

