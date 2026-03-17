package com.moritzsoft.renterspiele;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WortfinderActivity extends AppCompatActivity {

    // Only senior-friendly directions: right and down
    private static final int[][] DIRECTIONS = {
            {1, 0},  // horizontal right
            {0, 1},  // vertical down
    };

    private int gridCols = 10;
    private int gridRows = 8;
    private int wordsPerPuzzle = 5;

    private char[][] grid;
    private boolean[][] highlighted;
    private int[][] highlightColor;
    private List<String> currentWords;
    private boolean[] wordFound;
    private List<int[]> currentWordPositions;
    private int foundCount = 0;

    private WordGridView gridView;
    private LinearLayout wordListLayout;
    private TextView[] wordLabels;
    private TextView txtCategory;
    private TextView txtFoundCount;
    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    private static final int[] HIGHLIGHT_COLORS = {
            0xFF66BB6A, 0xFF42A5F5, 0xFFFFCA28, 0xFFEF5350, 0xFFAB47BC,
            0xFF26A69A, 0xFFFF7043, 0xFF5C6BC0
    };

    // Word database by category
    private static final String[][] WORD_CATEGORIES = {
            {"Tiere", "HUND", "KATZE", "PFERD", "VOGEL", "FISCH", "MAUS", "HASE", "RIND",
                    "HUHN", "ENTE", "GANS", "SCHAF", "ZIEGE", "RABE", "ADLER", "LACHS",
                    "FUCHS", "DACHS", "IGEL", "WOLF", "ESEL", "TIGER", "ZEBRA", "AFFE"},
            {"Essen", "BROT", "KUCHEN", "SUPPE", "SALAT", "NUDEL", "REIS",
                    "APFEL", "BIRNE", "TOMATE", "GURKE", "BUTTER", "MILCH", "SAHNE", "HONIG",
                    "WURST", "BRATEN", "BOHNE", "ERBSE", "KOHL", "TORTE", "KEKS",
                    "KAKAO", "KAFFEE", "SAFT", "WASSER", "MEHL", "ZUCKER"},
            {"Haushalt", "TISCH", "STUHL", "LAMPE", "SOFA", "BETT", "REGAL",
                    "SPIEGEL", "KISSEN", "DECKE", "TELLER", "TASSE", "GABEL", "MESSER",
                    "TOPF", "PFANNE", "GLAS", "DOSE", "EIMER", "BESEN", "SEIFE",
                    "KERZE", "VASE", "BILD", "UHR", "OFEN", "HERD", "BUCH"},
            {"Natur", "BAUM", "BLUME", "GRAS", "WALD", "WIESE", "BERG",
                    "FLUSS", "BACH", "MEER", "STEIN", "FELS", "SAND", "ERDE",
                    "PILZ", "BUSCH", "ROSE", "TULPE", "EICHE", "BIRKE", "TANNE",
                    "LINDE", "AHORN", "WEIDE", "KLEE", "FARN", "BLATT", "ZWEIG"},
            {"Kleidung", "HOSE", "HEMD", "ROCK", "KLEID", "JACKE", "MANTEL",
                    "SCHAL", "SOCKE", "SCHUH", "GURT", "BLUSE", "PULLI", "WESTE",
                    "RING", "KETTE", "BRILLE", "STOFF", "SEIDE", "WOLLE", "LEDER"},
            {"Berufe", "ARZT", "KOCH", "LEHRER", "MALER", "PILOT", "BAUER",
                    "RICHTER", "FAHRER", "MAURER", "MUSIKER", "DICHTER", "KELLNER",
                    "FORSCHER", "MAKLER", "BERATER"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_wortfinder);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Difficulty buttons
        Button btnEasy = findViewById(R.id.btnEasy);
        Button btnMedium = findViewById(R.id.btnMedium);
        Button btnHard = findViewById(R.id.btnHard);
        Button btnBackDiff = findViewById(R.id.btnBackDiff);

        btnEasy.setOnClickListener(v -> { vibrate(30); startGameWithDifficulty(0); });
        btnMedium.setOnClickListener(v -> { vibrate(30); startGameWithDifficulty(1); });
        btnHard.setOnClickListener(v -> { vibrate(30); startGameWithDifficulty(2); });
        btnBackDiff.setOnClickListener(v -> { vibrate(30); finish(); });

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> { vibrate(30); finish(); });

        Button btnNewPuzzle = findViewById(R.id.btnNewPuzzle);
        btnNewPuzzle.setOnClickListener(v -> { vibrate(30); generatePuzzle(); });

        Button btnHint = findViewById(R.id.btnHint);
        btnHint.setOnClickListener(v -> { vibrate(30); giveHint(); });

        wordListLayout = findViewById(R.id.wordListLayout);
        txtCategory = findViewById(R.id.txtCategory);
        txtFoundCount = findViewById(R.id.txtFoundCount);
    }

    private void startGameWithDifficulty(int level) {
        switch (level) {
            case 0: gridCols = 8; gridRows = 6; wordsPerPuzzle = 4; break;
            case 1: gridCols = 10; gridRows = 8; wordsPerPuzzle = 6; break;
            case 2: gridCols = 12; gridRows = 10; wordsPerPuzzle = 8; break;
        }
        findViewById(R.id.difficultyScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);
        generatePuzzle();
    }

    private void generatePuzzle() {
        // Pick random category
        int catIdx = random.nextInt(WORD_CATEGORIES.length);
        String[] category = WORD_CATEGORIES[catIdx];
        String categoryName = category[0];
        txtCategory.setText(categoryName);

        // Pick words that fit
        List<String> allWords = new ArrayList<>();
        for (int i = 1; i < category.length; i++) {
            String w = category[i];
            if (w.length() <= Math.max(gridCols, gridRows)) {
                allWords.add(w);
            }
        }
        Collections.shuffle(allWords, random);

        grid = new char[gridRows][gridCols];
        highlighted = new boolean[gridRows][gridCols];
        highlightColor = new int[gridRows][gridCols];
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                grid[r][c] = 0;
                highlightColor[r][c] = -1;
            }
        }

        currentWords = new ArrayList<>();
        currentWordPositions = new ArrayList<>();
        int placed = 0;

        for (String word : allWords) {
            if (placed >= wordsPerPuzzle) break;
            if (tryPlaceWord(word)) {
                currentWords.add(word);
                placed++;
            }
        }

        // Fill remaining cells with random letters
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (grid[r][c] == 0) {
                    grid[r][c] = (char) ('A' + random.nextInt(26));
                }
            }
        }

        wordFound = new boolean[currentWords.size()];
        foundCount = 0;

        // Build word list UI
        buildWordList();
        updateFoundCount();

        // Create grid view
        FrameLayout container = findViewById(R.id.gridContainer);
        container.removeAllViews();
        gridView = new WordGridView(this);
        container.addView(gridView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private boolean tryPlaceWord(String word) {
        List<int[]> attempts = new ArrayList<>();
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                for (int[] dir : DIRECTIONS) {
                    attempts.add(new int[]{r, c, dir[0], dir[1]});
                }
            }
        }
        Collections.shuffle(attempts, random);

        for (int[] attempt : attempts) {
            int row = attempt[0], col = attempt[1];
            int dx = attempt[2], dy = attempt[3];
            if (canPlace(word, row, col, dx, dy)) {
                placeWord(word, row, col, dx, dy);
                currentWordPositions.add(new int[]{row, col, dx, dy});
                return true;
            }
        }
        return false;
    }

    private boolean canPlace(String word, int row, int col, int dx, int dy) {
        for (int i = 0; i < word.length(); i++) {
            int r = row + i * dy;
            int c = col + i * dx;
            if (r < 0 || r >= gridRows || c < 0 || c >= gridCols) return false;
            if (grid[r][c] != 0 && grid[r][c] != word.charAt(i)) return false;
        }
        return true;
    }

    private void placeWord(String word, int row, int col, int dx, int dy) {
        for (int i = 0; i < word.length(); i++) {
            grid[row + i * dy][col + i * dx] = word.charAt(i);
        }
    }

    private void buildWordList() {
        wordListLayout.removeAllViews();
        wordLabels = new TextView[currentWords.size()];
        for (int i = 0; i < currentWords.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(currentWords.get(i));
            tv.setTextSize(32);
            tv.setTextColor(0xFF1A1A1A);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setPadding(0, dpToPx(6), 0, dpToPx(6));
            wordLabels[i] = tv;
            wordListLayout.addView(tv);
        }
    }

    private void updateFoundCount() {
        txtFoundCount.setText(String.format(getString(R.string.wortfinder_found), foundCount, currentWords.size()));
    }

    private void giveHint() {
        for (int i = 0; i < currentWords.size(); i++) {
            if (!wordFound[i]) {
                int[] pos = currentWordPositions.get(i);
                // Highlight first letter
                int r = pos[0];
                int c = pos[1];
                highlighted[r][c] = true;
                highlightColor[r][c] = i % HIGHLIGHT_COLORS.length;
                if (gridView != null) gridView.invalidate();
                break;
            }
        }
    }

    private void checkSelection(int startRow, int startCol, int endRow, int endCol) {
        for (int i = 0; i < currentWords.size(); i++) {
            if (wordFound[i]) continue;
            int[] pos = currentWordPositions.get(i);
            String word = currentWords.get(i);
            int wStartRow = pos[0];
            int wStartCol = pos[1];
            int dx = pos[2];
            int dy = pos[3];
            int wEndRow = wStartRow + (word.length() - 1) * dy;
            int wEndCol = wStartCol + (word.length() - 1) * dx;

            if ((startRow == wStartRow && startCol == wStartCol && endRow == wEndRow && endCol == wEndCol) ||
                    (startRow == wEndRow && startCol == wEndCol && endRow == wStartRow && endCol == wStartCol)) {
                // Found!
                wordFound[i] = true;
                foundCount++;
                vibrate(30);

                int colorIdx = i % HIGHLIGHT_COLORS.length;
                for (int j = 0; j < word.length(); j++) {
                    int r = wStartRow + j * dy;
                    int c = wStartCol + j * dx;
                    highlighted[r][c] = true;
                    highlightColor[r][c] = colorIdx;
                }

                wordLabels[i].setTextColor(HIGHLIGHT_COLORS[colorIdx]);
                wordLabels[i].setPaintFlags(wordLabels[i].getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                updateFoundCount();

                if (foundCount == currentWords.size()) {
                    vibrate(500);
                    Toast.makeText(this, getString(R.string.wortfinder_all_found), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

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

    // ========== Word Grid View ==========

    private class WordGridView extends View {
        private Paint cellPaint, gridPaint, textPaint, highlightPaint, selectionPaint;
        private float cellWidth, cellHeight, offsetX, offsetY;
        private boolean dragging = false;
        private int dragStartRow = -1, dragStartCol = -1;
        private int dragEndRow = -1, dragEndCol = -1;

        public WordGridView(Context context) {
            super(context);

            cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cellPaint.setColor(0xFFFFFFFF);
            cellPaint.setStyle(Paint.Style.FILL);

            gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gridPaint.setColor(0xFFBBBBBB);
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeWidth(2f);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(0xFF1A1A1A);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlightPaint.setStyle(Paint.Style.FILL);

            selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            selectionPaint.setColor(0x4400BCD4);
            selectionPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();

            float cellW = (float) w / gridCols;
            float cellH = (float) h / gridRows;
            float cellSize = Math.min(cellW, cellH);
            cellWidth = cellSize;
            cellHeight = cellSize;
            offsetX = (w - cellSize * gridCols) / 2f;
            offsetY = (h - cellSize * gridRows) / 2f;
            textPaint.setTextSize(cellSize * 0.55f);

            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    float left = offsetX + c * cellWidth;
                    float top = offsetY + r * cellHeight;
                    RectF rect = new RectF(left + 2, top + 2, left + cellWidth - 2, top + cellHeight - 2);

                    if (highlighted[r][c] && highlightColor[r][c] >= 0) {
                        highlightPaint.setColor(HIGHLIGHT_COLORS[highlightColor[r][c]]);
                        highlightPaint.setAlpha(100);
                        canvas.drawRoundRect(rect, 8, 8, highlightPaint);
                        highlightPaint.setAlpha(255);
                    } else {
                        canvas.drawRoundRect(rect, 8, 8, cellPaint);
                    }
                    canvas.drawRoundRect(rect, 8, 8, gridPaint);

                    float textX = left + cellWidth / 2f;
                    float textY = top + cellHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                    textPaint.setColor(highlighted[r][c] ? 0xFFFFFFFF : 0xFF1A1A1A);
                    canvas.drawText(String.valueOf(grid[r][c]), textX, textY, textPaint);
                }
            }

            // Copyright
            Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
            cp.setColor(0x88888888); cp.setTextSize(12f); cp.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("© Moritzsoft", w - 8, h - 4, cp);

            if (dragging && dragStartRow >= 0 && dragEndRow >= 0) {
                drawSelectionLine(canvas);
            }
        }

        private void drawSelectionLine(Canvas canvas) {
            int dr = 0, dc = 0;
            int rowDiff = dragEndRow - dragStartRow;
            int colDiff = dragEndCol - dragStartCol;

            if (rowDiff != 0) dr = rowDiff > 0 ? 1 : -1;
            if (colDiff != 0) dc = colDiff > 0 ? 1 : -1;

            int len;
            if (rowDiff == 0 && colDiff == 0) len = 1;
            else if (rowDiff == 0) len = Math.abs(colDiff) + 1;
            else if (colDiff == 0) len = Math.abs(rowDiff) + 1;
            else if (Math.abs(rowDiff) == Math.abs(colDiff)) len = Math.abs(rowDiff) + 1;
            else return;

            for (int i = 0; i < len; i++) {
                int r = dragStartRow + i * dr;
                int c = dragStartCol + i * dc;
                if (r >= 0 && r < gridRows && c >= 0 && c < gridCols) {
                    float left = offsetX + c * cellWidth;
                    float top = offsetY + r * cellHeight;
                    RectF rect = new RectF(left + 2, top + 2, left + cellWidth - 2, top + cellHeight - 2);
                    canvas.drawRoundRect(rect, 8, 8, selectionPaint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            int col = (int) ((x - offsetX) / cellWidth);
            int row = (int) ((y - offsetY) / cellHeight);
            col = Math.max(0, Math.min(col, gridCols - 1));
            row = Math.max(0, Math.min(row, gridRows - 1));

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dragStartRow = row;
                    dragStartCol = col;
                    dragEndRow = row;
                    dragEndCol = col;
                    dragging = true;
                    invalidate();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int rowD = row - dragStartRow;
                    int colD = col - dragStartCol;

                    // Snap to horizontal or vertical only
                    if (Math.abs(rowD) > Math.abs(colD)) {
                        dragEndRow = row;
                        dragEndCol = dragStartCol;
                    } else {
                        dragEndRow = dragStartRow;
                        dragEndCol = col;
                    }

                    dragEndRow = Math.max(0, Math.min(dragEndRow, gridRows - 1));
                    dragEndCol = Math.max(0, Math.min(dragEndCol, gridCols - 1));
                    invalidate();
                    return true;

                case MotionEvent.ACTION_UP:
                    dragging = false;
                    checkSelection(dragStartRow, dragStartCol, dragEndRow, dragEndCol);
                    invalidate();
                    return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
