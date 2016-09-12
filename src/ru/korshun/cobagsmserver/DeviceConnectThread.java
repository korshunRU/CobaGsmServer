package ru.korshun.cobagsmserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DeviceConnectThread
        extends ConnectThread
        implements Runnable {

    private Logger                      logger =            Main.getLoader().getLoggerInstance();

    private DatagramPacket              receivePacket;

    private String                      outputStr =         "";
    private String                      deviceIp;

    private GcmSender                   gcmSender;


    public DeviceConnectThread(DatagramPacket receivePacket) {
        this.receivePacket =                                receivePacket;
        String ip =                                         this.receivePacket.getAddress().toString();
        gcmSender =                                         new GcmSender();

        if(!ip.contains("127.0.0.1")) {
//            System.out.println(this.receivePacket.getAddress().toString());
            deviceIp =                                      ip.substring(ip.indexOf("/") + 1);
            outputStr +=                                    getCurrentDateAndTime() + ": UDP Клиент подключился: " +
                                                                deviceIp + " ";
        }

    }

    @Override
    public void run() {

        String inStr =                                      new String(receivePacket.getData());
        String[] data;
        ArrayList<String[]> hashList;

        outputStr +=                                        inStr;
        System.out.println(outputStr);

        if(inStr.startsWith("7") && inStr.contains("=") && inStr.contains("/")) {
            data =                                          parseStrForPush(inStr);

            try {
                logger.writeToLog(deviceIp, data[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            addPhoneToMySql(data[1], data[2]);

            if (addEventToMySql(data[0], data[1])) {

                hashList =                                  getDataForPush(data[0], data[1]);

                hashList
                        .stream()
                        .filter(dataForPush -> dataForPush[1] != null)
                        .forEach(dataForPush -> {

                    gcmSender.send(dataForPush[0], dataForPush[2], dataForPush[1], dataForPush[3]);
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                });

            }
        }
        else {
            outputStr +=                                    ": UNKNOWN QUERY: " + inStr + " ";
        }



    }


    /**
     *  Функция записывает в БД номер телефона блока
     * @param code                  - радиоканал объекта
     * @param phone                 - телефонный номер
     */
    private void addPhoneToMySql(String code, String phone) {

        Connection connection =                             createConnect();
        PreparedStatement ps;

        String query =  "INSERT INTO " + tablePrefix + "objects_phones " +
                        "SET  " + tablePrefix + "objects_phones.id_client = (SELECT id_client " +
                                                                            "FROM  " + tablePrefix + "objects " +
                                                                            "WHERE number = (SELECT number " +
                                                                                            "FROM  " + tablePrefix + "numbers_hex " +
                                                                                            "WHERE r_code = ?)), " +
                                tablePrefix + "objects_phones.id_object =   (SELECT id " +
                                                                            "FROM  " + tablePrefix + "objects " +
                                                                            "WHERE number = (SELECT number " +
                                                                                            "FROM  " + tablePrefix + "numbers_hex " +
                                                                                            "WHERE r_code = ?)), " +
                                tablePrefix + "objects_phones.object_phone = ? " +
                        "ON DUPLICATE KEY UPDATE " +
                                tablePrefix + "objects_phones.id_client = (SELECT id_client " +
                                                                            "FROM  " + tablePrefix + "objects " +
                                                                            "WHERE number = (SELECT number " +
                                                                                            "FROM  " + tablePrefix + "numbers_hex " +
                                                                                            "WHERE r_code = ?)), " +
                                tablePrefix + "objects_phones.id_object = (SELECT id " +
                                                                            "FROM  " + tablePrefix + "objects " +
                                                                            "WHERE number = (SELECT number " +
                                                                                            "FROM  " + tablePrefix + "numbers_hex " +
                                                                                            "WHERE r_code = ?)), " +
                                tablePrefix + "objects_phones.object_phone = ?";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setString(1, code);
            ps.setString(2, code);
            ps.setString(3, phone);
            ps.setString(4, code);
            ps.setString(5, code);
            ps.setString(6, phone);

            ps.executeUpdate();

        }  catch (SQLException e) {
//            System.out.println(getCurrentDateAndTime() + ": " + e.getMessage());
        } finally {
            try {
                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
                        "SET " +    tablePrefix + "events_gsm.time = ?, " +
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
//            if(!e.getMessage().contains("FOREIGN KEY (`event_id`) REFERENCES `coba_events_codes`")) {
//                System.out.println(getCurrentDateAndTime() + ": Error inserting data ");
//            }
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




    /**
     *  Функция достает из БД всю необходимую информацию, для формирования и отправки push сообщения: это номер объекта,
     *  адрес, описание события и токен gcm
     * @param event                 - код события, который пришел с блока
     * @param code                  - hex номер радиоканала объекта
     * @return                      - возвращается массив { строкадляотправки, токен}
     */
    private ArrayList<String[]> getDataForPush(String event, String code) {

        Connection connection =                             createConnect();
        PreparedStatement ps;
        ResultSet rs;

//        String[] returnArray =                              new String[2];
        ArrayList<String[]> returnArray =                   new ArrayList<>();

        String query = "SELECT " + tablePrefix + "events_codes.desc AS `desc`, " +
                                    tablePrefix + "numbers_hex.number AS `number`, " +
                                    tablePrefix + "objects.address AS `address`, " +
                                    tablePrefix + "users_gcm_hash.gcm_hash AS `token` " +
                        "FROM " + tablePrefix + "events_codes " +
                        "LEFT JOIN " + tablePrefix + "numbers_hex ON " + tablePrefix + "numbers_hex.r_code = ? " +
                        "LEFT JOIN " + tablePrefix + "objects ON " + tablePrefix + "objects.number = " +
                                                                        tablePrefix + "numbers_hex.number " +
                        "LEFT JOIN " + tablePrefix + "users_gcm_hash ON " + tablePrefix + "users_gcm_hash.id_client = " +
                                                "(SELECT " + tablePrefix + "objects.id_client " +
                                                "FROM " + tablePrefix + "objects " +
                                                "WHERE " + tablePrefix + "objects.number = " +
                                                            tablePrefix + "numbers_hex.number) " +
                        "WHERE " + tablePrefix + "events_codes.code = ?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setString(1, code);
            ps.setString(2, event);

//            System.out.println(ps.toString());

            rs =                                            ps.executeQuery();

            while (rs.next()) {

                String[] data =                             new String[3];

                String address =                            rs.getString("address") != null ?
                                                                decodeStr(rs.getString("address")).trim() :
                                                                "-";

                data[0] =                                   getCurrentDateAndTime() + " (" +
                                                                rs.getString("number") + ", " +
                                                                address + ") " +
                                                                rs.getString("desc");
                data[1] =                                   rs.getString("token");
                data[2] =                                   rs.getString("desc");
                data[3] =                                   rs.getString("number");

                returnArray.add(data);

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

//        System.out.println(returnArray[0]);

        return returnArray;
    }



}
