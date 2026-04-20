package com.kodimanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent(this, KodiManagerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        new Thread(() -> {
            final String mensaje = probarConexion();
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                finish();
            });
        }).start();
    }

    private String probarConexion() {
        try {
            String body = "{\"jsonrpc\":\"2.0\",\"method\":\"JSONRPC.Ping\",\"id\":1}";
            URL url = new URL(ScreenReceiver.KODI_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.getBytes("UTF-8"));

            int code = conn.getResponseCode();
            if (code == 200) {
                return "KodiManager activo - Kodi responde correctamente";
            } else {
                return "Kodi respondio con codigo " + code + " - revisa usuario/contrasena";
            }
        } catch (java.net.ConnectException e) {
            return "No se pudo conectar a Kodi - esta abierto? puerto 8080?";
        } catch (java.net.SocketTimeoutException e) {
            return "Kodi no responde - tiempo de espera agotado";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
