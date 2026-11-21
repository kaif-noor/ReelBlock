package com.reelblock.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    TextView statusText;
    TextView logText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.status_text);
        logText = (TextView) findViewById(R.id.log_text);
        Button settingsButton = (Button) findViewById(R.id.settings_button);
        Button refreshButton = (Button) findViewById(R.id.refresh_button);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateScreen();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScreen();
    }

    void updateScreen() {
        if (ReelBlockService.isRunning) {
            statusText.setText("Blocker is ON");
        } else {
            statusText.setText("Blocker is OFF");
        }

        String log = "";
        for (String line : ReelBlockService.logLines) {
            log = log + line + "\n";
        }
        if (log.isEmpty()) {
            log = "nothing yet";
        }
        logText.setText(log);
    }
}
