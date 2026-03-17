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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DameActivity extends AppCompatActivity {

    // 0=empty, 1=white, 2=black, 3=white king, 4=black king
    private int[][] board = new int[8][8];
    private boolean vsBot = true;
    private boolean whiteTurn = true;
    private int selectedRow = -1, selectedCol = -1;
    private List<int[]> validMoves = new ArrayList<>();
    private boolean gameOver = false;
    private boolean mustContinueChain = false; // true when mid-chain capture

    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BoardView boardView;
    private TextView txtStatus, txtInfo;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_dame);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        txtStatus = findViewById(R.id.txtStatus);
        txtInfo = findViewById(R.id.txtInfo);
        ((TextView) findViewById(R.id.txtGameTitle)).setText("Dame");

        findViewById(R.id.btnBot).setOnClickListener(v -> { vibrate(30); vsBot = true; startGame(); });
        findViewById(R.id.btnLocal).setOnClickListener(v -> { vibrate(30); vsBot = false; startGame(); });
        findViewById(R.id.btnBackMode).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnBack).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnNewGame).setOnClickListener(v -> { vibrate(30); startGame(); });
        findViewById(R.id.btnUndo).setVisibility(View.GONE);
        findViewById(R.id.btnHelp).setOnClickListener(v -> {
            vibrate(30);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.help_dame_title))
                    .setMessage(getString(R.string.help_dame_text))
                    .setPositiveButton(getString(R.string.btn_understood), null).show();
        });
    }

    private void startGame() {
        board = new int[8][8];
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1) board[r][c] = 2;
        for (int r = 5; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1) board[r][c] = 1;

        whiteTurn = true; gameOver = false; mustContinueChain = false;
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

    private boolean isWhite(int p) { return p == 1 || p == 3; }
    private boolean isBlack(int p) { return p == 2 || p == 4; }
    private boolean isKing(int p) { return p == 3 || p == 4; }

    // Get captures only for a piece
    private List<int[]> getCapturesForPiece(int r, int c) {
        List<int[]> captures = new ArrayList<>();
        int piece = board[r][c];
        if (piece == 0) return captures;
        boolean white = isWhite(piece);

        // All 4 diagonal directions for captures (even non-kings can capture backwards)
        for (int dr = -1; dr <= 1; dr += 2) {
            for (int dc = -1; dc <= 1; dc += 2) {
                int mr = r + dr, mc = c + dc;
                int tr = r + 2 * dr, tc = c + 2 * dc;
                if (tr >= 0 && tr < 8 && tc >= 0 && tc < 8
                        && board[tr][tc] == 0 && board[mr][mc] != 0
                        && isWhite(board[mr][mc]) != white) {
                    captures.add(new int[]{tr, tc, mr, mc});
                }
            }
        }
        return captures;
    }

    // Get all moves for a piece (captures + regular)
    private List<int[]> getMovesForPiece(int r, int c) {
        int piece = board[r][c];
        if (piece == 0) return new ArrayList<>();

        List<int[]> captures = getCapturesForPiece(r, c);
        if (!captures.isEmpty()) return captures; // if captures exist, must capture

        // Regular moves
        List<int[]> moves = new ArrayList<>();
        boolean white = isWhite(piece);
        int[] dirs = isKing(piece) ? new int[]{-1, 1} : (white ? new int[]{-1} : new int[]{1});
        for (int dr : dirs) {
            for (int dc : new int[]{-1, 1}) {
                int tr = r + dr, tc = c + dc;
                if (tr >= 0 && tr < 8 && tc >= 0 && tc < 8 && board[tr][tc] == 0) {
                    moves.add(new int[]{tr, tc, -1, -1});
                }
            }
        }
        return moves;
    }

    private boolean anyoneHasCapture(boolean white) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != 0 && isWhite(board[r][c]) == white)
                    if (!getCapturesForPiece(r, c).isEmpty()) return true;
        return false;
    }

    private void onCellTapped(int row, int col) {
        if (gameOver) return;
        if (vsBot && !whiteTurn) return;

        // If in chain capture, only allow moves from the chain piece
        if (mustContinueChain) {
            for (int[] move : validMoves) {
                if (move[0] == row && move[1] == col) {
                    executeMove(selectedRow, selectedCol, move);
                    return;
                }
            }
            return; // must continue chain, can't select other piece
        }

        // Try to execute a move from selected piece
        if (selectedRow >= 0) {
            for (int[] move : validMoves) {
                if (move[0] == row && move[1] == col) {
                    executeMove(selectedRow, selectedCol, move);
                    return;
                }
            }
        }

        // Select piece
        int piece = board[row][col];
        if (piece != 0 && isWhite(piece) == whiteTurn) {
            selectedRow = row; selectedCol = col;
            validMoves = getMovesForPiece(row, col);

            // Enforce capture: if other pieces can capture, only allow captures
            if (anyoneHasCapture(whiteTurn)) {
                List<int[]> capturesOnly = new ArrayList<>();
                for (int[] m : validMoves) if (m[2] >= 0) capturesOnly.add(m);
                if (capturesOnly.isEmpty()) {
                    // This piece has no captures but others do - can't select this one
                    selectedRow = -1; selectedCol = -1;
                    validMoves.clear();
                    vibrate(20);
                    boardView.invalidate();
                    return;
                }
                validMoves = capturesOnly;
            }
            vibrate(20);
            boardView.invalidate();
        }
    }

    private void executeMove(int fromR, int fromC, int[] move) {
        int piece = board[fromR][fromC];
        board[fromR][fromC] = 0;
        board[move[0]][move[1]] = piece;

        boolean wasCapture = move[2] >= 0;
        if (wasCapture) {
            board[move[2]][move[3]] = 0;
        }

        // King promotion
        if (isWhite(piece) && !isKing(piece) && move[0] == 0) board[move[0]][move[1]] = 3;
        if (isBlack(piece) && !isKing(piece) && move[0] == 7) board[move[0]][move[1]] = 4;

        vibrate(30);

        // Check for chain captures
        if (wasCapture) {
            List<int[]> moreCaptures = getCapturesForPiece(move[0], move[1]);
            if (!moreCaptures.isEmpty()) {
                // Must continue chain
                selectedRow = move[0]; selectedCol = move[1];
                validMoves = moreCaptures;
                mustContinueChain = true;
                boardView.invalidate();
                updateStatus();

                // If bot, continue chain automatically
                if (vsBot && !whiteTurn) {
                    handler.postDelayed(() -> botContinueChain(move[0], move[1]), 500);
                }
                return;
            }
        }

        // Turn complete
        endTurn();
    }

    private void endTurn() {
        selectedRow = -1; selectedCol = -1;
        validMoves.clear();
        mustContinueChain = false;
        whiteTurn = !whiteTurn;
        boardView.invalidate();
        updateStatus();
        checkWin();

        if (vsBot && !whiteTurn && !gameOver) {
            handler.postDelayed(this::botMove, 800);
        }
    }

    private void botContinueChain(int r, int c) {
        if (gameOver) return;
        List<int[]> captures = getCapturesForPiece(r, c);
        if (captures.isEmpty()) {
            endTurn();
            return;
        }
        int[] chosen = captures.get(random.nextInt(captures.size()));
        executeMove(r, c, chosen);
    }

    private void botMove() {
        if (gameOver) return;

        // Gather all legal moves for black
        List<int[]> allMoves = new ArrayList<>(); // {fromR, fromC, toR, toC, capR, capC}
        boolean mustCapture = anyoneHasCapture(false);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] != 0 && isBlack(board[r][c])) {
                    for (int[] m : getMovesForPiece(r, c)) {
                        if (!mustCapture || m[2] >= 0) {
                            allMoves.add(new int[]{r, c, m[0], m[1], m[2], m[3]});
                        }
                    }
                }
            }
        }

        if (allMoves.isEmpty()) {
            gameOver = true;
            txtStatus.setText("Weiß gewinnt!");
            vibrate(500);
            return;
        }

        // 30% chance: skip a capture and make a random non-capture move instead (intentional blunder)
        if (mustCapture && random.nextInt(100) < 30) {
            List<int[]> nonCaptures = new ArrayList<>();
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (board[r][c] != 0 && isBlack(board[r][c])) {
                        for (int[] m : getMovesForPiece(r, c)) {
                            nonCaptures.add(new int[]{r, c, m[0], m[1], m[2], m[3]});
                        }
                    }
                }
            }
            if (!nonCaptures.isEmpty()) {
                allMoves = nonCaptures;
                mustCapture = false;
            }
        }

        // Prefer captures, then random
        List<int[]> captures = new ArrayList<>();
        for (int[] m : allMoves) if (m[4] >= 0) captures.add(m);

        int[] chosen;
        if (!captures.isEmpty()) {
            chosen = captures.get(random.nextInt(captures.size()));
        } else {
            // 25% chance: pick a move that moves backward (bad move)
            if (random.nextInt(100) < 25) {
                List<int[]> badMoves = new ArrayList<>();
                for (int[] m : allMoves) {
                    if (m[2] < m[0]) badMoves.add(m); // moving toward own side
                }
                if (!badMoves.isEmpty()) {
                    chosen = badMoves.get(random.nextInt(badMoves.size()));
                } else {
                    chosen = allMoves.get(random.nextInt(allMoves.size()));
                }
            } else {
                chosen = allMoves.get(random.nextInt(allMoves.size()));
            }
        }

        selectedRow = chosen[0]; selectedCol = chosen[1];
        executeMove(chosen[0], chosen[1], new int[]{chosen[2], chosen[3], chosen[4], chosen[5]});
    }

    private void checkWin() {
        int w = 0, b = 0;
        boolean wCanMove = false, bCanMove = false;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (isWhite(p)) { w++; if (!getMovesForPiece(r, c).isEmpty()) wCanMove = true; }
                if (isBlack(p)) { b++; if (!getMovesForPiece(r, c).isEmpty()) bCanMove = true; }
            }

        if (w == 0 || (!wCanMove && whiteTurn)) {
            gameOver = true; txtStatus.setText("Schwarz gewinnt!"); vibrate(500);
        } else if (b == 0 || (!bCanMove && !whiteTurn)) {
            gameOver = true; txtStatus.setText("Weiß gewinnt!"); vibrate(500);
        }
    }

    private void updateStatus() {
        if (!gameOver)
            txtStatus.setText(whiteTurn ? "Weiß ist dran" : (vsBot ? "Roboter denkt..." : "Schwarz ist dran"));
        int w = 0, b = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                if (isWhite(board[r][c])) w++;
                if (isBlack(board[r][c])) b++;
            }
        txtInfo.setText("Weiß: " + w + "  Schwarz: " + b);
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
        private Paint lightPaint, darkPaint, whitePiece, blackPiece, copyPaint;
        private Paint selectedHL, moveHL, kingMark, outline;
        private float cellSize, offsetX, offsetY;

        public BoardView(Context ctx) {
            super(ctx);
            lightPaint = mp(0xFFF5E6CC);
            darkPaint = mp(0xFF8B6914);
            whitePiece = mp(0xFFFAFAFA);
            blackPiece = mp(0xFF333333);
            selectedHL = mp(0x88FFEB3B);
            moveHL = mp(0x8866BB6A);
            kingMark = new Paint(Paint.ANTI_ALIAS_FLAG);
            kingMark.setColor(0xFFFFD700);
            kingMark.setStyle(Paint.Style.STROKE);
            kingMark.setStrokeWidth(4f);
            outline = new Paint(Paint.ANTI_ALIAS_FLAG);
            outline.setColor(0xFF555555);
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(2f);
            copyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            copyPaint.setColor(0x88888888);
            copyPaint.setTextSize(12f);
            copyPaint.setTextAlign(Paint.Align.RIGHT);
        }

        private Paint mp(int color) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color); p.setStyle(Paint.Style.FILL); return p;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            cellSize = Math.min(w, h) / 8f;
            offsetX = (w - cellSize * 8) / 2f;
            offsetY = (h - cellSize * 8) / 2f;

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    float left = offsetX + c * cellSize, top = offsetY + r * cellSize;
                    canvas.drawRect(left, top, left + cellSize, top + cellSize,
                            (r + c) % 2 == 0 ? lightPaint : darkPaint);

                    if (r == selectedRow && c == selectedCol)
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, selectedHL);
                    for (int[] m : validMoves)
                        if (m[0] == r && m[1] == c)
                            canvas.drawRect(left, top, left + cellSize, top + cellSize, moveHL);

                    int piece = board[r][c];
                    if (piece != 0) {
                        float cx = left + cellSize / 2f, cy = top + cellSize / 2f;
                        float radius = cellSize * 0.38f;
                        canvas.drawCircle(cx, cy, radius, isWhite(piece) ? whitePiece : blackPiece);
                        canvas.drawCircle(cx, cy, radius, outline);
                        if (isKing(piece))
                            canvas.drawCircle(cx, cy, radius * 0.6f, kingMark);
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
                if (row >= 0 && row < 8 && col >= 0 && col < 8) onCellTapped(row, col);
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
