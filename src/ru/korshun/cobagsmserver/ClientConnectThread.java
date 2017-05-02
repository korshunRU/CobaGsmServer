package ru.korshun.cobagsmserver;


import com.mkyong.asymmetric.CryptRSA;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("FieldCanBeLocal")
public class ClientConnectThread
        extends ConnectThread
        implements Runnable {

    private Socket                      socket;

    private String                      outputStr =         "";

    private boolean                     checkCommand =      true;
    private boolean                     checkIP =           false;

    private final int                   VERSION =           5;

    ClientConnectThread(Socket socket) {
        this.socket =                                       socket;

        if(!this.socket.getInetAddress().toString().contains("127.0.0.1")) {
            outputStr +=                                    getCurrentDateAndTime() + ": Клиент подключился: " +
                                                                this.socket + " ";
//            System.out.println(getCurrentDateAndTime() + ": Клиент подключился: " + this.socket);
        }
    }

    @Override
    public void run() {

        try(DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            JSONObject query;
            String inStr;

            while(true) {

                inStr =                                     in.readUTF();

                if(!checkJSON(inStr)) {
                    if(inStr.startsWith("7") && inStr.contains("=") && inStr.contains("/")) {
                        parseStrForPush(inStr);
                    }
                    else {
                        outputStr =                         getCurrentDateAndTime() + ": UNKNOWN QUERY: " + inStr + " ";
                        System.out.println(outputStr);
//                        System.out.println(getCurrentDateAndTime() + ": UNKNOWN QUERY: " + inStr);
                    }
                    break;
                }

//                System.out.println("1");

                query =                                     new JSONObject(inStr);

                String type =                               query.getString("type");

//                System.out.println("2");

                if(type.equals("exit")) {
                    break;
                }

                outputStr +=                                query;
                System.out.println(outputStr);

//                System.out.println("3");

                if(!query.has("version") || query.getInt("version") < VERSION) {
                    outputStr =                             getCurrentDateAndTime() + ": version error!";
                    sendOperationStatusToClient(out, STATUS_ERROR, "Обновите приложение");
                    System.out.println(outputStr);
                    break;
                }

//                System.out.println("WWW");

                try {
                    if(!keyInitialization(socket, type, query.getString("key"))) {
                        outputStr =                         getCurrentDateAndTime() + ": initialization error!";
                        sendOperationStatusToClient(out, STATUS_ERROR, "Неверный запрос инициализации");
                        System.out.println(outputStr);
                        break;
                    }
                } catch (Exception e) {
//                    e.printStackTrace();
                    outputStr =                             getCurrentDateAndTime() + ": check key error! " + e.getMessage();
                    sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка инициализации на сервере");
                    System.out.println(outputStr);
                    break;
                }

                JSONObject data =                           query.getJSONObject("data");
//                JSONObject returnStatus =                   new JSONObject();

                switch (type) {






                    case "enter":
                        proccessUserLogin(data, out);
                        break;







                    case "sendToken":
                        updateUserData(data.getString("token"), data.getInt("userId"), data.getString("mac"), null, out);
                        break;







                    case "getObjectsList":
                        getObjectsListFromMySql(data, out);
                        break;







                    case "getSignalsList":
                        getSignalsListFromMySql(data, out);
                        break;


                }

            }

        } catch (IOException | JSONException e) {
//            e.printStackTrace();
            System.out.println(getCurrentDateAndTime() + ": " + e.getMessage());
        } finally {
            if(!this.socket.getInetAddress().toString().contains("127.0.0.1")) {
                System.out.println(getCurrentDateAndTime() + ": Клиент отключился: " + this.socket);
            }
        }

    }


    /**
     *  Функция проверяет пришедший ключ на соответствие требованиям
     * @param socket            - ссылка на сокет, нужна для получения ip адреса
     * @param type              - тип запроса (вход, запрос сигналов и т.п.)
     * @param key               - сам ключ, зашифрованный с помощью RSA
     * @return                  - если ключ прошел проверку, возвращается true
     * @throws Exception
     */
    private boolean keyInitialization(Socket socket, String type, String key) throws Exception {
//        System.out.println(socket.getRemoteSocketAddress() + " " + key);

        CryptRSA cryptRSA = new CryptRSA();
        PrivateKey privateKey = cryptRSA.getPrivate("key/privateKey");
        String keyDecrypt = cryptRSA.decryptText(key, privateKey);
        String[] keyArray = keyDecrypt.split(":");
        String clientIp = socket.getInetAddress().toString().substring(1);

//        System.out.println(clientIp + " " + keyDecrypt);

        if(checkCommand) {
            if (!keyArray[0].equals(type)) {
                return false;
            }
        }

        if(checkIP) {
            if(!keyArray[0].equals(type) || !keyArray[1].equals(clientIp)) {
                return false;
            }
        }

        return true;
    }



    /**
     *  Функция лезет в БД и получает все сигналы с нужного объекта пользователя
     * @param data              - json c userId и номером объекта клиента
     * @param out               - ссылка на DataOutputStream для отправки сообщения клиенту
     * @throws IOException
     */
    private void getSignalsListFromMySql(JSONObject data, DataOutputStream out) throws IOException {

        Connection connection =                             createConnect();
        ResultSet rs;
        PreparedStatement ps;

        JSONObject signals =                                new JSONObject();
//        JSONObject returnStatus =                           new JSONObject();
        JSONArray array =                                   new JSONArray();

        if(connection == null) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Сервер БД недоступен");
            return;
        }

        int userId =                                        data.getInt("userId");

        if(userId == 0) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Неверный идентификатор пользователя");
            return;
        }

        int objectNumber =                                  Integer.parseInt(data.getString("objectNumber"));
        int step =                                          data.has("step") ?
                                                                data.getInt("step") :
                                                                Main.getLoader().getSettingsInstance().getITEMS_COUNT_DEFAULT();
        int listCount =                                     data.has("listCount") ?
                                                                data.getInt("listCount") :
                                                                0;


        String query = "SELECT  DATE_FORMAT(" +  tablePrefix + "events_gsm.time, '%d.%m.%Y') AS `date`, " +
                                "DATE_FORMAT(" + tablePrefix + "events_gsm.time, '%H:%i:%S') AS `time`, " +
                                tablePrefix + "events_codes.desc AS `event` " +
