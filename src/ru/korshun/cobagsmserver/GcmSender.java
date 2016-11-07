package ru.korshun.cobagsmserver;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// NOTE:
// This class emulates a server for the purposes of this sample,
// but it's not meant to serve as an example for a production app server.
// This class should also not be included in the client (Android) application
// since it includes the server's API key. For information on GCM server
// implementation see: https://developers.google.com/cloud-messaging/server
class GcmSender {

//    AIzaSyAW5TzeOd6OWCUQNmstlpIrSlZADhA2cLo


//    private final String API_KEY = "AIzaSyAW5TzeOd6OWCUQNmstlpIrSlZADhA2cLo";
    private final String                API_KEY =           "AIzaSyAHCK8hzu5lBVLd2XU2PB59-3tdu9PznIQ";

    private final int                   TIME_TO_LIVE =      3;// в часах
    private final String                NOTIFICATION_ICON = "ic_notification";
    private final String                NOTIFICATION_SOUND =
                                                            "default";

    private final String                PRIORITY_HIGH =     "high";
    private final String                PRIORITY_NORMAL =   "normal";





    public int send(String msg, String event, String token, String objectNumber, String[] data) {

//        System.out.println(str + " " + token)

        try {
            // Prepare JSON containing the GCM message content. What to send and where to send.
            JSONObject jGcmData =                           new JSONObject();
            JSONObject jData =                              new JSONObject();
            JSONObject jNotification =                      new JSONObject();

            jData.put("message",                            msg.trim());
            jData.put("title",                              event.trim());
            jData.put("object_number",                      objectNumber.trim());
            jData.put("event",                              data[0].trim());
            jData.put("channel",                            data[1].trim());

            jNotification.put("body",                       msg.trim());
            jNotification.put("title",                      event.trim());
            jNotification.put("icon",                       NOTIFICATION_ICON);
            jNotification.put("sound",                      NOTIFICATION_SOUND);

            jGcmData.put("data",                            jData);
//            jGcmData.put("notification",                    jNotification);
            jGcmData.put("to",                              token.trim());
            jGcmData.put("priority",                        PRIORITY_HIGH);
            jGcmData.put("time_to_live",                    60 * 60 * TIME_TO_LIVE);

            // Create connection to send GCM Message request.
//            URL url = new URL("https://android.googleapis.com/gcm/send");

            URL url =                                       new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn =                        (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization",        "key=" + API_KEY);
            conn.setRequestProperty("Content-Type",         "application/json;charset=UTF-8");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Send GCM message content.
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(jGcmData.toString().getBytes("UTF-8"));

            // Read GCM response.
            InputStream inputStream =                       conn.getInputStream();
            String resp =                                   IOUtils.toString(inputStream);
            System.out.println(resp);

            JSONObject response =                           new JSONObject(resp);

            return (int)response.get("success");
//            System.out.println(response.get("success"));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERROR!!!! " + e.getMessage());
            return 0;
        }
    }

















//fjhYeY80WOQ:APA91bEJU453nuPRIgGKieJs0J5OkR6h69ngzkznqW44BP8iFsbzlllN_8vbB66vVuhpZb8nnESNmtRKcWchjazTVVHDL05c8Dc9ZrLGIJtKEmJfqkXtOB7A8wOjb8MsLAJe0EHY0qhN
//    private static String TOKEN_G3 =
//"fzj_zGtUHEE:APA91bFUcLcpoNsqQ3Id2ldmsGL-mFGga9ExfNOFPdZBXhJp12roindMPNvbTroJXB7jk6LX-guSzdHk2lIJpVqawm1iqFfDKMSpwlkH2n2RpdVowaFJzt4aI-7-keqWNQZs1tobp0_J";
////    private final String TOKEN_G3 =
////"cQU6QCSw4oE:APA91bG_k2isGUJFlZYqzJCPHy4xquLcVJcjctg4sGjLP3KOf19lHIn42NNscKWC-FLCQdOEyzr7kAEHUmwk2eymfVawzxbIczfTICev71ep6lte62ZdjjUFpfoiSTPBGDJn81NdFW3V";
//    private final String TOKEN_LENOVO =
//"e6RoIJr_t5M:APA91bFrk_mnfns6xz-OP2Ys1zXQPsu_o9rmhq78Ri2jzJhC06FdWyGUKu-Z-Jmzqg44Vrf7pEK--2Z1LKS0Fqy5PHL2r_Gol9DL1VdQyMHS7iVFy24PaZjDKn7GD8EXeti_fWkEWVav";
//    private final String RECEIVE_STR = "7912123456789=4061/B8 #13B1";

    // 1000 - 3099 -
    // 4000 - 7000
    //

//    public void send(String str) {
//        send(str, null);
//    }
//
//    void send(String str, String token) {
//
////        System.out.println(str + " " + token)
//
//        try {
//            // Prepare JSON containing the GCM message content. What to send and where to send.
//            JSONObject jGcmData = new JSONObject();
//            JSONObject jData = new JSONObject();
//
//            jData.put("message", str.trim());
//            jGcmData.put("data", jData);
//
//            jGcmData.put("to", token != null ? token.trim() : TOKEN_G3);
////            jGcmData.put("delivery_receipt_requested", true);
////            jGcmData.put("delay_while_idle", true);
//
//
//            // Where to send GCM message.
////            if (args.length > 1 && args[1] != null) {
////                jGcmData.put("to", args[1].trim());
////                System.out.println("to");
////            } else {
////                jGcmData.put("to", "/topics/global");
////                System.out.println("all");
////            }
//            // What to send in GCM message.
//
//
////            System.out.println(jGcmData.toString());
//
//            // Create connection to send GCM Message request.
//            URL url = new URL("https://android.googleapis.com/gcm/send");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestProperty("Authorization", "key=" + API_KEY);
//            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
//            conn.setRequestMethod("POST");
//            conn.setDoOutput(true);
//
//            // Send GCM message content.
//            OutputStream outputStream = conn.getOutputStream();
//            outputStream.write(jGcmData.toString().getBytes("UTF-8"));
//
//            // Read GCM response.
//            InputStream inputStream = conn.getInputStream();
//            String resp = IOUtils.toString(inputStream);
//            System.out.println(resp);
//
//            JSONObject response = new JSONObject(resp);
//
////            System.out.println(response.get("success"));
////            System.out.println("Check your device/emulator for notification or logcat for " +
////                    "confirmation of the receipt of the GCM message.");
//        } catch (IOException e) {
////            System.out.println("Unable to send GCM message.");
////            System.out.println("Please ensure that API_KEY has been replaced by the server " +
////                    "API key, and that the device's registration TOKEN_G3 is correct (if specified).");
////            e.printStackTrace();
//            System.out.println("ERROR!!!! " + e.getMessage());
//        }
//    }
//
//
//
////    {
////        "delivery_receipt_requested":true,
////        "data":
////                {"message":"Если читаешь - иди сюда!"},
////        "to":"e6RoIJr_t5M:APA91bFrk_mnfns6xz-OP2Ys1zXQPsu_o9rmhq78Ri2jzJhC06FdWyGUKu-Z-Jmzqg44Vrf7pEK--2Z1LKS0Fqy5PHL2r_Gol9DL1VdQyMHS7iVFy24PaZjDKn7GD8EXeti_fWkEWVav"
////    }

}
