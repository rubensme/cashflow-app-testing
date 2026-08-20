package com.cashflow.app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String URL =
            System.getenv().getOrDefault(
                    "CASHFLOW_DB_URL",
                    "jdbc:sqlite:cashflow.db"
            );

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}