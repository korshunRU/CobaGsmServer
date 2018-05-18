package ru.korshun.cobagsmserver;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
     * @param inStr             - входящая строка вида 7912123456789=4061/B8 дфаовдлавыдал
     * @return                  - возвращается массив типа {кодсобытия, радиоканал, телефон_блока}
     */
    protected String[] parseStrForPush(String inStr) {

        String tmp = inStr.substring(inStr.indexOf("=") + 1, inStr.indexOf("/"));

        return new String[]{
                inStr.substring(inStr.indexOf("/") + 1, inStr.indexOf(" ")),
                (inStr.contains("$vvk$")) ? getChannelFromObjectNumber(tmp) : tmp,
                inStr.substring(0, 11)
        };

    }


    /**
     *  В случае прихода сигнала с кодом вирутальной кнопки (с моб. устройства), выдергиваем радиоканал
     *  пришедшего объекта
     * @param objectNumber      - объект, для которого надо достать радиоканал
     * @return                  - возвращается HEX код радиоканала
     */
    private String getChannelFromObjectNumber(String objectNumber) {

        Connection connection = createConnect();
        PreparedStatement ps;
        ResultSet rs;
        String channel = null;

//        System.out.println(text);

        String query = "SELECT " + tablePrefix + "numbers_hex.r_code AS `r_code` " +
                "FROM " + tablePrefix + "numbers_hex " +
                "WHERE " + tablePrefix + "numbers_hex.number = ?;";

        try {

            ps = connection.prepareStatement(query);
            ps.setString(1, objectNumber);
            rs = ps.executeQuery();

            if (rs.next()) {
                channel = rs.getString("r_code");
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

        return channel;
    }


    /**
     *   Кодирование (шифрование) строки
     * @param str               - строка для кодирования
     * @return
     */
    public static String encodeStr(String str) {
        if(str != null && str.length() > 0) {
            String encodeString =                               "";
            for (int x = 0; x < str.length(); x++) {
                encodeString +=                                 Main.getLoader().getSettingsInstance().getWORDS_LIST()
                        .indexOf(Character.toString(str.charAt(x))) +
                        Main.getLoader().getSettingsInstance().getWORDS_DIVIDER();
            }
            return encodeString;
        }
        return null;
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
