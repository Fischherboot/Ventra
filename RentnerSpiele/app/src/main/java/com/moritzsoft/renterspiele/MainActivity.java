package com.moritzsoft.renterspiele;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Vibrator vibrator;

    private static final String[][] GAMES = {
            {"Sudoku", "Zahlen eintragen"},
            {"Memory", "Paare finden"},
            {"Wortfinder", "Wörter suchen"},
            {"Buchstaben\nZuweisen", "Zahlen entschlüsseln"},
            {"Dame", "Steine schlagen"},
            {"Schach", "Figuren klug ziehen"},
            {"Halma", "Figuren ins Ziel"},
            {"Mensch ärgere\ndich nicht", "Würfeln und ziehen"},
            {"Solitär", "Stifte überspringen"},
    };

    private static final Class<?>[] GAME_ACTIVITIES = {
            SudokuActivity.class,
            MemoryActivity.class,
            WortfinderActivity.class,
            BuchstabenActivity.class,
            DameActivity.class,
            SchachActivity.class,
            HalmaActivity.class,
            MenschActivity.class,
            SolitaerActivity.class,
    };

    private static final String[] GAME_ICONS = {
            "🔢", "🃏", "🔤", "🔠", "⬛",
            "♟", "⭐", "🎲", "📍",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_main);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Button btnHelp = findViewById(R.id.btnHelp);
        btnHelp.setOnClickListener(v -> { vibrate(); showHelpDialog(); });
        buildTileGrid();
    }

    private void buildTileGrid() {
        GridLayout grid = findViewById(R.id.tileGrid);
        grid.removeAllViews();
        grid.setColumnCount(5);
        grid.setRowCount(2);

        int margin = dpToPx(8);
        for (int i = 0; i < GAMES.length; i++) {
            final int index = i;
            boolean enabled = GAME_ACTIVITIES[i] != null;

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(getDrawable(enabled ? R.drawable.btn_tile : R.drawable.btn_tile_disabled));
            tile.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / 5, 1f);
            params.columnSpec = GridLayout.spec(i % 5, 1f);
            params.width = 0;
            params.height = 0;
            params.setMargins(margin, margin, margin, margin);
            tile.setLayoutParams(params);

            TextView icon = new TextView(this);
            icon.setText(GAME_ICONS[i]);
            icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            icon.setGravity(Gravity.CENTER);
            tile.addView(icon);

            TextView title = new TextView(this);
            title.setText(GAMES[i][0]);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            title.setTextColor(enabled ? 0xFF1A1A1A : 0xFF888888);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, dpToPx(6), 0, dpToPx(2));
            tile.addView(title);

            TextView subtitle = new TextView(this);
            subtitle.setText(GAMES[i][1]);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            subtitle.setTextColor(enabled ? 0xFF555555 : 0xFFAAAAAA);
            subtitle.setGravity(Gravity.CENTER);
            tile.addView(subtitle);

            if (enabled) {
                tile.setOnClickListener(v -> {
                    vibrate();
                    startActivity(new Intent(this, GAME_ACTIVITIES[index]));
                });
            }
            grid.addView(tile);
        }
    }

    private void showHelpDialog() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(24);
        layout.setPadding(pad, pad, pad, pad);

        addHelpSection(layout, getString(R.string.help_sudoku_title), getString(R.string.help_sudoku_text));
        addHelpSection(layout, getString(R.string.help_memory_title), getString(R.string.help_memory_text));
        addHelpSection(layout, getString(R.string.help_wortfinder_title), getString(R.string.help_wortfinder_text));
        addHelpSection(layout, getString(R.string.help_buchstaben_title), getString(R.string.help_buchstaben_text));
        addHelpSection(layout, getString(R.string.help_dame_title), getString(R.string.help_dame_text));
        addHelpSection(layout, getString(R.string.help_schach_title), getString(R.string.help_schach_text));
        addHelpSection(layout, getString(R.string.help_halma_title), getString(R.string.help_halma_text));
        addHelpSection(layout, getString(R.string.help_mensch_title), getString(R.string.help_mensch_text));
        addHelpSection(layout, getString(R.string.help_solitaer_title), getString(R.string.help_solitaer_text));

        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.help_title))
                .setView(scrollView)
                .setPositiveButton(getString(R.string.btn_understood), null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(22);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void addHelpSection(LinearLayout parent, String title, String text) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(26);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setPadding(0, dpToPx(16), 0, dpToPx(8));
        titleView.setTypeface(null, Typeface.BOLD);
        parent.addView(titleView);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(20);
        textView.setTextColor(0xFFFFFFFF);
        textView.setLineSpacing(dpToPx(4), 1.0f);
        textView.setPadding(0, 0, 0, dpToPx(16));
        parent.addView(textView);

        View separator = new View(this);
        separator.setBackgroundColor(0xFFAAAAAA);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        separator.setLayoutParams(lp);
        parent.addView(separator);
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() { super.onResume(); hideSystemUI(); }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
