package com.carrental.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralised JDBC connection factory.
 *
 * Reads connection settings from {@code db.properties} on the classpath, with
 * sensible defaults for a local MySQL install. Each call returns a NEW
 * {@link Connection}; callers must close it (try-with-resources).
 */
public final class DBConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;
    private static final String DRIVER;

    static {
        Properties p = new Properties();
        try (InputStream in = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) { /* fall back to defaults */ }

        URL      = p.getProperty("db.url",
                "jdbc:mysql://localhost:3306/car_rental_db?useSSL=false&serverTimezone=UTC");
        USER     = p.getProperty("db.user",     "root");
        PASSWORD = p.getProperty("db.password", "root");
        DRIVER   = p.getProperty("db.driver",   "com.mysql.cj.jdbc.Driver");

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "JDBC driver not found on classpath: " + DRIVER);
        }
    }

    private DBConnection() { }

    public static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
        c.setAutoCommit(true);
        return c;
    }
}
