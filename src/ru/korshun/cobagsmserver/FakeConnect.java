package ru.korshun.cobagsmserver;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.TimerTask;


class FakeConnect
        extends TimerTask {

    @Override
    public void run() {

        try(Socket socket = new Socket("localhost", Main.getLoader().getSettingsInstance().getTCP_PORT());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            out.writeUTF(new JSONObject().put("type", "exit").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