//                                "IF(coba_events_codes.desc LIKE '%Постановка%', '1', " +
//                                "IF(coba_events_codes.desc LIKE '%Снятие%', '2', '0')) as status " +
                        "FROM " + tablePrefix + "events_gsm " +
                        "LEFT JOIN " + tablePrefix + "events_codes " +
                            "ON " + tablePrefix + "events_codes.id = " + tablePrefix + "events_gsm.event_id " +
                        "WHERE " + tablePrefix + "events_gsm.object_id = " +
                            "(SELECT " + tablePrefix + "objects.id " +
                            "FROM " + tablePrefix + "objects " +
                            "WHERE " + tablePrefix + "objects.number = ? " +
                                "AND " + tablePrefix + "objects.id_client = ?) " +
                        "ORDER BY " + tablePrefix + "events_gsm.time DESC " +
                        "LIMIT ?,?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setInt(1, objectNumber);
            ps.setInt(2, userId);
            ps.setInt(3, listCount);
            ps.setInt(4, step);

//            System.out.println(ps.toString());

            rs =                                            ps.executeQuery();

            while (rs.next()) {

                String date =                               rs.getString("date");
                String time =                               rs.getString("time");
                String event =                              rs.getString("event");
//                String phone =                              rs.getString("phone");
//                objectName =                                rs.getString("object_name");
//                objectAddress =                             rs.getString("object_address");

                if(date == null || time == null || event == null) {
                    continue;
                }

                Map<String, String> signal =                new HashMap<>();

//                signal.put("status", rs.getString("status"));
                signal.put("date", date);
                signal.put("time", time);
                signal.put("event", event);
//                signal.put("phone", phone);

                array.put(signal);

            }

