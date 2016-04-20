package ru.korshun.cobagsmserver;


import java.sql.Connection;
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




    /**
     *  Функция выдергивает из пришедшей строки с блока код события и номер радиоканала
     * @param inStr             - входящая строка вида 7912123456789=4061/B8 #13B1
     * @return                  - возвращается массив типа {кодсобытия, радиоканал}
     */
    protected String[] parseStrForPush(String inStr) {

        return new String[]{//"7912123456789=4061/B8 #13B1"
                inStr.substring(inStr.indexOf("/") + 1, inStr.indexOf("#") - 1),
                inStr.substring(inStr.indexOf("=") + 1, inStr.indexOf("/"))
        };

    }




    /**
     *  Функция декодирует входящую строку, полученную из БД
     * @param str               - строка для декодирования
     * @return                  - возвращается декодированная строка
     */
    protected String decodeStr(String str) {
        if(str != null && str.length() > 0) {
            String decodeString =                           "",
                    helpStr =                               "";
            for (int x = 0; x < str.length(); x++) {
                if (!Character.toString(str.charAt(x)).equals(Main.getLoader().getSettingsInstance().getWORDS_DIVIDER())) {
                    helpStr +=                              Character.toString(str.charAt(x));
                } else {
                    decodeString +=                         Main.getLoader().getSettingsInstance().getWORDS_LIST()
                                                                .get(Integer.parseInt(helpStr));
                    helpStr =                               "";
                }
            }
            return decodeString;
        }
        return null;
    }




}
