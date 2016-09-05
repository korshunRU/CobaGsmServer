package ru.korshun.cobagsmserver;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
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

    private final int                   VERSION =           1;

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
                        outputStr +=                        ": UNKNOWN QUERY: " + inStr + " ";
//                        System.out.println(getCurrentDateAndTime() + ": UNKNOWN QUERY: " + inStr);
                    }
                    break;
                }

//                System.out.println(checkJSON(inStr));

                query =                                     new JSONObject(inStr);

                String type =                               query.getString("type");

                if(type.equals("exit")) {
                    break;
                }

                if(!query.has("version") || query.getInt("version") < VERSION) {
                    outputStr +=                            ": version error!";
                    sendOperationStatusToClient(out, STATUS_ERROR, "Обновите приложение");
                    System.out.println(outputStr);
                    break;
                }

                outputStr +=                                query;
                System.out.println(outputStr);

                JSONObject data =                           query.getJSONObject("data");
//                JSONObject returnStatus =                   new JSONObject();

                switch (type) {






                    case "enter":
                        proccessUserLogin(data, out);
                        break;







                    case "sendToken":
                        updateToken(data.getString("token"), data.getInt("userId"), data.getString("mac"), out);
                        break;







                    case "getObjectsList":
                        getObjectsListFromMySql(data, out);

//                        JSONObject objects = new JSONObject();
//                        JSONArray array = new JSONArray();
//
//                        Map<String, String> object = new HashMap<>();
//                        object.put("number", "2548" + " " + data.get("userId"));
//                        object.put("address", "Ленина 10-12 (1 эт.)");
//                        array.put(object);
//
//                        Map<String, String> object1 = new HashMap<>();
//                        object1.put("number", "17587");
//                        object1.put("address", "Малышева 45 (1 эт.)");
//                        array.put(object1);
//
//                        Map<String, String> object2 = new HashMap<>();
//                        object2.put("number", "22548");
//                        object2.put("address", "Московская 33А, оф. 12 (2 эт.)");
//                        array.put(object2);
//
//                        objects.put("objects", array);
//                        returnStatus.put("data", objects);
//                        returnStatus.put("status", 1);
//
////                        System.out.println(returnStatus);
//
//                        out.writeUTF(returnStatus.toString());
//                        out.flush();
                        break;







                    case "getSignalsList":
                        getSignalsListFromMySql(data, out);

//                        JSONObject signals = new JSONObject();
//                        JSONArray arrayS = new JSONArray();
//
//                        Map<String, String> signal = new HashMap<>();
//                        signal.put("date", "01.01.2016");
//                        signal.put("time", "08:12:58");
//                        signal.put("event", data.get("objectNumber") + " " + data.get("userId") + " " + "Снятие (Пользователь 1)");
//                        arrayS.put(signal);
//
//                        Map<String, String> signal1 = new HashMap<>();
//                        signal1.put("date", "01.01.2016");
//                        signal1.put("time", "08:12:58");
//                        signal1.put("event", "Снятие (Пользователь 1)");
//                        arrayS.put(signal1);
//
//                        Map<String, String> signal2 = new HashMap<>();
//                        signal2.put("date", "01.01.2016");
//                        signal2.put("time", "08:12:58");
//                        signal2.put("event", "Снятие (Пользователь 1)");
//                        arrayS.put(signal2);
//
//                        signals.put("signals", arrayS);
//                        returnStatus.put("data", signals);
//                        returnStatus.put("status", 1);
//
////                        System.out.println(returnStatus);
//
//                        out.writeUTF(returnStatus.toString());
//                        out.flush();
                        break;


                }

            }

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if(!this.socket.getInetAddress().toString().contains("127.0.0.1")) {
                System.out.println(getCurrentDateAndTime() + ": Клиент отключился: " + this.socket);
            }
        }

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
                                tablePrefix + "events_codes.desc AS `event`, " +
                                "IF(coba_events_codes.desc LIKE '%Постановка%', '1', " +
                                "IF(coba_events_codes.desc LIKE '%Снятие%', '2', '0')) as status " +
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
            ps.setInt(4, listCount + step);

            rs =                                            ps.executeQuery();

            while (rs.next()) {

                String date =                               rs.getString("date");
                String time =                               rs.getString("time");
                String event =                              rs.getString("event");

                if(date == null || time == null || event == null) {
                    continue;
                }

                Map<String, String> signal =                new HashMap<>();

                signal.put("status", rs.getString("status"));
                signal.put("date", date);
                signal.put("time", time);
                signal.put("event", event);

                array.put(signal);

            }

            signals.put("signals", array);
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
     *  Функция лезет в БД и получает все объекты пользователя
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
                                    "IF(coba_events_codes.desc LIKE '%Постановка%', '1', " +
                                        "IF(coba_events_codes.desc LIKE '%Снятие%', '2', '0')) as status " +
                        "FROM " + tablePrefix + "objects " +
                        "LEFT JOIN coba_events_codes ON coba_events_codes.id = " +
                                "(SELECT coba_events_gsm.event_id " +
                                "FROM coba_events_gsm " +
                                "WHERE coba_events_gsm.object_id = coba_objects.id " +
                                "AND coba_events_gsm.event_id BETWEEN ? AND ? " +
                                "ORDER BY coba_events_gsm.time DESC " +
                                "LIMIT 0,1)" +

                "WHERE " + tablePrefix + "objects.id_client = ?;";