//            signals.put("objectName", decodeStr(objectName).trim());
//            signals.put("objectAddress", decodeStr(objectAddress).trim());
            signals.put("signals", array);

            query = "SELECT " + tablePrefix + "objects.name AS `object_name`, " +
                            tablePrefix + "objects.address AS `object_address` " +
//                            tablePrefix + "objects_phones.object_phone AS `object_phone`, " +
//                            "IF(coba_events_codes.desc LIKE '%Постановка%', '1', " +
//                            "IF(coba_events_codes.desc LIKE '%Снятие%', '2', '0')) as status " +
                    "FROM " + tablePrefix + "objects " +
//                    "LEFT JOIN " + tablePrefix + "objects_phones " +
//                        "ON " + tablePrefix + "objects_phones.id_object = " +
//                                "(SELECT " + tablePrefix + "objects.id " +
//                                "FROM " + tablePrefix + "objects " +
//                                "WHERE " + tablePrefix + "objects.number = ? " +
//                                    "AND " + tablePrefix + "objects.id_client = ?) " +
//                        "AND " + tablePrefix + "objects_phones.id_client = ? " +
//                    "LEFT JOIN " + tablePrefix + "events_codes ON " + tablePrefix + "events_codes.id = (" +
//                                "SELECT event_id " +
//                                "FROM " + tablePrefix + "events_gsm " +
//                                "WHERE object_id = " + tablePrefix + "objects.id AND event_id BETWEEN ? AND ? " +
//                                "ORDER BY time DESC " +
//                                "LIMIT 0,1) " +
                    "WHERE " + tablePrefix + "objects.id_client = ? " +
                        "AND " + tablePrefix + "objects.number = ?;";

            ps =                                            connection.prepareStatement(query);

//            ps.setInt(1, objectNumber);
//            ps.setInt(2, userId);
//            ps.setInt(3, userId);
//            ps.setInt(4, 1);
//            ps.setInt(5, 35);
            ps.setInt(1, userId);
            ps.setInt(2, objectNumber);

            rs =                                            ps.executeQuery();

//            System.out.println(ps.toString());

            String objectName = "-";
            String objectAddress = "-";
//            String objectPhone = "";
//            String objectGuardStatus = "";

            if(rs.next()) {
                objectName =                                rs.getString("object_name");
                objectAddress =                             rs.getString("object_address");
//                objectPhone =                               rs.getString("object_phone");
//                objectGuardStatus =                         rs.getString("status");
            }

            signals.put("objectName", objectName == null ? "-" : decodeStr(objectName).trim());
            signals.put("objectAddress", objectAddress == null ? "-" : decodeStr(objectAddress).trim());
//            signals.put("objectPhone", objectPhone);
//            signals.put("status", objectGuardStatus);

//            System.out.println(array.length());

//            returnStatus.put("data", signals);
//            returnStatus.put("status", 1);

            sendOperationStatusToClient(out, STATUS_COMPLITE, signals);

