package com.example.myapplication;

import android.util.Log;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PushNotificationHelper {

    private static final String ONESIGNAL_APP_ID = "29f0915e-c086-46c3-ab13-15c5d387e90c";

    // TODO: מחקי את המילים באנגלית והדביקי פה את ה- REST API Key מהאתר של OneSignal!
    private static final String REST_API_KEY = "os_v2_app_fhyjcxwaqzdmhkytcxc5hb7jbqa6d5iemzqeeneiuzdyvw4432lvjetj4u7ptu45urvvftd3urjwly7ux2fgw4lddzlinholivnfeuq";

    public static void sendNotification(String targetUserId, String title, String message) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL("https://onesignal.com/api/v1/notifications");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setUseCaches(false);
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Authorization", "Basic " + REST_API_KEY);
                con.setRequestMethod("POST");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("app_id", ONESIGNAL_APP_ID);
                jsonBody.put("target_channel", "push");

                // למי לשלוח? (לפי ה-UID מפיירבייס)
                JSONObject includeAliases = new JSONObject();
                includeAliases.put("external_id", new org.json.JSONArray().put(targetUserId));
                jsonBody.put("include_aliases", includeAliases);

                // כותרת
                JSONObject headings = new JSONObject();
                headings.put("he", title);
                headings.put("en", title);
                jsonBody.put("headings", headings);

                // תוכן ההודעה
                JSONObject contents = new JSONObject();
                contents.put("he", message);
                contents.put("en", message);
                jsonBody.put("contents", contents);

                OutputStream os = con.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = con.getResponseCode();
                Log.d("OneSignalPush", "Response Code: " + responseCode);

            } catch (Exception e) {
                Log.e("OneSignalPush", "Error sending push notification", e);
            }
        });
    }
}