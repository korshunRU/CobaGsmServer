package ru.korshun.cobagsmserver;


import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("FieldCanBeLocal")
class Settings {


    /**
     *   Данные для доступа к БД
     */
    private final String                HOST_SUFFIX =           "jdbc:mysql://";
    private final String                HOST =                  "localhost:3306/";
    private final String                DATABASE_NAME =         "coba_web_room";
    private final String                USERNAME =              "coba";
    private final String                PASSWORD =              "coba";
//    private final String                PASSWORD =          ",fhvfktq82";
    private final String                DB_TABLE_PREFIX =       "coba_";


    /**
     *  Порт, который "слушает" TCP сервер
     */
    private final int                   TCP_PORT =              7777;


    /**
     *  Порт, который "слушает" UPD сервер
     */
    private final int                   UDP_PORT =              8888;


    /**
     *  Максимальное число одновременных коннектов по всем протоколам
     */
    private final int                   MAX_CONNECT_COUNT =     30;


    /**
     *  Количество выгружаемых из БД сигналов по умолчанию, если данное кол-во не пришло с клиента
     */
    private final int                   ITEMS_COUNT_DEFAULT =   100;


    /**
     *   Массив символов для кодирования\декодирования
     */
    private final ArrayList<String>     WORDS_LIST =            new ArrayList<>(Arrays.asList(
            "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "и", "й", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч", "ш", "щ", "ъ", "ы", "ь", "э", "ю", "я",
            "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л", "М", "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ы", "Ь", "Э", "Ю", "Я",
            "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a", "s", "d", "f", "g", "h", "j", "k", "l", "z", "x", "c", "v", "b", "n", "m",
            "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "A", "S", "D", "F", "G", "H", "J", "K", "L", "Z", "X", "C", "V", "B", "N", "M",
            " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "-", "_", "(", ")", "\"", "/", ",", ".", ":", ";", "'", "!", "=", "+", "\\", "@", "#", "№", "$", "%", "^", "&", "?", "*", "~", "`"));


    /**
     *   Разделитель символов при кодировании
     */
    private final String                WORDS_DIVIDER =         "$";



    /**
     *   Папка, где будем хранить log файлы
     */
    private final String                LOG_PATH =              "logs";



    public Settings() { }

    public String getDATABASE_NAME() {
        return DATABASE_NAME;
    }

    public String getDB_TABLE_PREFIX() {
        return DB_TABLE_PREFIX;
    }

    public String getHOST() {
        return HOST;
    }

    public String getHOST_SUFFIX() {
        return HOST_SUFFIX;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }

    public String getUSERNAME() {
        return USERNAME;
    }

    int getTCP_PORT() {
        return TCP_PORT;
    }

    public int getUDP_PORT() {
        return UDP_PORT;
    }

    public int getMAX_CONNECT_COUNT() {
        return MAX_CONNECT_COUNT;
    }

    public String getWORDS_DIVIDER() {
        return WORDS_DIVIDER;
    }

    public ArrayList<String> getWORDS_LIST() {
        return WORDS_LIST;
    }

    public int getITEMS_COUNT_DEFAULT() {
        return ITEMS_COUNT_DEFAULT;
    }

    public String getLOG_PATH() {
        return LOG_PATH;
    }
}
