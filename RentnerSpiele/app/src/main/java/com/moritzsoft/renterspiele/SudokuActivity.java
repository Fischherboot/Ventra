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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SudokuActivity extends AppCompatActivity {

    private int[][] solution = new int[9][9];
    private int[][] puzzle = new int[9][9];
    private int[][] playerBoard = new int[9][9];
    private boolean[][] given = new boolean[9][9];

    private int selectedRow = -1, selectedCol = -1;
    private SudokuBoardView boardView;
    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private int currentDifficulty = 36;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_sudoku);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Button btnEasy = findViewById(R.id.btnEasy);
        Button btnMedium = findViewById(R.id.btnMedium);
        Button btnHard = findViewById(R.id.btnHard);
        Button btnBackDiff = findViewById(R.id.btnBackFromDifficulty);

        btnEasy.setOnClickListener(v -> {
            vibrate(30);
            currentDifficulty = 36;
            startGame(36);
        });
        btnMedium.setOnClickListener(v -> {
            vibrate(30);
            currentDifficulty = 46;
            startGame(46);
        });
        btnHard.setOnClickListener(v -> {
            vibrate(30);
            currentDifficulty = 54;
            startGame(54);
        });
        btnBackDiff.setOnClickListener(v -> {
            vibrate(30);
            finish();
        });

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            vibrate(30);
            finish();
        });

        Button btnNewGame = findViewById(R.id.btnNewGame);
        btnNewGame.setOnClickListener(v -> {
            vibrate(30);
            startGame(currentDifficulty);
        });
    }

    private void startGame(int cellsToRemove) {
        generateSudoku();
        createPuzzle(cellsToRemove);

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                playerBoard[r][c] = puzzle[r][c];
                given[r][c] = puzzle[r][c] != 0;
            }
        }

        selectedRow = -1;
        selectedCol = -1;

        findViewById(R.id.difficultyScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);

        FrameLayout boardContainer = findViewById(R.id.boardContainer);
        boardContainer.removeAllViews();
        boardView = new SudokuBoardView(this);
        boardContainer.addView(boardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        buildNumberPad();
    }

    private void buildNumberPad() {
        LinearLayout numPadContainer = findViewById(R.id.numPadContainer);
        numPadContainer.removeAllViews();
        numPadContainer.setGravity(Gravity.CENTER);

        GridLayout numGrid = new GridLayout(this);
        numGrid.setRowCount(4);
        numGrid.setColumnCount(3);
        numGrid.setUseDefaultMargins(false);

        LinearLayout.LayoutParams numGridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        numGridParams.gravity = Gravity.CENTER;
        numGrid.setLayoutParams(numGridParams);

        int btnSize = dpToPx(72);
        int margin = dpToPx(5);

        for (int i = 1; i <= 9; i++) {
            final int num = i;
            Button btn = new Button(this);
            btn.setText(String.valueOf(i));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            btn.setTextColor(0xFFFFFFFF);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setBackground(getDrawable(R.drawable.btn_numpad));
            btn.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = btnSize;
            params.height = btnSize;
            params.setMargins(margin, margin, margin, margin);
            params.rowSpec = GridLayout.spec((i - 1) / 3);
            params.columnSpec = GridLayout.spec((i - 1) % 3);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                vibrate(30);
                if (selectedRow >= 0 && selectedCol >= 0 && !given[selectedRow][selectedCol]) {
                    playerBoard[selectedRow][selectedCol] = num;
                    boardView.invalidate();
                    checkWin();
                }
            });

            numGrid.addView(btn);
        }

        // Delete button
        Button btnDelete = new Button(this);
        btnDelete.setText(getString(R.string.btn_delete));
        btnDelete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        btnDelete.setTextColor(0xFFFFFFFF);
        btnDelete.setTypeface(Typeface.DEFAULT_BOLD);
        btnDelete.setBackground(getDrawable(R.drawable.btn_delete));
        btnDelete.setGravity(Gravity.CENTER);

        GridLayout.LayoutParams delParams = new GridLayout.LayoutParams();
        delParams.width = btnSize * 3 + margin * 6;
        delParams.height = btnSize;
        delParams.setMargins(margin, margin, margin, margin);
        delParams.rowSpec = GridLayout.spec(3);
        delParams.columnSpec = GridLayout.spec(0, 3);
        btnDelete.setLayoutParams(delParams);

        btnDelete.setOnClickListener(v -> {
            vibrate(30);
            if (selectedRow >= 0 && selectedCol >= 0 && !given[selectedRow][selectedCol]) {
                playerBoard[selectedRow][selectedCol] = 0;
                boardView.invalidate();
            }
        });

        numGrid.addView(btnDelete);
        numPadContainer.addView(numGrid);
    }

    private void checkWin() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (playerBoard[r][c] == 0 || playerBoard[r][c] != solution[r][c]) {
                    return;
                }
            }
        }
        vibrate(500);
        Toast.makeText(this, getString(R.string.game_won), Toast.LENGTH_LONG).show();
        handler.postDelayed(this::finish, 2000);
    }

    private boolean hasConflict(int row, int col) {
        int val = playerBoard[row][col];
        if (val == 0) return false;

        for (int c = 0; c < 9; c++) {
            if (c != col && playerBoard[row][c] == val) return true;
        }
        for (int r = 0; r < 9; r++) {
            if (r != row && playerBoard[r][col] == val) return true;
        }
        int boxR = (row / 3) * 3;
        int boxC = (col / 3) * 3;
        for (int r = boxR; r < boxR + 3; r++) {
            for (int c = boxC; c < boxC + 3; c++) {
                if ((r != row || c != col) && playerBoard[r][c] == val) return true;
            }
        }
        return false;
    }

    // ==================== Sudoku Generator ====================

    private void generateSudoku() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                solution[r][c] = 0;
        fillBoard(solution, 0);
    }

    private boolean fillBoard(int[][] board, int pos) {
        if (pos == 81) return true;
        int row = pos / 9;
        int col = pos % 9;
        if (board[row][col] != 0) return fillBoard(board, pos + 1);

        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= 9; i++) nums.add(i);
        Collections.shuffle(nums, random);

        for (int num : nums) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num;
                if (fillBoard(board, pos + 1)) return true;
                board[row][col] = 0;
            }
        }
        return false;
    }

    private boolean isValid(int[][] board, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == num) return false;
            if (board[i][col] == num) return false;
        }
        int boxR = (row / 3) * 3;
        int boxC = (col / 3) * 3;
        for (int r = boxR; r < boxR + 3; r++)
            for (int c = boxC; c < boxC + 3; c++)
                if (board[r][c] == num) return false;
        return true;
    }

    private void createPuzzle(int cellsToRemove) {
        for (int r = 0; r < 9; r++)
            System.arraycopy(solution[r], 0, puzzle[r], 0, 9);

        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                cells.add(new int[]{r, c});
        Collections.shuffle(cells, random);

        int removed = 0;
        for (int[] cell : cells) {
            if (removed >= cellsToRemove) break;
            puzzle[cell[0]][cell[1]] = 0;
            removed++;
        }
    }

    // ==================== Utility ====================

    private void vibrate(int ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
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
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ==================== Custom SudokuBoardView ====================

    private class SudokuBoardView extends View {
        private Paint cellPaint, givenTextPaint, enteredTextPaint, conflictTextPaint;
        private Paint thickLinePaint, thinLinePaint, boardBgPaint;
        private float cellSize, offsetX, offsetY;

        public SudokuBoardView(Context context) {
            super(context);

            cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cellPaint.setStyle(Paint.Style.FILL);

            givenTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            givenTextPaint.setColor(0xFF1A1A1A);
            givenTextPaint.setTextAlign(Paint.Align.CENTER);
            givenTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

            enteredTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            enteredTextPaint.setColor(0xFF1565C0);
            enteredTextPaint.setTextAlign(Paint.Align.CENTER);
            enteredTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

            conflictTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            conflictTextPaint.setColor(0xFFEF5350);
            conflictTextPaint.setTextAlign(Paint.Align.CENTER);
            conflictTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

            thickLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            thickLinePaint.setColor(0xFF333333);
            thickLinePaint.setStyle(Paint.Style.STROKE);
            thickLinePaint.setStrokeWidth(5f);

            thinLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            thinLinePaint.setColor(0xFF888888);
            thinLinePaint.setStyle(Paint.Style.STROKE);
            thinLinePaint.setStrokeWidth(2f);

            boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boardBgPaint.setColor(0xFFEEEEEE);
            boardBgPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int w = getWidth();
            int h = getHeight();
            cellSize = Math.min(w, h) / 9f;
            offsetX = (w - cellSize * 9) / 2f;
            offsetY = (h - cellSize * 9) / 2f;

            float boardSize = cellSize * 9;

            givenTextPaint.setTextSize(cellSize * 0.6f);
            enteredTextPaint.setTextSize(cellSize * 0.6f);
            conflictTextPaint.setTextSize(cellSize * 0.6f);

            canvas.drawRect(offsetX, offsetY, offsetX + boardSize, offsetY + boardSize, boardBgPaint);

            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    float left = offsetX + c * cellSize;
                    float top = offsetY + r * cellSize;

                    boolean isSelected = (r == selectedRow && c == selectedCol);
                    boolean inRowCol = (selectedRow >= 0 && (r == selectedRow || c == selectedCol));
                    boolean inBox = (selectedRow >= 0 && r / 3 == selectedRow / 3 && c / 3 == selectedCol / 3);

                    if (isSelected) {
                        cellPaint.setColor(0xFFFFEB3B);
                    } else if (inRowCol || inBox) {
                        cellPaint.setColor(0xFFE0E0E0);
                    } else {
                        cellPaint.setColor(0xFFFFFFFF);
                    }

                    canvas.drawRect(left + 1, top + 1, left + cellSize - 1, top + cellSize - 1, cellPaint);

                    int val = playerBoard[r][c];
                    if (val != 0) {
                        String text = String.valueOf(val);
                        float textX = left + cellSize / 2f;
                        float textY = top + cellSize / 2f - (givenTextPaint.descent() + givenTextPaint.ascent()) / 2f;

                        if (given[r][c]) {
                            canvas.drawText(text, textX, textY, givenTextPaint);
                        } else if (hasConflict(r, c)) {
                            canvas.drawText(text, textX, textY, conflictTextPaint);
                        } else {
                            canvas.drawText(text, textX, textY, enteredTextPaint);
                        }
                    }
                }
            }

            // Copyright
            Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
            cp.setColor(0x88888888); cp.setTextSize(12f); cp.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("© Moritzsoft", w - 8, h - 4, cp);

            for (int i = 0; i <= 9; i++) {
                Paint paint = (i % 3 == 0) ? thickLinePaint : thinLinePaint;
                canvas.drawLine(offsetX, offsetY + i * cellSize,
                        offsetX + boardSize, offsetY + i * cellSize, paint);
                canvas.drawLine(offsetX + i * cellSize, offsetY,
                        offsetX + i * cellSize, offsetY + boardSize, paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX() - offsetX;
                float y = event.getY() - offsetY;

                int col = (int) (x / cellSize);
                int row = (int) (y / cellSize);

                if (row >= 0 && row < 9 && col >= 0 && col < 9) {
                    selectedRow = row;
                    selectedCol = col;
                    vibrate(20);
                    invalidate();
                }
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
