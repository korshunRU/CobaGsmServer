package ru.korshun.cobagsmserver;


import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



@SuppressWarnings("InfiniteLoopStatement")
public class ConnectListener
        extends Thread {

    public static final int             TCP_CONNECT_LISTENER =
                                                            1;
    public static final int             UDP_CONNECT_LISTENER =
                                                            2;

    private ExecutorService             executorService =   null;

    private int connectType;

//    private static boolean              isError =           false;

    public ConnectListener(int connectType) {
        this.connectType =                                  connectType;
        executorService =                                   Executors.newFixedThreadPool(
                                                                    Main.getLoader().getSettingsInstance()
                                                                            .getMAX_CONNECT_COUNT()
                                                            );
    }

    @Override
    public void run() {
//        super.run();

        switch (connectType) {

            case TCP_CONNECT_LISTENER:
                startTcpListener();
                break;

            case UDP_CONNECT_LISTENER:
                startUdpListener();
                break;

        }

    }



    private void startTcpListener() {
//        while(true) {

            try (ServerSocket serverSocket = new ServerSocket(Main.getLoader().getSettingsInstance().getTCP_PORT())) {

                System.out.println("Сервер запущен: TCP");

                while (true) {

//                    if (isError) {
//                        System.out.println("ОШИБКА! ПЕРЕЗАПУСК!");
//                        isError =                           false;
//                        break;
//                    }

                    executorService.submit(new ClientConnectThread(serverSocket.accept()));
                }

            } catch (IOException e) {
                e.printStackTrace();
//                break;
            } finally {
                if (executorService != null) {
                    executorService.shutdown();
                }
            }

//        }

    }

    private void startUdpListener() {
        System.out.println("Сервер запущен: UDP");

//        DatagramSocket serverSocket =                       null;
        try(DatagramSocket serverSocket = new DatagramSocket(Main.getLoader().getSettingsInstance().getUDP_PORT())) {

//            serverSocket =                                  new DatagramSocket(Main.getLoader().getSettingsInstance()
//                                                                                    .getUDP_PORT());
            byte[] receiveData =                            new byte[32];

            while(true) {
//
                DatagramPacket receivePacket =              new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
//
                executorService.submit(new DeviceConnectThread(receivePacket));
//
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }

    }

}
