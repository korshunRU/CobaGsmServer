package ru.korshun.cobagsmserver;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class ConnectThread {

    protected final int                 STATUS_COMPLITE =   1;
    protected final int                 STATUS_ERROR =      -1;
    protected final String              tablePrefix =       Main.getLoader().getSettingsInstance().getDB_TABLE_PREFIX();


    /**
     *  Получаем значение текущей даты вместе с временем
     * @return              - дата и время в формате "dd.MM.yyyy HH:mm:ss"
     */
    protected String getCurrentDateAndTime() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
    }




    /**
     *  Получаем значение текущей даты вместе с временем В ФОРМАТЕ БД MYSQL
     * @return              - дата и время в формате "yyyy-MM-dd HH:mm:ss"
     */
    protected String getCurrentDateAndTimeForMySQL() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }




    /**
     *  Что бы не дублировать код, в этой функции создаем ссылку на коннект к БД
     * @return              - возвразается ссылка на объект Connection
     */
    protected Connection createConnect() {
        Connection connection;

        try {
            connection =                                    Main.getLoader().getSqlInstance().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return connection;
    }





    protected String[] parseStrForPush(String inStr) {

//        "7912123456789=4061/B8 #13B1"

//        String event = inStr.substring(inStr.indexOf("/") + 1, inStr.indexOf("#") - 1);
//        String channel = inStr.substring(inStr.indexOf("#") + 1, inStr.indexOf("#") + 5);

//        System.out.println(event + " " + channel);

//        String output = getCurrentDateAndTime() + " (5041, Космонавтов 12-123 (1 эт.)) Открытие";

        return new String[]{
                inStr.substring(inStr.indexOf("/") + 1, inStr.indexOf("#") - 1),
                inStr.substring(inStr.indexOf("#") + 1, inStr.indexOf("#") + 5)
        };

    }



}
