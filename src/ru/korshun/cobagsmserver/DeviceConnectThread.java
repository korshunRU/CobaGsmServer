package ru.korshun.cobagsmserver;

import java.net.DatagramPacket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceConnectThread
        extends ConnectThread
        implements Runnable {

    private DatagramPacket              receivePacket;

    private String                      outputStr =         "";


    public DeviceConnectThread(DatagramPacket receivePacket) {
        this.receivePacket =                                receivePacket;
        if(!this.receivePacket.getAddress().toString().contains("127.0.0.1")) {
            outputStr +=                                    getCurrentDateAndTime() + ": UDP Клиент подключился: " +
                                                                this.receivePacket.getAddress() + " ";
        }

    }

    @Override
    public void run() {

        String inStr =                                      new String(receivePacket.getData());
        String[] data, dataForPush;

        outputStr +=                                        inStr;
        System.out.println(outputStr);

        if(inStr.startsWith("7") && inStr.contains("=") && inStr.contains("#") && inStr.contains("/")) {
            data =                                          parseStrForPush(inStr);

            if (addEventToMySql(data[0], data[1])) {
                dataForPush =                               getDataForPush(data[0], data[1]);
                new GcmSender().send(dataForPush[0], dataForPush[1]);
            }
        }
        else {
            outputStr +=                                    ": UNKNOWN QUERY: " + inStr + " ";
        }



    }




    /**
     *  Добавляем событие в базу данных
     * @param event             - код события (А1\D1 etc)
     * @param code              - код радиоканала объекта
     */
    protected boolean addEventToMySql(String event, String code) {
        Connection connection =                             createConnect();
        PreparedStatement ps;

        String query =  "INSERT INTO " +  tablePrefix + "events_gsm " +
                        "SET " +  tablePrefix + "events_gsm.time = ?, " +
                                tablePrefix + "events_gsm.object_id = " +
                                        "IFNULL((SELECT " +  tablePrefix + "objects.id " +
                                        "FROM " +  tablePrefix + "objects " +
                                        "WHERE " +  tablePrefix + "objects.number = " +
                                                "(SELECT " +  tablePrefix + "numbers_hex.number " +
                                                        "FROM " +  tablePrefix + "numbers_hex " +
                                                        "WHERE " +  tablePrefix + "numbers_hex.r_code = ?)), 0), " +
                                tablePrefix + "events_gsm.code_id = " +
                                        "IFNULL((SELECT " +  tablePrefix + "numbers_hex.id " +
                                                "FROM " +  tablePrefix + "numbers_hex " +
                                                "WHERE " +  tablePrefix + "numbers_hex.r_code = ?), 0), " +
                                tablePrefix + "events_gsm.event_id = " +
                                        "IFNULL((SELECT " +  tablePrefix + "events_codes.id " +
                                                "FROM " +  tablePrefix + "events_codes " +
                                                "WHERE " +  tablePrefix + "events_codes.code = ?), 0);";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setString(1, getCurrentDateAndTimeForMySQL());
            ps.setString(2, code);
            ps.setString(3, code);
            ps.setString(4, event);

            ps.executeUpdate();

        }  catch (SQLException e) {
            System.out.println("ERROR!!!! " + e.getMessage());
//            e.printStackTrace();
            return false;
        } finally {
            try {
                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return true;

    }




    private String[] getDataForPush(String event, String code) {

        Connection connection =                             createConnect();
        PreparedStatement ps;
        ResultSet rs;

        String[] returnArray =                              new String[2];

        String query = "SELECT " + tablePrefix + "events_codes.desc AS `desc`, " +
                                    tablePrefix + "numbers_hex.number AS `number`, " +
                                    tablePrefix + "objects.address AS `address`, " +
                                    tablePrefix + "users.gcm_hash AS `token` " +
                        "FROM " + tablePrefix + "events_codes " +
                        "LEFT JOIN " + tablePrefix + "numbers_hex ON " + tablePrefix + "numbers_hex.r_code = ? " +
                        "LEFT JOIN " + tablePrefix + "objects ON " + tablePrefix + "objects.number = " +
                                                                        tablePrefix + "numbers_hex.number " +
                        "LEFT JOIN " + tablePrefix + "users ON " + tablePrefix + "users.id = " +
                                                "(SELECT " + tablePrefix + "objects.id_client " +
                                                "FROM " + tablePrefix + "objects " +
                                                "WHERE " + tablePrefix + "objects.number = " +
                                                            tablePrefix + "numbers_hex.number) " +
                        "WHERE " + tablePrefix + "events_codes.code = ?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setString(1, code);
            ps.setString(2, event);

            rs =                                            ps.executeQuery();

            if(rs.first()) {

                returnArray[0] =                            getCurrentDateAndTime() + " (" +
                                                            rs.getString("number") + ", " + rs.getString("address") + ") " +
                                                            rs.getString("desc");
                returnArray[1] =                            rs.getString("token");

            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        return returnArray;
    }



}
