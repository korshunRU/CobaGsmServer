package ru.korshun.cobagsmserver;


@SuppressWarnings("FieldCanBeLocal")
class Settings {


    /**
     *   Данные для доступа к БД
     */
    private final String                HOST_SUFFIX =       "jdbc:mysql://";
    private final String                HOST =              "localhost:3306/";
    private final String                DATABASE_NAME =     "coba_web_room";
    private final String                USERNAME =          "coba";
    private final String                PASSWORD =          "coba";
//    private final String                PASSWORD =          ",fhvfktq82";
    private final String                DB_TABLE_PREFIX =   "coba_";


    /**
     *  Порт, который "слушает" TCP сервер
     */
    private final int                   TCP_PORT =          7777;


    /**
     *  Порт, который "слушает" UPD сервер
     */
    private final int                   UDP_PORT =          8888;


    /**
     *  Максимальное число одновременных коннектов по всем протоколам
     */
    private final int                   MAX_CONNECT_COUNT = 15;



    Settings() { }

    String getDATABASE_NAME() {
        return DATABASE_NAME;
    }

    String getDB_TABLE_PREFIX() {
        return DB_TABLE_PREFIX;
    }

    String getHOST() {
        return HOST;
    }

    String getHOST_SUFFIX() {
        return HOST_SUFFIX;
    }

    String getPASSWORD() {
        return PASSWORD;
    }

    String getUSERNAME() {
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

}
