package com.moritzsoft.renterspiele;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SolitaerActivity extends AppCompatActivity {

    // English-style cross board: 7x7 with corners cut
    // 0=invalid, 1=peg, 2=empty
    private int[][] board = new int[7][7];
    private static final boolean[][] VALID = new boolean[7][7];
    static {
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 7; c++)
                VALID[r][c] = !((r < 2 || r > 4) && (c < 2 || c > 4));
    }

    private int selectedRow = -1, selectedCol = -1;
    private List<int[]> possibleMoves = new ArrayList<>();
    private Vibrator vibrator;
    private BoardView boardView;
    private TextView txtStatus, txtInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_solitaer);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        txtStatus = findViewById(R.id.txtStatus);
        txtInfo = findViewById(R.id.txtInfo);

        TextView title = findViewById(R.id.txtGameTitle);
        title.setText("Solitär");

        // Mode screen - solitaire is single player, so skip mode selection
        findViewById(R.id.btnBot).setVisibility(View.GONE);
        Button btnLocal = findViewById(R.id.btnLocal);
        btnLocal.setText("▶  Spiel starten");
        btnLocal.setOnClickListener(v -> { vibrate(30); startGame(); });
        findViewById(R.id.btnBackMode).setOnClickListener(v -> { vibrate(30); finish(); });

        findViewById(R.id.btnBack).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnNewGame).setOnClickListener(v -> { vibrate(30); startGame(); });
        findViewById(R.id.btnUndo).setVisibility(View.GONE);

        Button btnHelp = findViewById(R.id.btnHelp);
        btnHelp.setOnClickListener(v -> {
            vibrate(30);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.help_solitaer_title))
                    .setMessage(getString(R.string.help_solitaer_text))
                    .setPositiveButton(getString(R.string.btn_understood), null)
                    .show();
        });
    }

    private void startGame() {
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 7; c++)
                board[r][c] = VALID[r][c] ? 1 : 0;
        board[3][3] = 2; // center empty

        selectedRow = -1; selectedCol = -1;
        possibleMoves.clear();

        findViewById(R.id.modeScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);

        updateStatus();

        FrameLayout container = findViewById(R.id.boardContainer);
        container.removeAllViews();
        boardView = new BoardView(this);
        container.addView(boardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void onCellTapped(int row, int col) {
        if (!VALID[row][col]) return;

        if (selectedRow >= 0) {
            // Check if tapped cell is a valid move target
            for (int[] move : possibleMoves) {
                if (move[0] == row && move[1] == col) {
                    // Execute move
                    int midR = (selectedRow + row) / 2;
                    int midC = (selectedCol + col) / 2;
                    board[selectedRow][selectedCol] = 2;
                    board[midR][midC] = 2;
                    board[row][col] = 1;
                    vibrate(30);
                    selectedRow = -1; selectedCol = -1;
                    possibleMoves.clear();
                    boardView.invalidate();
                    updateStatus();
                    checkGameEnd();
                    return;
                }
            }
        }

        // Select a peg
        if (board[row][col] == 1) {
            selectedRow = row; selectedCol = col;
            possibleMoves.clear();
            // Find valid jumps
            int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
            for (int[] d : dirs) {
                int tr = row + d[0], tc = col + d[1];
                int mr = row + d[0] / 2, mc = col + d[1] / 2;
                if (tr >= 0 && tr < 7 && tc >= 0 && tc < 7 && VALID[tr][tc]
                        && board[tr][tc] == 2 && board[mr][mc] == 1) {
                    possibleMoves.add(new int[]{tr, tc});
                }
            }
            vibrate(20);
            boardView.invalidate();
        }
    }

    private void updateStatus() {
        int count = 0;
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 7; c++)
                if (board[r][c] == 1) count++;
        txtStatus.setText("Stifte: " + count);
        txtInfo.setText(count == 1 ? "Perfekt gelöst!" : "");
    }

    private void checkGameEnd() {
        // Check if any moves are possible
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (board[r][c] != 1) continue;
                int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
                for (int[] d : dirs) {
                    int tr = r + d[0], tc = c + d[1];
                    int mr = r + d[0] / 2, mc = c + d[1] / 2;
                    if (tr >= 0 && tr < 7 && tc >= 0 && tc < 7 && VALID[tr][tc]
                            && board[tr][tc] == 2 && board[mr][mc] == 1) {
                        return; // Still moves available
                    }
                }
            }
        }
        // No moves left
        int count = 0;
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 7; c++)
                if (board[r][c] == 1) count++;

        vibrate(500);
        String msg = count == 1 ? "Perfekt! Nur 1 Stift übrig!" : "Keine Züge mehr. " + count + " Stifte übrig.";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void vibrate(int ms) {
        if (vibrator != null && vibrator.hasVibrator())
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() { super.onResume(); hideSystemUI(); }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ========== Board View ==========
    private class BoardView extends View {
        private Paint pegPaint, emptyPaint, selectedPaint, movePaint, bgPaint, borderPaint, copyPaint;
        private float cellSize, offsetX, offsetY;

        public BoardView(Context ctx) {
            super(ctx);
            pegPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pegPaint.setColor(0xFF5D4037);
            pegPaint.setStyle(Paint.Style.FILL);

            emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            emptyPaint.setColor(0xFFD7CCC8);
            emptyPaint.setStyle(Paint.Style.FILL);

            selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            selectedPaint.setColor(0xFFFFEB3B);
            selectedPaint.setStyle(Paint.Style.FILL);

            movePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            movePaint.setColor(0xFFFF9800);
            movePaint.setStyle(Paint.Style.STROKE);
            movePaint.setStrokeWidth(4f);

            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(0xFFEFEBE9);
            bgPaint.setStyle(Paint.Style.FILL);

            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(0xFF8D6E63);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(3f);

            copyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            copyPaint.setColor(0x88888888);
            copyPaint.setTextSize(12f);
            copyPaint.setTextAlign(Paint.Align.RIGHT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            cellSize = Math.min(w, h) / 8f;
            offsetX = (w - cellSize * 7) / 2f;
            offsetY = (h - cellSize * 7) / 2f;

            for (int r = 0; r < 7; r++) {
                for (int c = 0; c < 7; c++) {
                    if (!VALID[r][c]) continue;
                    float cx = offsetX + c * cellSize + cellSize / 2f;
                    float cy = offsetY + r * cellSize + cellSize / 2f;
                    float radius = cellSize * 0.38f;

                    // Background circle (hole)
                    canvas.drawCircle(cx, cy, radius + 4, bgPaint);
                    canvas.drawCircle(cx, cy, radius + 4, borderPaint);

                    if (r == selectedRow && c == selectedCol) {
                        canvas.drawCircle(cx, cy, radius, selectedPaint);
                    } else if (board[r][c] == 1) {
                        canvas.drawCircle(cx, cy, radius, pegPaint);
                    } else {
                        canvas.drawCircle(cx, cy, radius * 0.4f, emptyPaint);
                    }

                    // Possible move targets: orange ring
                    for (int[] move : possibleMoves) {
                        if (move[0] == r && move[1] == c) {
                            canvas.drawCircle(cx, cy, radius, movePaint);
                        }
                    }
                }
            }
            // Copyright
            canvas.drawText("© Moritzsoft", w - 8, h - 4, copyPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int col = (int) ((event.getX() - offsetX) / cellSize);
                int row = (int) ((event.getY() - offsetY) / cellSize);
                if (row >= 0 && row < 7 && col >= 0 && col < 7) {
                    onCellTapped(row, col);
                }
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
