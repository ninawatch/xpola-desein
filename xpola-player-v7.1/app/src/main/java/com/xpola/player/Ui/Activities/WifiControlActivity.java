package com.xpola.player.Ui.Activities;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.xpola.player.Connections.WebServer;
import com.xpola.player.R;

import java.io.IOException;

public class WifiControlActivity extends AppCompatActivity {

    private WebServer server;
    private TextView ipAddressText;
    private Button serverToggleButton;
    private static final int PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_control);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ipAddressText = findViewById(R.id.ip_address_text);
        serverToggleButton = findViewById(R.id.server_toggle_button);

        serverToggleButton.setOnClickListener(v -> {
            if (server != null && server.isAlive()) {
                stopServer();
            } else {
                startServer();
            }
        });

        ipAddressText.setText("IP Address: " + getIpAddress() + ":" + PORT);
    }

    private void startServer() {
        try {
            server = new WebServer(this);
            server.start();
            serverToggleButton.setText("Stop Server");
            Toast.makeText(this, "Server started", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start server", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
            serverToggleButton.setText("Start Server");
            Toast.makeText(this, "Server stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private String getIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }
}