//            out.writeUTF(returnStatus.toString());
//            out.flush();

        } catch (SQLException e) {
            e.printStackTrace();
            sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка при запросе данных");
        } finally {
            try {
                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }






    /**
     *  Функция лезет в БД и получает все объекты пользователя с общей информацией по клиенту (имя\телефон)
     * @param data              - json c userId клиента
     * @param out               - ссылка на DataOutputStream для отправки сообщения клиенту
     * @throws IOException
     */
    private void getObjectsListFromMySql(JSONObject data, DataOutputStream out) throws IOException {

        Connection connection =                             createConnect();
        PreparedStatement ps;
        ResultSet rs, rs1;

        JSONObject objects =                                new JSONObject();
//        JSONObject returnStatus =                           new JSONObject();
        JSONArray array =                                   new JSONArray();

        if(connection == null) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Сервер БД недоступен");
            return;
        }

        int userId =                                        data.getInt("userId");

        if(userId == 0) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Неверный идентификатор пользователя");
            return;
        }

        String query = "SELECT " +  tablePrefix + "objects.number AS `number`, " +
                                    tablePrefix + "objects.name AS `type`, " +
                                    tablePrefix + "objects.address AS `address`, " +
                                    tablePrefix + "objects_phones.object_phone AS `phone`, " +
                                    tablePrefix + "users.name AS `user_name`, " +
                                    tablePrefix + "users.address AS `user_address`, " +
                                    tablePrefix + "users.phone AS `user_phone`, " +
                                    "IF(coba_events_codes.desc LIKE '%Постановка%', '1', " +
                                        "IF(coba_events_codes.desc LIKE '%Снятие%', '2', '0')) as status " +
                        "FROM " + tablePrefix + "objects_phones " +
                        "LEFT JOIN coba_events_codes ON coba_events_codes.id = " +
                                "(SELECT coba_events_gsm.event_id " +
                                "FROM coba_events_gsm " +
                                "WHERE coba_events_gsm.object_id = coba_objects.id " +
                                    "AND coba_events_gsm.event_id BETWEEN ? AND ? " +
                                "ORDER BY coba_events_gsm.time DESC " +
                                "LIMIT 0,1) " +
                        "LEFT JOIN coba_objects ON coba_objects.id = coba_objects_phones.id_object " +
                        "LEFT JOIN coba_users ON coba_users.id = ? " +
                        "WHERE " + tablePrefix + "objects.id_client = ?;";

