package ru.korshun.cobagsmserver;


import com.mkyong.asymmetric.CryptRSA;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


@SuppressWarnings("FieldCanBeLocal")
public class ClientConnectThread
        extends ConnectThread
        implements Runnable {

    private Socket                      socket;

    private String                      outputStr =         "";

    private boolean                     checkCommand =      true;
    private boolean                     checkIP =           false;

    private final int                   VERSION =           8;

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





                    // вход в приложение
                    case "enter":
                        proccessUserLogin(data, out);
                        break;






                    // отправка обновленного токена
                    case "sendToken":
                        updateUserData(data.getString("token"), data.getInt("userId"), data.getString("mac"), null, out);
                        break;






                    // запрос на получение списка объектов
                    case "getObjectsList":
                        getObjectsListFromMySql(data, out);
                        break;






                    // запрос на получение списка сигналов
                    case "getSignalsList":
                        getSignalsListFromMySql(data, out);
                        break;




                    // отправка вопроса
                    case "sendQueryToOffice":
                        sendQueryToOffice(data, out);
                        break;



                    // загрузка "опций"
                    case "getOptionsList":
                        getOptionsList(data, out);
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
                                                                Main.getLoader().getSettingsInstance().getSIGNAL_ITEMS_COUNT_DEFAULT();
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

                signal.put("status", rs.getString("status"));
                signal.put("date", date);
                signal.put("time", time);
                signal.put("event", event);
//                signal.put("phone", phone);

                array.put(signal);

            }

            signals.put("signals", array);

            query = "SELECT " + tablePrefix + "objects.name AS `object_name`, " +
                            tablePrefix + "objects.address AS `object_address` " +
                    "FROM " + tablePrefix + "objects " +
                    "WHERE " + tablePrefix + "objects.id_client = ? " +
                        "AND " + tablePrefix + "objects.number = ?;";

            ps =                                            connection.prepareStatement(query);

            ps.setInt(1, userId);
            ps.setInt(2, objectNumber);

            rs =                                            ps.executeQuery();

            String objectName = "-";
            String objectAddress = "-";

            if(rs.next()) {
                objectName =                                rs.getString("object_name");
                objectAddress =                             rs.getString("object_address");
            }

            signals.put("objectName", objectName == null ? "-" : decodeStr(objectName).trim());
            signals.put("objectAddress", objectAddress == null ? "-" : decodeStr(objectAddress).trim());

            sendOperationStatusToClient(out, STATUS_COMPLITE, signals);

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
                                    tablePrefix + "users.alert_btn_enabled AS `alert_button_status`, " +
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
            String alertBtnStatus = "0";

            while (rs.next()) {

                String number =                             rs.getString("number");
                String type =                               rs.getString("type");
                String address =                            rs.getString("address");
                userName =                                  rs.getString("user_name");
                userAddress =                               rs.getString("user_address");
                userPhone =                                 rs.getString("user_phone");
                String objectPhone =                        rs.getString("phone");
                alertBtnStatus =                            rs.getString("alert_button_status");

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
            objects.put("alertBtnStatus", alertBtnStatus);
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
     *  Функция добавляет в БД вопрос пользователя и отправляет его на e-mail тому, кто этот вопрос будет обрабатывать
     * @param data              - json c userId клиента
     * @param out               - ссылка на DataOutputStream для отправки сообщения клиенту
     * @throws IOException
     */
    private void sendQueryToOffice(JSONObject data, DataOutputStream out) throws IOException {

        Connection connection =                             createConnect();
        PreparedStatement ps;
        ResultSet rs;

        JSONObject clientsQueries;

        int lastId =                                        0;

        String userMail = null, userName = null, objects = "";
        StringBuilder sb = new StringBuilder();

        if(connection == null) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Сервер БД недоступен");
            return;
        }

        int userId =                                        data.getInt("userId");

        if(userId == 0) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Неверный идентификатор пользователя");
            return;
        }

        String theme =                                      data.has("queryTheme") ? data.getString("queryTheme") : null;
        String text =                                       data.has("queryText") ? data.getString("queryText") : null;



        // Если параметры темы и текста не существуют, вытаскиваем все запросы пользователя и отправляем их на клиента
        if(theme == null || text == null) {
            clientsQueries = getClientQueries(userId);

            if(clientsQueries == null) {
                sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка при запросе данных");
            }
            else {
                sendOperationStatusToClient(out, STATUS_COMPLITE, clientsQueries);
            }

        }




        // Если параметры темы и текста существуют - добавляем запрос в БД
        else {

            String query =  "INSERT INTO " + tablePrefix + "feedback_mobile " +
                            "SET " +    tablePrefix + "feedback_mobile.id_user = ?, " +
                                        tablePrefix + "feedback_mobile.date = ?, " +
                                        tablePrefix + "feedback_mobile.theme = ?, " +
                                        tablePrefix + "feedback_mobile.text = ?; ";

            try {
                String encodeTheme = encodeStr(theme);
                String encodeText = encodeStr(text);
                ps =                                        connection.prepareStatement(query,
                                                                PreparedStatement.RETURN_GENERATED_KEYS);

                ps.setInt(1, userId);
                ps.setString(2, getCurrentDateAndTimeForMySQL());
                ps.setString(3, encodeTheme);
                ps.setString(4, encodeText);

                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();

                if(keys.next()) {
                    lastId = keys.getInt(1);
                }

                clientsQueries = getClientQueries(userId);

                if(clientsQueries == null) {
                    sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка при запросе данных");
                }
                else {
                    sendOperationStatusToClient(out, STATUS_COMPLITE, clientsQueries);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка формирования запроса");
                return;
//            System.out.println(getCurrentDateAndTime() + ": " + e.getMessage());
            } finally {
                try {
                    Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }



            // Вытаскиваем данные пользователя, что бы отправить их в письме
            connection = createConnect();

            query = "SELECT " + tablePrefix + "users.login AS `login`, " +
                                tablePrefix + "users.name AS `name`, " +
                                tablePrefix + "objects.number AS `objectNumber`, " +
                                tablePrefix + "objects.address AS `objectAddress` " +
                    "FROM " + tablePrefix + "objects " +
                    "LEFT JOIN " + tablePrefix + "users ON " + tablePrefix + "users.id = ? " +
                    "WHERE " + tablePrefix + "objects.id_client = ?";

            try {
                ps = connection.prepareStatement(query);

                ps.setInt(1, userId);
                ps.setInt(2, userId);

                rs = ps.executeQuery();

                while(rs.next()) {
                    userMail = rs.getString("login");
                    userName = decodeStr(rs.getString("name"));

                    objects = sb.append(String.format("%s (%s)</br>",
                            rs.getString("objectNumber"), decodeStr(rs.getString("objectAddress")))).toString();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка при запросе данных");
                return;
            } finally {
                try {
                    Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            sb = new StringBuilder();

            String body = sb.append(String.format("<b>Клиент</b>: %s</br><b>Объекты</b>:</br>%s</br>" +
                    "<b>Текст обращения</b>:</br>", userName, objects)).append(text).toString();

            String[] receivers = getQueryReceivers(theme);
            int errors = 0;

            // Отправляем запрос клиента всем тем, кто должен его получить в зависимости от категории
            for(String receiver : receivers) {
                try {
                    sendMail(receiver, body, userMail);
                } catch (MessagingException e) {
                    e.printStackTrace();
                    errors++;
                }
            }

            // Тут проверяем, были ли ошибки при отправке. Если хоть одно письмо ушло, то ставим пометку в БД
            // что запрос обработан
            if(errors < receivers.length) {
                connection = createConnect();

                query = "UPDATE " + tablePrefix + "feedback_mobile " +
                        "SET " + tablePrefix + "feedback_mobile.status = ? " +
                        "WHERE " + tablePrefix + "feedback_mobile.id = ?;";

                try {
                    ps = connection.prepareStatement(query);

                    ps.setInt(1, 1);
                    ps.setInt(2, lastId);

                    ps.executeUpdate();

                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }


//            System.out.println(getCurrentDateAndTime() + ": Добавление обращения, тема: " + theme + ", " +
//                    "текст: " + text);
        }

//        sendOperationStatusToClient(out, STATUS_COMPLITE, clientsQueries);

    }




    /**
     *  Функция лезет в БД и получает список доп. возможностей
     * @param data              - json c userId и номером объекта клиента
     * @param out               - ссылка на DataOutputStream для отправки сообщения клиенту
     * @throws IOException
     */
    private void getOptionsList(JSONObject data, DataOutputStream out) throws IOException {

        JSONObject clientsQueries = new JSONObject();
        JSONArray items = new JSONArray();

        JSONObject item1 = new JSONObject();
        JSONObject item2 = new JSONObject();
        JSONObject item3 = new JSONObject();

        JSONArray array1 = new JSONArray();
        JSONArray array2 = new JSONArray();
        JSONArray array3 = new JSONArray();

        List<String> titlesList = new ArrayList<>();

        Connection connection =                             createConnect();
        PreparedStatement ps;
        ResultSet rs;

        if(connection == null) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Сервер БД недоступен");
            return;
        }

        int userId =                                        data.getInt("userId");

        if(userId == 0) {
            sendOperationStatusToClient(out, STATUS_ERROR, "Неверный идентификатор пользователя");
            return;
        }

//        String query = "SELECT " +  tablePrefix + "options_titles.title AS `title` " +
//                        "FROM " + tablePrefix + "options_titles " +
//                        "ORDER BY " + tablePrefix + "options_titles.id ASC;";
//
//        try {
//
//            ps = connection.prepareStatement(query);
//            rs = ps.executeQuery();
//
//            while (rs.next()) {
//
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            sendOperationStatusToClient(out, STATUS_ERROR, "Ошибка при запросе данных");
//        } finally {
//            try {
//                Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }


        String query = "SELECT " +  tablePrefix + "options_titles.id AS `id`, " +
                                    tablePrefix + "options_titles.title AS `title`, " +
                                    tablePrefix + "options.text AS `text`, " +
                                    tablePrefix + "options.hidden_text AS `hidden_text` " +
                        "FROM " + tablePrefix + "options " +
                        "LEFT JOIN " + tablePrefix + "options_titles " +
                            "ON " + tablePrefix + "options.id_title = " + tablePrefix + "options_titles.id " +
                        "ORDER BY " + tablePrefix + "options_titles.id, " + tablePrefix + "options.text ASC;";

        try {
            ps = connection.prepareStatement(query);

            rs = ps.executeQuery();

            while(rs.next()) {

                int titleId =                               rs.getInt("id");
                String title =                              rs.getString("title");
                String text =                               rs.getString("text");
                String hiddenText =                         rs.getString("hidden_text");

                if(!titlesList.contains(title)) {
                    titlesList.add(title);
                }

                Map<String, String> options =               new HashMap<>();

                options.put("text", text);
                options.put("hiddenText", hiddenText);

                switch (titleId) {
                    case 1:
                        array1.put(options);
                        break;

                    case 2:
                        array2.put(options);
                        break;

                    case 3:
                        array3.put(options);
                        break;
                }

            }

            item1.put(titlesList.get(0), array1);
            item2.put(titlesList.get(1), array2);
            item3.put(titlesList.get(2), array3);

            items.put(item1);
            items.put(item2);
            items.put(item3);

            clientsQueries.put("options", items);

//            System.out.println(clientsQueries);

            sendOperationStatusToClient(out, STATUS_COMPLITE, clientsQueries);

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
         *  Функция выдергивает из БД все запросы клиента и возвращает их в виде JSONObject
         * @param userId                - ID пользователя, чьи запросы ищем
         * @return                      - возвращается JSONObject с запросами
         */
    private JSONObject getClientQueries(int userId) {
        JSONObject returnObject = new JSONObject();
        JSONArray array = new JSONArray();

        Connection connection = createConnect();
        PreparedStatement ps;
        ResultSet rs;

        String query = "SELECT " +  tablePrefix + "feedback_mobile.theme AS `theme`, " +
                                    tablePrefix + "feedback_mobile.text AS `text`, " +
                                    tablePrefix + "feedback_mobile.status AS `status`, " +
                                    "DATE_FORMAT(" +  tablePrefix + "feedback_mobile.date, '%d.%m.%Y') AS `date`, " +
                                    "DATE_FORMAT(" +  tablePrefix + "feedback_mobile.date, '%H:%i:%S') AS `time` " +
                        "FROM " + tablePrefix + "feedback_mobile " +
                        "WHERE " + tablePrefix + "feedback_mobile.id_user = ? " +
                        "ORDER BY " + tablePrefix + "feedback_mobile.date DESC " +
                        "LIMIT 0,?;";

        try {
            ps = connection.prepareStatement(query);

            ps.setInt(1, userId);
            ps.setInt(2, Main.getLoader().getSettingsInstance().getQUERIES_ITEMS_COUNT_DEFAULT());

            rs = ps.executeQuery();

            while(rs.next()) {
                String date =                               rs.getString("date");
                String time =                               rs.getString("time");
                String theme =                              decodeStr(rs.getString("theme"));
                String text =                               decodeStr(rs.getString("text"));
                String status =                             rs.getString("status");

                Map<String, String> queries =               new HashMap<>();

                queries.put("status", status);
                queries.put("date", date);
                queries.put("time", time);
                queries.put("theme", theme);
                queries.put("text", text);

                array.put(queries);
            }

            returnObject.put("queries", array);

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


        return returnObject;
    }




    /**
     * Тупая функция. В зависимости от темы выбираем получателей
     * @param theme             - тема обращен0ия
     * @return                  - возвращается массив с адресами получателей
     */
    private String[] getQueryReceivers(String theme) {

        String[] receivers;

        if(theme.contains("Договорной")) {
            receivers = Main.getLoader().getSettingsInstance().getOFFICE_RECEIVER().split(",");
        }
        else if(theme.contains("Сервисная")) {
            receivers = Main.getLoader().getSettingsInstance().getSERVICE_RECEIVER().split(",");
        }
        else if(theme.contains("разработки")) {
            receivers = Main.getLoader().getSettingsInstance().getCODERS_RECEIVER().split(",");
        }
        else {
            receivers = Main.getLoader().getSettingsInstance().getCODERS_RECEIVER().split(",");
        }

        return receivers;
    }




    /**
     *  Функция отправляет электронное письмо клиенту
     * @param address                   - email получателя
     * @param body                      - текст сообщения
     * @param replyToAddress            - адрес для обратного ответа (адрес клиента)
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    private void sendMail(String address, String body, String replyToAddress) throws UnsupportedEncodingException, MessagingException {

        String subj =                                           "\"СОВА\" - мобильное приложение";

        Properties properties =                                 new Properties();
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.host", "smtp.mail.ru");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.user", Main.getLoader().getSettingsInstance().getMAIL_LOGIN());
        properties.put("mail.password", Main.getLoader().getSettingsInstance().getMAIL_PASSWORD());

        // creates a new session with an authenticator
        Authenticator auth =                                    new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        Main.getLoader().getSettingsInstance().getMAIL_LOGIN(),
                        Main.getLoader().getSettingsInstance().getMAIL_PASSWORD());
            }
        };
        Session session =                                       Session.getInstance(properties, auth);

        // creates a new e-mail message
        MimeMessage msg =                                           new MimeMessage(session);

        msg.setFrom(new InternetAddress(Main.getLoader().getSettingsInstance().getMAIL_LOGIN(), subj, "UTF-8"));
        InternetAddress[] toAddress = { new InternetAddress(address) };
        msg.setRecipients(Message.RecipientType.TO, toAddress);
        InternetAddress[] replyTo = { new InternetAddress(replyToAddress) };
        msg.setReplyTo(replyTo);
        msg.setSubject(subj,"UTF-8");
        msg.setSentDate(new Date());

        // creates message part
        MimeBodyPart messageBodyPart =                          new MimeBodyPart();
        messageBodyPart.setContent(body, "text/html; charset=UTF-8");

        // creates multi-part
        Multipart multipart =                                   new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        // sets the multi-part as e-mail's content
        msg.setContent(multipart);

        // sends the e-mail
        Transport.send(msg);

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
