package com.moritzsoft.keyboard;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SetupActivity extends AppCompatActivity {

    private TextView mStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mStatusText = findViewById(R.id.status_text);
        Button btnEnable = findViewById(R.id.btn_enable);
        Button btnSelect = findViewById(R.id.btn_select);

        btnEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                startActivity(intent);
            }
        });

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        String enabledMethods = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_INPUT_METHODS);
        String packageName = getPackageName();

        if (enabledMethods != null && enabledMethods.contains(packageName)) {
            String defaultMethod = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD);
            if (defaultMethod != null && defaultMethod.contains(packageName)) {
                mStatusText.setText("✓ Moritzsoft Keyboard ist aktiv!");
                mStatusText.setTextColor(0xFF4CAF50);
            } else {
                mStatusText.setText("Tastatur aktiviert — jetzt als Standard auswählen");
                mStatusText.setTextColor(0xFFff914d);
            }
        } else {
            mStatusText.setText("Tastatur ist noch nicht aktiviert");
            mStatusText.setTextColor(0xFF808080);
        }
    }
}