//        SELECT
//        coba_objects.number AS `number`,
//        coba_objects.name AS `type`,
//        coba_objects.address AS `address`,
//        coba_objects_phones.object_phone AS `phone`,
//        IF(coba_events_codes.desc LIKE '%Постановка%', '1', IF(coba_events_codes.desc LIKE '%Снятие%', '2', '0')) as status
//        FROM coba_objects_phones
//        LEFT JOIN coba_events_codes ON coba_events_codes.id = (SELECT
//        coba_events_gsm.event_id
//        FROM coba_events_gsm
//        WHERE coba_events_gsm.object_id = coba_objects.id
//        AND coba_events_gsm.event_id BETWEEN 1 AND 35
//        ORDER BY coba_events_gsm.time
//        DESC LIMIT 0,1)
//        LEFT JOIN coba_objects ON coba_objects.id = coba_objects_phones.id_object
//        WHERE coba_objects.id_client = 19;



        try {
            ps =                                            connection.prepareStatement(query);

            ps.setInt(1, 1);
            ps.setInt(2, 35);
            ps.setInt(3, userId);
            ps.setInt(4, userId);

//            System.out.println(ps.toString());

            rs =                                            ps.executeQuery();

            String userName = "-";
            String userAddress = "-";
            String userPhone = "-";

            while (rs.next()) {

                String number =                             rs.getString("number");
                String type =                               rs.getString("type");
                String address =                            rs.getString("address");
                userName =                                  rs.getString("user_name");
                userAddress =                               rs.getString("user_address");
                userPhone =                                 rs.getString("user_phone");
                String objectPhone =                        rs.getString("phone");

                if(number == null) {
                    continue;
                }

                Map<String, String> object =                new HashMap<>();

                object.put("status", rs.getString("status"));
                object.put("number", number);
                object.put("objectPhone", objectPhone);
                object.put("type", type != null ? decodeStr(type).trim() : "-");
                object.put("address", address != null ? decodeStr(address).trim() : "-");

                query = "SELECT  DATE_FORMAT(" +  tablePrefix + "events_gsm.time, '%d.%m.%Y') AS `date`, " +
                                "DATE_FORMAT(" + tablePrefix + "events_gsm.time, '%H:%i:%S') AS `time`, " +
                                                tablePrefix + "events_codes.desc AS `event` " +
                        "FROM " + tablePrefix + "events_gsm " +
                        "LEFT JOIN " + tablePrefix + "events_codes " +
                                "ON " + tablePrefix + "events_codes.id = " + tablePrefix + "events_gsm.event_id " +
                                "WHERE " + tablePrefix + "events_gsm.object_id = " +
                                    "(SELECT " + tablePrefix + "objects.id " +
                                    "FROM " + tablePrefix + "objects " +
                                    "WHERE " + tablePrefix + "objects.number = ? " +
                                    "AND " + tablePrefix + "objects.id_client = ?) " +
                        "ORDER BY " + tablePrefix + "events_gsm.time DESC " +
                        "LIMIT 0,4;";

                ps =                                            connection.prepareStatement(query);

                ps.setInt(1, Integer.parseInt(number));
                ps.setInt(2, userId);

//                System.out.println(ps.toString());

                rs1 =                                           ps.executeQuery();
                int x =                                         1;

                while (rs1.next()) {

                    String date =                               rs1.getString("date");
                    String time =                               rs1.getString("time");
                    String event =                              rs1.getString("event");

                    if(date == null || time == null || event == null) {
                        continue;
                    }

                    object.put("signal" + x, String.format("%s %s %s", date, time, event));

//                    object.put("date" + x, date);
//                    object.put("time" + x, time);
//                    object.put("event" + x, event);

                    x++;
                }

                array.put(object);

            }

//            System.out.println(array.toString());

            objects.put("userName", userName == null ? "-" : decodeStr(userName).trim());
            objects.put("userAddress", userAddress == null ? "-" : decodeStr(userAddress).trim());
            objects.put("userPhone", userPhone == null ? "-" : decodeStr(userPhone).trim());
            objects.put("objects", array);



//            returnStatus.put("data", objects);
//            returnStatus.put("status", 1);

            sendOperationStatusToClient(out, STATUS_COMPLITE, objects);

//            out.writeUTF(returnStatus.toString());
//            out.flush();

        } catch (SQLException e) {
            e.printStackTrace();
            sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка при запросе данных");
        } finally {
            try {
                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }





    /**
     *  Функция проверяет пару логин-пароль на соответствие с данными из БД
     * @param data              - JSONObject с данными
     * @param out               - ссылка на DataOutputStream для отправки сообщения клиенту
     * @throws IOException
     */
    private void proccessUserLogin(JSONObject data, DataOutputStream out) throws IOException {
        Connection connection =                             createConnect();
        PreparedStatement ps;
        ResultSet rs;

        String login =                                      data.getString("login");
        String pass =                                       data.getString("pass");
        String token =                                      data.getString("token");

//        if(data.has("time")) {
//
//            try {
//                CryptRSA cryptRSA = new CryptRSA();
//                PrivateKey privateKey = cryptRSA.getPrivate("key/privateKey");
//                String keyD = cryptRSA.decryptText(data.getString("time"), privateKey);
//
////                System.out.println(keyD);
//
//                long timeServer = Calendar.getInstance().getTimeInMillis();
//                long timeClient = Long.parseLong(keyD);
//                long timeFault = Main.getLoader().getSettingsInstance().getTIME_FAULT_IN_SECONDS();
//
//                System.out.println(String.format("Client: %s, Server: %s", timeClient, timeServer));
//
//                Date dateClient = new Date(timeClient);
//                Date dateServer = new Date(timeServer);
//                DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
//                String dateFormattedClient = formatter.format(dateClient);
//                String dateFormattedServer = formatter.format(dateServer);
//                System.out.println(String.format("Client: %s, Server: %s", dateFormattedClient, dateFormattedServer));
//
//
//                if(timeServer - timeClient <= timeFault) {
//                    System.out.println("OK");
//                }
//                else {
//                    System.out.println("Error");
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }

        if(!data.has("mac")) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Обновите приложение");
            return;
    }

        if(connection == null) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Сервер БД недоступен");
            return;
        }

        String mac =                                        data.getString("mac");
        String api =                                        data.getString("api");


        String query = "SELECT COUNT(*) AS `size`, " +
                            tablePrefix + "users.id AS `id`, " +
                            tablePrefix + "users.name AS `name`, " +
                            tablePrefix + "users.m_enable AS `service_enable`, " +
                            tablePrefix + "objects.number AS `number` " +
                        "FROM " + tablePrefix + "objects " +
                        "LEFT JOIN " + tablePrefix + "users ON " + tablePrefix + "users.id = " + tablePrefix + "objects.id_client " +
                        "WHERE (" + tablePrefix + "users.login = ? OR " + tablePrefix + "users.login_short = ?) " +
                                "AND " + tablePrefix + "users.password_mobile = ? " +
                        "GROUP BY " + tablePrefix + "objects.number;";



        try {
            ps =                                            connection.prepareStatement(query);

            ps.setString(1, login.trim());
            ps.setString(2, login.trim());
            ps.setString(3, pass);

//            System.out.println(ps.toString());

            rs =                                            ps.executeQuery();

//            System.out.println();

            int size = 0, userId = 0, serviceEnable = 0;
            String objects = "", userName = "";
            JSONObject userIdData = new JSONObject();

            while(rs.next()) {

                serviceEnable =                             rs.getInt("service_enable");
                size =                                      rs.getInt("size");
                userId =                                    rs.getInt("id");
                userName =                                  rs.getString("name");
                objects +=                                  rs.getString("number") + ",";

            }

            if(size == 1) {

                if(serviceEnable == 0) {
                    sendOperationStatusToClient(out, STATUS_ERROR, "Доступ закрыт, обратитесь в офис");
                    System.out.println(getCurrentDateAndTime() + ": Доступ закрыт");
                    return;
                }

                userIdData.put("userId", userId);
                userIdData.put("userName", decodeStr(userName).trim());
                userIdData.put("listObjects", objects.substring(0, objects.length() - 1));

                sendOperationStatusToClient(out, STATUS_COMPLITE, userIdData);
                updateUserData(token, userId, mac, api);
                addEnterDateTime(userId);
                System.out.println(getCurrentDateAndTime() + ": Авторизация успешна");
            }
            else {
                sendOperationStatusToClient(out, STATUS_ERROR, "Неверные данные");
                System.out.println(getCurrentDateAndTime() + ": Авторизация не удалась");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка при запросе данных");
        } finally {
            try {
                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }






    /**
     *  Отправка статуса операции клиенту
     * @param out                       - ссылка на DataOutputStream
     * @param status                    - числовое значение статуса операции
     * @param errMessage                - в случае, если произошла ошибка, отправка сообщения о ней
     * @param data                      - данные, которые надо отправить клиенту. Например, id пользователя
     * @throws IOException
     */
    private void sendOperationStatusToClient(DataOutputStream out, int status, String errMessage, JSONObject data)
            throws IOException {
        JSONObject returnStatus =                           new JSONObject();
        returnStatus.put("status", status);
        if(errMessage != null) {
            returnStatus.put("message", errMessage);
        }
        if(data != null) {
            returnStatus.put("data", data);
        }
        out.writeUTF(returnStatus.toString());
        out.flush();
    }




    /**
     *  Отправка статуса операции клиенту
     * @param out                       - ссылка на DataOutputStream
     * @param status                    - числовое значение статуса операции
     * @throws IOException
     */
    private void sendOperationStatusToClient(DataOutputStream out, int status) throws IOException {
        sendOperationStatusToClient(out, status, null, null);
    }






    /**
     *  Отправка статуса операции клиенту
     * @param out                       - ссылка на DataOutputStream
     * @param status                    - числовое значение статуса операции
     * @param data                      - данные, которые надо отправить клиенту. Например, id пользователя
     * @throws IOException
     */
    private void sendOperationStatusToClient(DataOutputStream out, int status, JSONObject data) throws IOException {
        sendOperationStatusToClient(out, status, null, data);
    }





    /**
     *  Отправка статуса операции клиенту
     * @param out                       - ссылка на DataOutputStream
     * @param status                    - числовое значение статуса операции
     * @param errMessage                - в случае, если произошла ошибка, отправка сообщения о ней
     * @throws IOException
     */
    private void sendOperationStatusToClient(DataOutputStream out, int status, String errMessage) throws IOException {
        sendOperationStatusToClient(out, status, errMessage, null);
    }


    /**
     *  Обновление gcm ключа клиента
     * @param token                 - ключ
     * @param userId                - id пользователя
     * @param mac                   - mac телефона пользователя
     * @param api                   - версия ос на телефоне
     * @throws IOException
     */
    private void updateUserData(String token, int userId, String mac, String api) throws IOException {
        updateUserData(token, userId, mac, api, null);
    }



    /**
     *  Обновление gcm ключа клиента
     * @param token                 - ключ
     * @param userId                - id пользователя
     * @param mac                   - mac телефона пользователя
     * @param out                   - ссылка на DataOutputStream
     * @throws IOException
     */
    private void updateUserData(String token, int userId, String mac, String api, DataOutputStream out)
            throws IOException {

        Connection connection =                             createConnect();
        PreparedStatement ps;

        if(connection == null || userId == 0 || token.length() == 0){
            return;
        }

        String query = "INSERT INTO " + tablePrefix + "users_gcm_hash " +
                        "SET " + tablePrefix + "users_gcm_hash.id_client = ?, " +
                                tablePrefix + "users_gcm_hash.gcm_hash = ?, " +
                                tablePrefix + "users_gcm_hash.v_api = ?, " +
                                tablePrefix + "users_gcm_hash.phone_mac = ? " +
                        "ON DUPLICATE KEY UPDATE " +
                                tablePrefix + "users_gcm_hash.id_client = ?, " +
                                tablePrefix + "users_gcm_hash.gcm_hash = ?," +
                                tablePrefix + "users_gcm_hash.v_api = ?," +
                                tablePrefix + "users_gcm_hash.phone_mac = ?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setString(3, api);
            ps.setString(4, mac);
            ps.setInt(5, userId);
            ps.setString(6, token);
            ps.setString(7, api);
            ps.setString(8, mac);

//            System.out.println(ps.toString());
            ps.executeUpdate();

            if(out != null) {
                sendOperationStatusToClient(out, STATUS_COMPLITE);
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

    }





    /**
     *  Добавляем в БД метку о времени входа пользователя
     * @param userId                - id пользователя
     */
    private void addEnterDateTime(int userId) {
        Connection connection =                             createConnect();
        PreparedStatement ps;

        String ip =                                         socket.getInetAddress().toString();

        if(connection == null) {
            return;
        }

        String query = "INSERT INTO " + tablePrefix + "users_login " +
                        "SET " + tablePrefix + "users_login.id_client = ?, " +
                                tablePrefix + "users_login.date = ?, " +
                                tablePrefix + "users_login.ip = ?, " +
                                tablePrefix + "users_login.is_mobile = ?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setInt(1, userId);
            ps.setString(2, getCurrentDateAndTimeForMySQL());
            ps.setString(3, ip.substring(1));
            ps.setInt(4, 1);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }





    /**
     *  Проверяем, является ли строка JSON объектом
     * @param str               - строка для проверки
     * @return                  - в случае успеха возвращается true
     */
    private boolean checkJSON(String str) {
        try {
            new JSONObject(str);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }





}