//        SELECT
//        coba_objects.number AS number,
//                coba_objects.name AS name,
//        coba_objects.address AS address,
//                IF(coba_events_codes.desc LIKE '%Постановка%', '1', IF(coba_events_codes.desc LIKE '%Снятие%', '2', '0')) as status
//        FROM coba_objects
//        LEFT JOIN coba_events_codes ON coba_events_codes.id = (SELECT coba_events_gsm.event_id
//        FROM coba_events_gsm
//        WHERE coba_events_gsm.object_id = coba_objects.id
//        AND coba_events_gsm.event_id BETWEEN 1 AND 32
//        ORDER BY coba_events_gsm.time DESC
//        LIMIT 0,1)
//        WHERE coba_objects.id_client = 19

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setInt(1, 1);
            ps.setInt(2, 35);
            ps.setInt(3, userId);

//            System.out.println(ps.toString());

            rs =                                            ps.executeQuery();

            while (rs.next()) {

                String number =                             rs.getString("number");
                String type =                               rs.getString("type");
                String address =                            rs.getString("address");

                if(number == null) {
                    continue;
                }

                Map<String, String> object =                new HashMap<>();

                object.put("status", rs.getString("status"));
                object.put("number", number);
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

        if(!data.has("mac")) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Обновите приложение");
            return;
        }

        if(connection == null) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Сервер БД недоступен");
            return;
        }

        String mac =                                        data.getString("mac");


        String query = "SELECT COUNT(*) AS `size`, " +
                            tablePrefix + "users.id AS `id`, " +
                            tablePrefix + "users.name AS `name` " +
                        "FROM " + tablePrefix + "users " +
                        "WHERE (" + tablePrefix + "users.login = ? OR " + tablePrefix + "users.login_short = ?)" +
                                "AND " + tablePrefix + "users.password_mobile = ?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setString(1, login.trim());
            ps.setString(2, login.trim());
            ps.setString(3, pass);

            rs =                                            ps.executeQuery();

            if(rs.first()) {

                int size =                                  rs.getInt("size");
                int userId =                                rs.getInt("id");
                String userName =                           rs.getString("name");

                if(size == 1) {
                    JSONObject userIdData =                 new JSONObject();

                    userIdData.put("userId",                userId);
                    userIdData.put("userName",              decodeStr(userName).trim());

                    sendOperationStatusToClient(out, STATUS_COMPLITE, userIdData);
                    updateToken(token, userId, mac);
                    addEnterDateTime(userId);
                    System.out.println(getCurrentDateAndTime() + ": Авторизация успешна");
                }

                else {
                    sendOperationStatusToClient(out, STATUS_ERROR, "Неверные данные");
                    System.out.println(getCurrentDateAndTime() + ": Авторизация не удалась");
                }

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
     * @param mac                   mac- mac телефона пользователя
     * @throws IOException
     */
    private void updateToken(String token, int userId, String mac) throws IOException {
        updateToken(token, userId, mac, null);
    }


    /**
     *  Обновление gcm ключа клиента
     * @param token                 - ключ
     * @param userId                - id пользователя
     * @param mac                   - mac телефона пользователя
     * @param out                   - ссылка на DataOutputStream
     * @throws IOException
     */
    private void updateToken(String token, int userId, String mac, DataOutputStream out) throws IOException {

        Connection connection =                             createConnect();
        PreparedStatement ps;

        if(connection == null || userId == 0){
            return;
        }

//        String query = "UPDATE " + tablePrefix + "users " +
//                        "SET " + tablePrefix + "users.gcm_hash = ? " +
//                        "WHERE " + tablePrefix + "users.id = ?;";

        String query = "INSERT INTO " + tablePrefix + "users_gcm_hash " +
                        "SET " + tablePrefix + "users_gcm_hash.id_client = ?, " +
                                tablePrefix + "users_gcm_hash.gcm_hash = ?, " +
                                tablePrefix + "users_gcm_hash.phone_mac = ? " +
                        "ON DUPLICATE KEY UPDATE " +
                                tablePrefix + "users_gcm_hash.gcm_hash = ?," +
                                tablePrefix + "users_gcm_hash.phone_mac = ?;";

        try {
            ps =                                            connection.prepareStatement(query);

            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setString(3, mac);
            ps.setString(4, token);
            ps.setString(5, mac);
//            ps.setString(5, mac);
//            ps.setInt(6, userId);

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
