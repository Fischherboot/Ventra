package com.moritzsoft.renterspiele;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MenschActivity extends AppCompatActivity {

    // 40 track positions on an 11x11 grid (row, col), counter-clockwise
    private static final int[][] TRACK = {
        {10,4},{9,4},{8,4},{7,4},{6,4},         // 0-4: bottom arm up (left col)
        {6,3},{6,2},{6,1},{6,0},                 // 5-8: left
        {5,0},                                    // 9: corner
        {4,0},{4,1},{4,2},{4,3},{4,4},           // 10-14: left arm right (top row)
        {3,4},{2,4},{1,4},{0,4},                 // 15-18: top arm up (left col)
        {0,5},                                    // 19: corner
        {0,6},{1,6},{2,6},{3,6},{4,6},           // 20-24: top arm down (right col)
        {4,7},{4,8},{4,9},{4,10},                // 25-28: right
        {5,10},                                   // 29: corner
        {6,10},{6,9},{6,8},{6,7},{6,6},          // 30-34: right arm left (bottom row)
        {7,6},{8,6},{9,6},{10,6},                // 35-38: bottom arm down (right col)
        {10,5},                                   // 39: corner
    };

    // Home positions (off-track, 4 per player) [player][piece] = {row, col}
    private static final int[][][] HOME = {
        {{9,0},{9,1},{10,0},{10,1}},     // Red: bottom-left
        {{0,0},{0,1},{1,0},{1,1}},       // Blue: top-left
        {{0,9},{0,10},{1,9},{1,10}},     // Green: top-right
        {{9,9},{9,10},{10,9},{10,10}},   // Yellow: bottom-right
    };

    // Finish lanes (4 cells per player, leading toward center)
    private static final int[][][] FINISH = {
        {{9,5},{8,5},{7,5},{6,5}},       // Red: going up center
        {{5,1},{5,2},{5,3},{5,4}},       // Blue: going right center
        {{1,5},{2,5},{3,5},{4,5}},       // Green: going down center
        {{5,9},{5,8},{5,7},{5,6}},       // Yellow: going left center
    };

    // Track entry positions (which TRACK index each player enters at)
    private static final int[] ENTRY = {0, 10, 20, 30};

    private static final int[] PLAYER_COLORS = {0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFDD835};
    private static final String[] PLAYER_NAMES = {"Rot", "Blau", "Grün", "Gelb"};
    private static final int TRACK_SIZE = 40;

    private int numPlayers = 4;
    private boolean[] isBot;
    // Piece state: -1 = home, 0-39 = steps taken on track (relative), 40-43 = finish slot
    private int[][] pieceState; // [player][piece]
    private int currentPlayer = 0;
    private int diceValue = 0;
    private boolean diceRolled = false;
    private int rollAttempts = 0;
    private boolean gameOver = false;

    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BoardView boardView;
    private TextView txtStatus, txtDice, txtFlash, txtPickPiece;
    private Button btnRoll;
    private Button[] btnPieces = new Button[4];
    private LinearLayout pieceButtonsLayout;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_mensch);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        txtStatus = findViewById(R.id.txtStatus);
        txtDice = findViewById(R.id.txtDice);
        txtFlash = findViewById(R.id.txtFlash);
        txtPickPiece = findViewById(R.id.txtPickPiece);
        pieceButtonsLayout = findViewById(R.id.pieceButtonsLayout);
        btnRoll = findViewById(R.id.btnRoll);
        btnRoll.setOnClickListener(v -> { vibrate(30); rollDice(); });

        btnPieces[0] = findViewById(R.id.btnPiece1);
        btnPieces[1] = findViewById(R.id.btnPiece2);
        btnPieces[2] = findViewById(R.id.btnPiece3);
        btnPieces[3] = findViewById(R.id.btnPiece4);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            btnPieces[i].setOnClickListener(v -> { vibrate(30); onPieceTapped(currentPlayer, idx); });
        }

        // Only 4 players allowed - hide 2 and 3 player buttons
        findViewById(R.id.btn2Players).setVisibility(View.GONE);
        findViewById(R.id.btn3Players).setVisibility(View.GONE);
        findViewById(R.id.btn4Players).setOnClickListener(v -> { vibrate(30); setup(4, true); });
        findViewById(R.id.btn2Local).setOnClickListener(v -> { vibrate(30); setup(4, false); });
        findViewById(R.id.btnBackSetup).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnBack).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnNewGame).setOnClickListener(v -> {
            vibrate(30);
            findViewById(R.id.gameScreen).setVisibility(View.GONE);
            findViewById(R.id.setupScreen).setVisibility(View.VISIBLE);
        });
    }

    private void setup(int players, boolean withBots) {
        numPlayers = players;
        isBot = new boolean[4];
        if (withBots) { isBot[0] = false; for (int i = 1; i < 4; i++) isBot[i] = i < players; }
        else { for (int i = 0; i < 4; i++) isBot[i] = false; }
        pieceState = new int[4][4];
        for (int p = 0; p < 4; p++) for (int i = 0; i < 4; i++) pieceState[p][i] = -1;
        currentPlayer = 0; diceValue = 0; diceRolled = false; gameOver = false;

        findViewById(R.id.setupScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);
        // Always keep flash overlay hidden
        txtFlash.setVisibility(View.GONE);

        FrameLayout container = findViewById(R.id.boardContainer);
        container.removeAllViews();
        boardView = new BoardView(this);
        container.addView(boardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        txtDice.setText("🎲");
        btnRoll.setEnabled(true);
        updateStatus();
        if (isBot()) handler.postDelayed(this::botTurn, 600);
    }

    private boolean isBot() { return currentPlayer < numPlayers && isBot[currentPlayer]; }

    // Convert player's relative track position to global TRACK index
    private int toGlobal(int player, int relPos) {
        return (ENTRY[player] + relPos) % TRACK_SIZE;
    }

    // Get screen coordinates for a piece
    private int[] getPieceCoord(int player, int piece) {
        int state = pieceState[player][piece];
        if (state == -1) return HOME[player][piece];
        if (state >= 40) return FINISH[player][state - 40];
        return TRACK[toGlobal(player, state)];
    }

    private void rollDice() {
        if (diceRolled || gameOver) return;
        diceValue = random.nextInt(6) + 1;
        String[] emoji = {"⚀","⚁","⚂","⚃","⚄","⚅"};
        txtDice.setText(emoji[diceValue - 1]);
        diceRolled = true;

        List<Integer> movable = getMovablePieces();
        if (movable.isEmpty()) {
            boolean allHome = true;
            for (int i = 0; i < 4; i++) if (pieceState[currentPlayer][i] != -1) allHome = false;
            rollAttempts++;
            if (allHome && rollAttempts < 3 && diceValue != 6) {
                // Can try again
                diceRolled = false;
                if (isBot()) handler.postDelayed(this::botTurn, 400);
                return;
            }
            handler.postDelayed(this::nextTurn, 600);
            return;
        }

        if (isBot()) {
            // Bot picks move
            int best = movable.get(0);
            // Prefer leaving home on 6
            for (int idx : movable)
                if (pieceState[currentPlayer][idx] == -1) { best = idx; break; }
            final int choice = best;
            handler.postDelayed(() -> movePiece(choice), 500);
        } else {
            txtStatus.setText(PLAYER_NAMES[currentPlayer] + " – Figur wählen");
            txtStatus.setTextColor(PLAYER_COLORS[currentPlayer]);
            btnRoll.setEnabled(false);
            showPieceButtons(movable);
            boardView.invalidate();
        }
    }

    private List<Integer> getMovablePieces() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int state = pieceState[currentPlayer][i];
            if (state == -1) {
                if (diceValue == 6) {
                    // Check if own piece already on start field (relPos 0)
                    boolean startOccupied = false;
                    for (int j = 0; j < 4; j++) {
                        if (j != i && pieceState[currentPlayer][j] == 0) {
                            startOccupied = true; break;
                        }
                    }
                    if (!startOccupied) result.add(i);
                }
            } else if (state < 40) {
                int newState = state + diceValue;
                if (newState <= 43) {
                    // Check if finish slot is free
                    if (newState >= 40) {
                        boolean occupied = false;
                        for (int j = 0; j < 4; j++)
                            if (j != i && pieceState[currentPlayer][j] == newState) occupied = true;
                        if (!occupied) result.add(i);
                    } else {
                        // Check own piece not already there
                        int globalNew = toGlobal(currentPlayer, newState);
                        boolean ownBlocking = false;
                        for (int j = 0; j < 4; j++) {
                            if (j != i && pieceState[currentPlayer][j] >= 0 && pieceState[currentPlayer][j] < 40
                                    && toGlobal(currentPlayer, pieceState[currentPlayer][j]) == globalNew) {
                                ownBlocking = true; break;
                            }
                        }
                        if (!ownBlocking) result.add(i);
                    }
                }
            }
        }
        return result;
    }

    private void movePiece(int pieceIdx) {
        int state = pieceState[currentPlayer][pieceIdx];
        if (state == -1) {
            // Enter track at position 0
            pieceState[currentPlayer][pieceIdx] = 0;
            kickOpponent(currentPlayer, 0);
        } else {
            int newState = state + diceValue;
            pieceState[currentPlayer][pieceIdx] = newState;
            if (newState < 40) {
                kickOpponent(currentPlayer, newState);
            }
        }
        vibrate(30);
        boardView.invalidate();

        // Check win
        boolean won = true;
        for (int i = 0; i < 4; i++)
            if (pieceState[currentPlayer][i] < 40) { won = false; break; }
        if (won) {
            gameOver = true;
            txtStatus.setText(PLAYER_NAMES[currentPlayer] + " gewinnt!");
            txtStatus.setTextColor(PLAYER_COLORS[currentPlayer]);
            vibrate(500); btnRoll.setEnabled(false); return;
        }

        if (diceValue == 6) {
            diceRolled = false;
            txtDice.setText("🎲");
            txtStatus.setText(PLAYER_NAMES[currentPlayer] + " – Nochmal! (6!)");
            txtStatus.setTextColor(PLAYER_COLORS[currentPlayer]);
            btnRoll.setEnabled(true);
            hidePieceButtons();
            if (isBot()) handler.postDelayed(this::botTurn, 600);
        } else {
            nextTurn();
        }
    }

    private void kickOpponent(int player, int relPos) {
        int global = toGlobal(player, relPos);
        for (int p = 0; p < numPlayers; p++) {
            if (p == player) continue;
            for (int i = 0; i < 4; i++) {
                int ps = pieceState[p][i];
                if (ps >= 0 && ps < 40 && toGlobal(p, ps) == global) {
                    pieceState[p][i] = -1;
                    vibrate(100);
                }
            }
        }
    }

    private void nextTurn() {
        do { currentPlayer = (currentPlayer + 1) % numPlayers; }
        while (currentPlayer >= numPlayers);
        if (currentPlayer >= numPlayers) currentPlayer = 0;

        diceRolled = false; diceValue = 0; rollAttempts = 0;
        txtDice.setText("🎲");
        btnRoll.setEnabled(true);
        hidePieceButtons();
        updateStatus();
        boardView.invalidate();
        // No flash overlay - turn info shown via colored txtStatus
        if (isBot() && !gameOver) handler.postDelayed(this::botTurn, 800);
    }

    private void botTurn() {
        if (gameOver) return;
        rollDice();
    }

    private void updateStatus() {
        if (!gameOver) {
            String who = PLAYER_NAMES[currentPlayer];
            if (isBot()) {
                txtStatus.setText(who + " (Roboter) denkt...");
            } else {
                txtStatus.setText(who + " ist dran – Würfeln!");
            }
            txtStatus.setTextColor(PLAYER_COLORS[currentPlayer]);
        }
    }

    private void onPieceTapped(int player, int pieceIdx) {
        if (gameOver || !diceRolled || isBot() || player != currentPlayer) return;
        List<Integer> movable = getMovablePieces();
        if (movable.contains(pieceIdx)) {
            hidePieceButtons();
            movePiece(pieceIdx);
        }
    }

    private void showPieceButtons(List<Integer> movable) {
        txtPickPiece.setVisibility(View.VISIBLE);
        txtPickPiece.setTextColor(PLAYER_COLORS[currentPlayer]);
        pieceButtonsLayout.setVisibility(View.VISIBLE);
        for (int i = 0; i < 4; i++) {
            btnPieces[i].setEnabled(movable.contains(i));
            btnPieces[i].setAlpha(movable.contains(i) ? 1.0f : 0.3f);
            // Tint buttons to match current player color
            btnPieces[i].getBackground().setTint(PLAYER_COLORS[currentPlayer]);
            btnPieces[i].setTextColor(0xFFFFFFFF);
        }
    }

    private void hidePieceButtons() {
        txtPickPiece.setVisibility(View.GONE);
        pieceButtonsLayout.setVisibility(View.GONE);
    }

    private void showFlash(String text) {
        // Disabled - no overlay flash, turn info shown via txtStatus
        txtFlash.setVisibility(View.GONE);
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

    // ========== Board View ==========
    private class BoardView extends View {
        private Paint bgPaint, cellPaint, outlinePaint, textPaint;
        private Paint[] playerPaints;
        private float cellSize, boardLeft, boardTop;
        private static final int G = 11;

        // Valid board cells (cross shape + home corners)
        private final boolean[][] isTrackCell = new boolean[G][G];

        public BoardView(Context ctx) {
            super(ctx);
            bgPaint = mp(0xFFF5F0E0);
            cellPaint = mp(0xFFFFFFFF);
            outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outlinePaint.setColor(0xFF888888);
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setStrokeWidth(2f);
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            playerPaints = new Paint[4];
            for (int i = 0; i < 4; i++) playerPaints[i] = mp(PLAYER_COLORS[i]);

            // Mark all track cells
            for (int[] tc : TRACK) isTrackCell[tc[0]][tc[1]] = true;
            for (int p = 0; p < 4; p++) {
                for (int[] hc : HOME[p]) isTrackCell[hc[0]][hc[1]] = true;
                for (int[] fc : FINISH[p]) isTrackCell[fc[0]][fc[1]] = true;
            }
        }

        private Paint mp(int c) { Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(c); p.setStyle(Paint.Style.FILL); return p; }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            cellSize = Math.min(w, h) / 12f;
            boardLeft = (w - cellSize * G) / 2f;
            boardTop = (h - cellSize * G) / 2f;
            textPaint.setTextSize(cellSize * 0.35f);

            canvas.drawRect(0, 0, w, h, bgPaint);

            // Draw track cells
            for (int[] tc : TRACK) drawCell(canvas, tc[0], tc[1], cellPaint);

            // Draw home zones with player colors
            for (int p = 0; p < numPlayers; p++) {
                Paint homeBg = new Paint(playerPaints[p]); homeBg.setAlpha(60);
                for (int[] hc : HOME[p]) drawCell(canvas, hc[0], hc[1], homeBg);
            }

            // Draw finish zones
            for (int p = 0; p < numPlayers; p++) {
                Paint finBg = new Paint(playerPaints[p]); finBg.setAlpha(60);
                for (int[] fc : FINISH[p]) drawCell(canvas, fc[0], fc[1], finBg);
            }

            // Highlight entry positions
            for (int p = 0; p < numPlayers; p++) {
                int[] entry = TRACK[ENTRY[p]];
                Paint ep = new Paint(playerPaints[p]); ep.setAlpha(100);
                drawCell(canvas, entry[0], entry[1], ep);
            }

            // Copyright
            Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
            cp.setColor(0x88888888); cp.setTextSize(12f); cp.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("© Moritzsoft", w - 8, h - 4, cp);

            // Draw all pieces
            for (int p = 0; p < numPlayers; p++) {
                for (int i = 0; i < 4; i++) {
                    int[] coord = getPieceCoord(p, i);
                    float cx = boardLeft + coord[1] * cellSize + cellSize / 2f;
                    float cy = boardTop + coord[0] * cellSize + cellSize / 2f;
                    canvas.drawCircle(cx, cy, cellSize * 0.35f, playerPaints[p]);
                    canvas.drawCircle(cx, cy, cellSize * 0.35f, outlinePaint);
                    canvas.drawText(String.valueOf(i + 1), cx,
                            cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint);
                }
            }
        }

        private void drawCell(Canvas canvas, int row, int col, Paint paint) {
            float left = boardLeft + col * cellSize + 2;
            float top = boardTop + row * cellSize + 2;
            canvas.drawRoundRect(left, top, left + cellSize - 4, top + cellSize - 4, 6, 6, paint);
            canvas.drawRoundRect(left, top, left + cellSize - 4, top + cellSize - 4, 6, 6, outlinePaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && diceRolled && !isBot()) {
                float x = event.getX(), y = event.getY();
                for (int i = 0; i < 4; i++) {
                    int[] coord = getPieceCoord(currentPlayer, i);
                    float cx = boardLeft + coord[1] * cellSize + cellSize / 2f;
                    float cy = boardTop + coord[0] * cellSize + cellSize / 2f;
                    if (Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy)) < cellSize * 0.5f) {
                        onPieceTapped(currentPlayer, i);
                        return true;
                    }
                }
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
