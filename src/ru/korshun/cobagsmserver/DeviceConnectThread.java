package ru.korshun.cobagsmserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DeviceConnectThread
        extends ConnectThread
        implements Runnable {

//    private Logger                      logger =            Main.getLoader().getLoggerInstance();

    private DatagramPacket              receivePacket;

    private String                      outputStr =         "";
    private String                      deviceIp;

    private GcmSender                   gcmSender;

    private final String LOG_FILE_PATH = "logs";


    public DeviceConnectThread(DatagramPacket receivePacket) {
        this.receivePacket =                                receivePacket;
        String ip =                                         this.receivePacket.getAddress().toString();
        gcmSender =                                         new GcmSender();

        if(!ip.contains("127.0.0.1")) {
//            System.out.println(this.receivePacket.getAddress().toString());
            deviceIp =                                      ip.substring(ip.indexOf("/") + 1);
            outputStr +=                                    getCurrentDateAndTime() + ": UDP: " +
                                                                deviceIp + " ";
        }

    }

    @Override
    public void run() {

        String inStr =                                      new String(receivePacket.getData());
        String[] data;
        ArrayList<String[]> dataForPush;

        outputStr +=                                        inStr;

        if(inStr.startsWith("7") && inStr.contains("=") && inStr.contains("/")) {

            System.out.println(outputStr);
            data =                                          parseStrForPush(inStr);

//            try {
//                logger.writeToLog(deviceIp, data[1]);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            addPhoneToMySql(data[1], data[2]);

            if (addEventToMySql(data[0], data[1])) {

                dataForPush =                                  getDataForPush(data[0], data[1]);

                if(dataForPush == null) {
                    System.out.println(getCurrentDateAndTime() + ": " + data[1] + "/" + data[0] + " - сервис приостановлен");
                    writeEventToFile(data[1], "сервис приостановлен");
                    return;
                }

                writeEventToFile(data[1], inStr.substring(0, inStr.indexOf(" ")).trim());

                dataForPush
                        .stream()
                        .filter(item -> item[1] != null)
                        .forEach(dataForPushItem -> {

                            gcmSender.send(dataForPushItem[0], dataForPushItem[2], dataForPushItem[1], dataForPushItem[3], data);

                            try {
                                TimeUnit.MILLISECONDS.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                });

            }
        }

        else if(inStr.startsWith("RECEIVED:")) {
            System.out.println(outputStr);
            System.out.println(getCurrentDateAndTime() + ": try to write ...");

            String code = inStr.substring(inStr.lastIndexOf(" "), inStr.indexOf("/")).trim();
            writeEventToFile(code, inStr);

//            System.out.println(code);
//            outputStr =                                    ": " + inStr + " ";
        }

        else {
            outputStr +=                                    ": UNKNOWN QUERY: " + inStr + " ";
            System.out.println(outputStr);
        }



    }



    /**
     *  Функция Вытаскивает из БД номер объекта и записывает событие в файл
     * @param code                  - радиоканал, с которого поступил сигнал
     * @param text                  - текст для записи в файл
     * @return                      - при удачном завершении возвращается true
     */
    private boolean writeEventToFile(String code, String text) {

        Connection connection = createConnect();
        PreparedStatement ps;
        ResultSet rs;

        String query = "SELECT " + tablePrefix + "numbers_hex.number AS `number` " +
                        "FROM " + tablePrefix + "numbers_hex " +
                        "WHERE " + tablePrefix + "numbers_hex.r_code = ?;";

        try {
            ps = connection.prepareStatement(query);
            ps.setString(1, code);

//            System.out.println(ps.toString());

            rs = ps.executeQuery();

            if(rs.next()) {
                int number = rs.getInt("number");

                if(!new File(LOG_FILE_PATH).exists()) {
                    if(!new File(LOG_FILE_PATH).mkdir()) {
                        System.out.println("Ошибка создания лог-директории");
                    }
                }

                File logFile = new File(LOG_FILE_PATH + File.separator + number + "_udp.txt");

                if(!logFile.exists()) {
                    try {
                        if(!logFile.createNewFile()) {
                            System.out.println("Ошибка создания лог-файла");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try(BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))){
                    text += "\r\n";
                    writer.append(String.format("%s %s", getCurrentDateAndTime(), text));
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            }

            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
     * @return                      - возвращается массив { строка_для_отправки, токен, расшифровка_события, номер объекта }
     *                                  в случае, если сервис работает и массив { "Сервис приостановлен" }, если сервис
     *                                  отключен, например, за неуплату
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
                                    tablePrefix + "users.m_enable AS `service_enable`, " +
                                    tablePrefix + "users_gcm_hash.gcm_hash AS `token` " +
                        "FROM " + tablePrefix + "events_codes " +
                        "LEFT JOIN " + tablePrefix + "numbers_hex ON " + tablePrefix + "numbers_hex.r_code = ? " +
                        "LEFT JOIN " + tablePrefix + "objects ON " + tablePrefix + "objects.number = " +
                                                                        tablePrefix + "numbers_hex.number " +
                        "LEFT JOIN " + tablePrefix + "users_gcm_hash ON " + tablePrefix + "users_gcm_hash.id_client = " +
                            "(SELECT " + tablePrefix + "objects.id_client " +
                            "FROM " + tablePrefix + "objects " +
                            "WHERE " + tablePrefix + "objects.number = " + tablePrefix + "numbers_hex.number) " +
                        "LEFT JOIN " + tablePrefix + "users ON " + tablePrefix + "users.id = " +
                            "(SELECT " + tablePrefix + "objects.id_client " +
                            "FROM " + tablePrefix + "objects " +
                            "WHERE " + tablePrefix + "objects.number = " + tablePrefix + "numbers_hex.number) " +
                        "WHERE " + tablePrefix + "events_codes.code = ?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setString(1, code);
            ps.setString(2, event);

//            System.out.println(ps.toString());

            rs =                                            ps.executeQuery();

            while (rs.next()) {

                String[] data;

                int serviceEnable =                         rs.getInt("service_enable");

                if(serviceEnable == 0) {
                    return null;
                }

                data =                             new String[4];

                String address = rs.getString("address") != null ?
                        decodeStr(rs.getString("address")).trim() :
                        "-";

                data[0] = getCurrentDateAndTime() + " (" +
                        rs.getString("number") + ", " +
                        address + ") " +
                        rs.getString("desc");
                data[1] = rs.getString("token");
                data[2] = rs.getString("desc");
                data[3] = rs.getString("number");

                returnArray.add(data);

            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
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
