package com.kodimanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ScreenReceiver extends BroadcastReceiver {

    private static final String TAG = "KodiManager";
    static final String KODI_URL = "http://kodiMami:BeatrizAlc@localhost:8080/jsonrpc";
    private static final String KODI_PACKAGE = "org.xbmc.kodi";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            Log.d(TAG, "Pantalla encendida -> lanzando Kodi");
            launchKodi(context);

        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Log.d(TAG, "Pantalla apagada -> verificando Kodi");
            // Ejecutar en hilo separado para no bloquear el receiver
            new Thread(() -> handleScreenOff(context)).start();
        }
    }

    private void handleScreenOff(Context context) {
        try {
            // Pequeña pausa para que Kodi no esté en transición
            Thread.sleep(800);

            if (isKodiPlaying()) {
                Log.d(TAG, "Kodi está reproduciendo -> deteniendo...");
                stopKodiPlayback();
                Thread.sleep(1200); // Esperar que detenga
            }

            Log.d(TAG, "Cerrando Kodi...");
            closeKodi(context);

        } catch (Exception e) {
            Log.e(TAG, "Error en handleScreenOff: " + e.getMessage());
            // Si falla la API, cerrar Kodi igual
            closeKodi(context);
        }
    }

    private boolean isKodiPlaying() {
        try {
            String request = "{\"jsonrpc\":\"2.0\",\"method\":\"Player.GetActivePlayers\",\"id\":1}";
            String response = kodiRequest(request);
            // Si hay algún player activo, el array result no estará vacío
            return response != null && !response.contains("\"result\":[]");
        } catch (Exception e) {
            Log.e(TAG, "Error consultando estado: " + e.getMessage());
            return false;
        }
    }

    private void stopKodiPlayback() {
        try {
            // Primero obtenemos el playerid activo
            String getPlayers = "{\"jsonrpc\":\"2.0\",\"method\":\"Player.GetActivePlayers\",\"id\":1}";
            String response = kodiRequest(getPlayers);

            // Extraer playerid (0=video, 1=audio, 2=imagen)
            int playerId = 0;
            if (response != null && response.contains("\"playerid\":1")) playerId = 1;
            if (response != null && response.contains("\"playerid\":2")) playerId = 2;

            String stopRequest = "{\"jsonrpc\":\"2.0\",\"method\":\"Player.Stop\","
                + "\"params\":{\"playerid\":" + playerId + "},\"id\":2}";
            kodiRequest(stopRequest);
            Log.d(TAG, "Reproducción detenida (playerid=" + playerId + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error deteniendo reproducción: " + e.getMessage());
        }
    }

    private String kodiRequest(String jsonBody) throws Exception {
        URL url = new URL(KODI_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.close();

        int code = conn.getResponseCode();
        if (code == 200) {
            Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) sb.append(sc.nextLine());
            sc.close();
            return sb.toString();
        }
        return null;
    }

    private void launchKodi(Context context) {
        try {
            Intent launch = context.getPackageManager()
                .getLaunchIntentForPackage(KODI_PACKAGE);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launch);
                Log.d(TAG, "Kodi lanzado correctamente");
            } else {
                Log.w(TAG, "Kodi no encontrado en el sistema");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error lanzando Kodi: " + e.getMessage());
        }
    }

    private void closeKodi(Context context) {
        try {
            // Intent para cerrar Kodi limpiamente
            Intent close = new Intent(Intent.ACTION_MAIN);
            close.addCategory(Intent.CATEGORY_HOME);
            close.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(close);

            // Pequeña pausa y luego matar el proceso
            Thread.sleep(500);

            android.app.ActivityManager am = (android.app.ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);

            // Forzar cierre (funciona sin root en versiones anteriores)
            am.killBackgroundProcesses(KODI_PACKAGE);
            Log.d(TAG, "Kodi cerrado");

        } catch (Exception e) {
            Log.e(TAG, "Error cerrando Kodi: " + e.getMessage());
        }
    }
}
