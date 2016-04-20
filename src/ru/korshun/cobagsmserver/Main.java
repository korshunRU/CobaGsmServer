package ru.korshun.cobagsmserver;


import java.sql.Connection;
import java.sql.SQLException;

@SuppressWarnings("FieldCanBeLocal")
public class Main {


//    private static boolean              isError =           false;
    private static Loader               loader;

    static {
        loader =                                            new Loader();
    }


    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {

        checkConnectionToMySql();
        new ConnectListener(ConnectListener.TCP_CONNECT_LISTENER).start();
        new ConnectListener(ConnectListener.UDP_CONNECT_LISTENER).start();

//        new GcmSender().send("ENTER");
//        SystemTrayIcon.createTrayIcon();

//        ExecutorService executorService =                   null;
//        Loader loader =                                     new Loader();

//        new Timer().schedule(new FakeConnect(), 0, 100);


//        while(true) {
//
//            try (ServerSocket serverSocket = new ServerSocket(getLoader().getSettingsInstance().getTCP_PORT())) {
//
//                executorService =                           Executors.newCachedThreadPool();
//
//                System.out.println("Сервер запущен");
//
//                while (true) {
//
//                    if (isError) {
//                        System.out.println("ОШИБКА! ПЕРЕЗАПУСК!");
//                        isError =                           false;
//                        break;
//                    }
//
//                    executorService.submit(new ClientConnectThread(serverSocket.accept()));
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                break;
//            } finally {
//                if (executorService != null) {
//                    executorService.shutdown();
//                }
//            }
//
//        }

    }


    /**
     *  Проверка доступности БД при запуске сервера
     */
    private static void checkConnectionToMySql() {
        Connection connection;
        try {
            connection =                                    Main.getLoader().getSqlInstance().getConnection();
            System.out.println("Соединение с БД работает");
            Main.getLoader().getSqlInstance().disconnectionFromSql(connection);
        } catch (SQLException e) {
            System.out.println("Ошибка при соединении с БД!!!! " + e.getMessage());
        }
    }

    public static Loader getLoader() {
        return loader;
    }
}