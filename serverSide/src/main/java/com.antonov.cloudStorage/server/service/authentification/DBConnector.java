package com.antonov.cloudStorage.server.service.authentification;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Класс для установки соединения с базой данных
 */
public final class DBConnector {
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB = "jdbc:mysql://localhost/cloud_storage";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private static Connection connection;

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        if (connection == null)
            initConnection();
        return connection;
    }

    private synchronized static void initConnection() throws ClassNotFoundException, SQLException {
        Class.forName(DRIVER);
        connection = DriverManager.getConnection(DB, USER, PASSWORD);
    }
}
