package com.moritzsoft.renterspiele;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class HalmaActivity extends AppCompatActivity {

    private static final int SIZE = 8;
    // 0=empty, 1=green(human, starts bottom-right), 2=red(bot/p2, starts top-left)
    private int[][] board = new int[SIZE][SIZE];
    private boolean vsBot = true;
    private boolean greenTurn = true;
    private int selectedRow = -1, selectedCol = -1;
    private List<int[]> validMoves = new ArrayList<>();
    private boolean gameOver = false;

    // Green starts at bottom-right triangle, goal is top-left
    // Red starts at top-left triangle, goal is bottom-right
    private static final boolean[][] GREEN_START = new boolean[SIZE][SIZE];
    private static final boolean[][] RED_START = new boolean[SIZE][SIZE];
    static {
        // Top-left triangle (3 rows): Red start, Green goal
        RED_START[0][0] = true; RED_START[0][1] = true; RED_START[0][2] = true;
        RED_START[1][0] = true; RED_START[1][1] = true;
        RED_START[2][0] = true;

        // Bottom-right triangle (3 rows): Green start, Red goal
        GREEN_START[7][7] = true; GREEN_START[7][6] = true; GREEN_START[7][5] = true;
        GREEN_START[6][7] = true; GREEN_START[6][6] = true;
        GREEN_START[5][7] = true;
    }

    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BoardView boardView;
    private TextView txtStatus, txtInfo;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_halma);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        txtStatus = findViewById(R.id.txtStatus);
        txtInfo = findViewById(R.id.txtInfo);
        ((TextView) findViewById(R.id.txtGameTitle)).setText("Halma");

        findViewById(R.id.btnBot).setOnClickListener(v -> { vibrate(30); vsBot = true; startGame(); });
        findViewById(R.id.btnLocal).setOnClickListener(v -> { vibrate(30); vsBot = false; startGame(); });
        findViewById(R.id.btnBackMode).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnBack).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnNewGame).setOnClickListener(v -> { vibrate(30); startGame(); });
        findViewById(R.id.btnUndo).setVisibility(View.GONE);
        findViewById(R.id.btnHelp).setOnClickListener(v -> {
            vibrate(30);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.help_halma_title))
                    .setMessage(getString(R.string.help_halma_text))
                    .setPositiveButton(getString(R.string.btn_understood), null).show();
        });
    }

    private void startGame() {
        board = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                if (GREEN_START[r][c]) board[r][c] = 1;
                if (RED_START[r][c]) board[r][c] = 2;
            }
        greenTurn = true; gameOver = false;
        selectedRow = -1; selectedCol = -1;
        validMoves.clear();

        findViewById(R.id.modeScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);

        FrameLayout container = findViewById(R.id.boardContainer);
        container.removeAllViews();
        boardView = new BoardView(this);
        container.addView(boardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        updateStatus();
    }

    private List<int[]> getMovesForPiece(int r, int c) {
        List<int[]> moves = new ArrayList<>();
        // Single step in 8 directions
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && board[nr][nc] == 0)
                    moves.add(new int[]{nr, nc});
            }
        // Chain jumps
        Set<String> visited = new HashSet<>();
        visited.add(r + "," + c);
        findJumps(r, c, visited, moves);
        return moves;
    }

    private void findJumps(int r, int c, Set<String> visited, List<int[]> moves) {
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int mr = r + dr, mc = c + dc;
                int tr = r + 2 * dr, tc = c + 2 * dc;
                if (tr >= 0 && tr < SIZE && tc >= 0 && tc < SIZE
                        && board[mr][mc] != 0 && board[tr][tc] == 0) {
                    String key = tr + "," + tc;
                    if (!visited.contains(key)) {
                        visited.add(key);
                        moves.add(new int[]{tr, tc});
                        findJumps(tr, tc, visited, moves);
                    }
                }
            }
    }

    private void onCellTapped(int row, int col) {
        if (gameOver) return;
        if (vsBot && !greenTurn) return;

        if (selectedRow >= 0) {
            for (int[] m : validMoves) {
                if (m[0] == row && m[1] == col) {
                    board[row][col] = board[selectedRow][selectedCol];
                    board[selectedRow][selectedCol] = 0;
                    vibrate(30);
                    selectedRow = -1; selectedCol = -1;
                    validMoves.clear();
                    greenTurn = !greenTurn;
                    boardView.invalidate();
                    updateStatus();
                    checkWin();
                    if (vsBot && !greenTurn && !gameOver)
                        handler.postDelayed(this::botMove, 800);
                    return;
                }
            }
        }

        if (board[row][col] == (greenTurn ? 1 : 2)) {
            selectedRow = row; selectedCol = col;
            validMoves = getMovesForPiece(row, col);
            vibrate(20);
            boardView.invalidate();
        }
    }

    private void botMove() {
        if (gameOver) return;

        // Red (2) wants to reach GREEN_START (bottom-right triangle)
        // Strategy: weakened bot that makes frequent mistakes
        int bestScore = Integer.MIN_VALUE;
        int bestFromR = -1, bestFromC = -1, bestToR = -1, bestToC = -1;

        // Collect all possible moves
        List<int[]> allMoves = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != 2) continue;
                for (int[] m : getMovesForPiece(r, c)) {
                    allMoves.add(new int[]{r, c, m[0], m[1]});
                }
            }
        }

        if (allMoves.isEmpty()) {
            greenTurn = true;
            boardView.invalidate();
            updateStatus();
            return;
        }

        // 18% chance: make a completely random move (occasional mistake)
        if (random.nextInt(100) < 18) {
            int[] pick = allMoves.get(random.nextInt(allMoves.size()));
            board[pick[2]][pick[3]] = board[pick[0]][pick[1]];
            board[pick[0]][pick[1]] = 0;
            vibrate(30);
            greenTurn = true;
            boardView.invalidate();
            updateStatus();
            checkWin();
            return;
        }

        for (int[] move : allMoves) {
            int r = move[0], c = move[1];
            boolean inGoal = GREEN_START[r][c];
            int currentDist = distToGoal(r, c, false);

            int newDist = distToGoal(move[2], move[3], false);
            boolean willBeInGoal = GREEN_START[move[2]][move[3]];

            int score = currentDist - newDist;

            if (willBeInGoal && !inGoal) score += 18;
            if (inGoal && !willBeInGoal) score -= 50;
            if (inGoal && willBeInGoal) score += 4;

            // Moderate randomness so bot sometimes picks suboptimal moves
            score += random.nextInt(18);

            if (score > bestScore) {
                bestScore = score;
                bestFromR = move[0]; bestFromC = move[1];
                bestToR = move[2]; bestToC = move[3];
            }
        }

        if (bestFromR >= 0) {
            board[bestToR][bestToC] = board[bestFromR][bestFromC];
            board[bestFromR][bestFromC] = 0;
            vibrate(30);
        }

        greenTurn = true;
        boardView.invalidate();
        updateStatus();
        checkWin();
    }

    private int distToGoal(int r, int c, boolean isGreen) {
        // Green goal = RED_START (top-left), Red goal = GREEN_START (bottom-right)
        if (isGreen) {
            return r + c; // distance to top-left corner (0,0)
        } else {
            return (SIZE - 1 - r) + (SIZE - 1 - c); // distance to bottom-right (7,7)
        }
    }

    private void checkWin() {
        // Green wins if all green pieces are in RED_START zone
        boolean greenWon = true, redWon = true;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 1 && !RED_START[r][c]) greenWon = false;
                if (board[r][c] == 2 && !GREEN_START[r][c]) redWon = false;
            }
        if (greenWon) {
            gameOver = true; txtStatus.setText("Grün gewinnt!"); vibrate(500);
        } else if (redWon) {
            gameOver = true; txtStatus.setText("Rot gewinnt!"); vibrate(500);
        }
    }

    private void updateStatus() {
        if (!gameOver)
            txtStatus.setText(greenTurn ? "Grün ist dran" : (vsBot ? "Roboter denkt..." : "Rot ist dran"));
        txtInfo.setText("");
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
        private Paint bgPaint, goalGreen, goalRed, greenPiece, redPiece, copyPaint;
        private Paint selectedHL, moveHL, gridPaint, outlinePaint;
        private float cellSize, offsetX, offsetY;

        public BoardView(Context ctx) {
            super(ctx);
            bgPaint = mp(0xFFF5F0E0);
            goalGreen = mp(0x3066BB6A);
            goalRed = mp(0x30EF5350);
            greenPiece = mp(0xFF43A047);
            redPiece = mp(0xFFE53935);
            selectedHL = mp(0x88FFEB3B);
            moveHL = mp(0x8866BB6A);
            gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gridPaint.setColor(0xFFBBBBBB); gridPaint.setStyle(Paint.Style.STROKE); gridPaint.setStrokeWidth(2f);
            outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outlinePaint.setColor(0xFF555555); outlinePaint.setStyle(Paint.Style.STROKE); outlinePaint.setStrokeWidth(2f);
            copyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            copyPaint.setColor(0x88888888);
            copyPaint.setTextSize(12f);
            copyPaint.setTextAlign(Paint.Align.RIGHT);
        }

        private Paint mp(int c) { Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(c); p.setStyle(Paint.Style.FILL); return p; }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            cellSize = Math.min(w, h) / (float) SIZE;
            offsetX = (w - cellSize * SIZE) / 2f;
            offsetY = (h - cellSize * SIZE) / 2f;

            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    float left = offsetX + c * cellSize, top = offsetY + r * cellSize;
                    canvas.drawRect(left, top, left + cellSize, top + cellSize, bgPaint);
                    // Goal zones: Green goal = RED_START (top-left), Red goal = GREEN_START (bottom-right)
                    if (RED_START[r][c]) canvas.drawRect(left, top, left + cellSize, top + cellSize, goalGreen);
                    if (GREEN_START[r][c]) canvas.drawRect(left, top, left + cellSize, top + cellSize, goalRed);
                    canvas.drawRect(left, top, left + cellSize, top + cellSize, gridPaint);

                    if (r == selectedRow && c == selectedCol)
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, selectedHL);
                    for (int[] m : validMoves)
                        if (m[0] == r && m[1] == c)
                            canvas.drawRect(left, top, left + cellSize, top + cellSize, moveHL);

                    if (board[r][c] != 0) {
                        float cx = left + cellSize / 2f, cy = top + cellSize / 2f;
                        canvas.drawCircle(cx, cy, cellSize * 0.36f, board[r][c] == 1 ? greenPiece : redPiece);
                        canvas.drawCircle(cx, cy, cellSize * 0.36f, outlinePaint);
                    }
                }
            }
            copyPaint.setTextSize(12f);
            canvas.drawText("© Moritzsoft", w - 8, h - 4, copyPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int col = (int) ((event.getX() - offsetX) / cellSize);
                int row = (int) ((event.getY() - offsetY) / cellSize);
                if (row >= 0 && row < SIZE && col >= 0 && col < SIZE) onCellTapped(row, col);
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
