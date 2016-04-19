package ru.korshun.cobagsmserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Sql {

    public Sql() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Main.getLoader().getSettingsInstance().getHOST_SUFFIX() +
                        Main.getLoader().getSettingsInstance().getHOST() +
                        Main.getLoader().getSettingsInstance().getDATABASE_NAME(),
                Main.getLoader().getSettingsInstance().getUSERNAME(),
                Main.getLoader().getSettingsInstance().getPASSWORD()
        );
    }

    public void disconnectionFromSql(Connection connection) throws SQLException {
        if(connection != null) {
            connection.close();
        }
    }

}
