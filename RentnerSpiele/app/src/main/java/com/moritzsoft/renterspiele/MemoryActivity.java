package com.moritzsoft.renterspiele;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryActivity extends AppCompatActivity {

    private static final int SHAPE_CIRCLE = 0;
    private static final int SHAPE_SQUARE = 1;
    private static final int SHAPE_TRIANGLE = 2;
    private static final int SHAPE_STAR = 3;
    private static final int SHAPE_CROSS = 4;
    private static final int SHAPE_DIAMOND = 5;
    private static final int SHAPE_HEART = 6;
    private static final int SHAPE_PLUS = 7;

    private static final int[] SHAPE_COLORS = {
            0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFDD835,
            0xFFFB8C00, 0xFF8E24AA, 0xFFEC407A, 0xFF00897B
    };

    private int numPairs;
    private int rows, cols;
    private CardView[] cards;
    private int[] cardSymbols;
    private boolean[] cardRevealed;
    private boolean[] cardMatched;
    private int firstCard = -1;
    private int secondCard = -1;
    private boolean inputLocked = false;
    private int matchedCount = 0;
    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int[][] pairDefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_memory);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Button btn12 = findViewById(R.id.btn12Pairs);
        Button btn16 = findViewById(R.id.btn16Pairs);
        Button btnBackSel = findViewById(R.id.btnBackFromSelection);
        Button btnBackGame = findViewById(R.id.btnBackFromGame);

        btn12.setOnClickListener(v -> { vibrate(30); startGame(12); });
        btn16.setOnClickListener(v -> { vibrate(30); startGame(16); });
        btnBackSel.setOnClickListener(v -> { vibrate(30); finish(); });
        btnBackGame.setOnClickListener(v -> { vibrate(30); finish(); });

        Button btnNew = findViewById(R.id.btnNewMemory);
        btnNew.setOnClickListener(v -> {
            vibrate(30);
            // Go back to selection
            findViewById(R.id.gameScreen).setVisibility(View.GONE);
            findViewById(R.id.selectionScreen).setVisibility(View.VISIBLE);
        });
    }

    private void startGame(int pairs) {
        this.numPairs = pairs;

        if (pairs == 12) { rows = 4; cols = 6; }
        else { rows = 4; cols = 8; }

        int totalCards = rows * cols;
        cards = new CardView[totalCards];
        cardRevealed = new boolean[totalCards];
        cardMatched = new boolean[totalCards];
        matchedCount = 0;
        firstCard = -1;
        secondCard = -1;
        inputLocked = false;

        pairDefs = new int[pairs][2];
        List<int[]> allCombos = new ArrayList<>();
        for (int c = 0; c < SHAPE_COLORS.length; c++) {
            for (int s = 0; s < 8; s++) {
                allCombos.add(new int[]{c, s});
            }
        }
        Collections.shuffle(allCombos);
        for (int i = 0; i < pairs; i++) {
            pairDefs[i] = allCombos.get(i);
        }

        List<Integer> cardIndices = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            cardIndices.add(i);
            cardIndices.add(i);
        }
        Collections.shuffle(cardIndices);
        cardSymbols = new int[totalCards];
        for (int i = 0; i < totalCards; i++) {
            cardSymbols[i] = cardIndices.get(i);
        }

        findViewById(R.id.selectionScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);

        GridLayout grid = findViewById(R.id.cardGrid);
        grid.removeAllViews();
        grid.setRowCount(rows);
        grid.setColumnCount(cols);

        for (int i = 0; i < totalCards; i++) {
            final int idx = i;
            int pairIdx = cardSymbols[i];
            int colorIdx = pairDefs[pairIdx][0];
            int shapeIdx = pairDefs[pairIdx][1];

            CardView card = new CardView(this, SHAPE_COLORS[colorIdx], shapeIdx);
            cards[i] = card;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(i / cols, 1f);
            params.columnSpec = GridLayout.spec(i % cols, 1f);
            params.width = 0;
            params.height = 0;
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            card.setLayoutParams(params);
            card.showBack();
            card.setOnClickListener(v -> onCardClicked(idx));
            grid.addView(card);
        }
    }

    private void onCardClicked(int idx) {
        if (inputLocked || cardMatched[idx] || cardRevealed[idx]) return;

        vibrate(30);
        cardRevealed[idx] = true;
        cards[idx].showFront();

        if (firstCard == -1) {
            firstCard = idx;
        } else {
            secondCard = idx;
            inputLocked = true;

            if (cardSymbols[firstCard] == cardSymbols[secondCard]) {
                cardMatched[firstCard] = true;
                cardMatched[secondCard] = true;
                matchedCount++;
                cards[firstCard].setMatched();
                cards[secondCard].setMatched();

                handler.postDelayed(() -> {
                    firstCard = -1;
                    secondCard = -1;
                    inputLocked = false;

                    if (matchedCount == numPairs) {
                        vibrate(500);
                        Toast.makeText(this, getString(R.string.game_won), Toast.LENGTH_LONG).show();
                        handler.postDelayed(this::finish, 2000);
                    }
                }, 500);
            } else {
                handler.postDelayed(() -> {
                    cards[firstCard].showBack();
                    cards[secondCard].showBack();
                    cardRevealed[firstCard] = false;
                    cardRevealed[secondCard] = false;
                    firstCard = -1;
                    secondCard = -1;
                    inputLocked = false;
                }, 1200);
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

    // ========== CardView ==========

    private static class CardView extends FrameLayout {
        private int shapeColor;
        private int shapeType;
        private boolean showingFront = false;
        private boolean matched = false;
        private Paint shapePaint, backPaint, borderPaint, matchBorderPaint, textPaint;

        public CardView(Context context, int shapeColor, int shapeType) {
            super(context);
            this.shapeColor = shapeColor;
            this.shapeType = shapeType;
            setWillNotDraw(false);

            shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shapePaint.setColor(shapeColor);
            shapePaint.setStyle(Paint.Style.FILL);

            backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backPaint.setColor(0xFFFFFFFF);
            backPaint.setStyle(Paint.Style.FILL);

            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(0xFF888888);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(4f);

            matchBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            matchBorderPaint.setColor(0xFF66BB6A);
            matchBorderPaint.setStyle(Paint.Style.STROKE);
            matchBorderPaint.setStrokeWidth(6f);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(0xFF999999);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        public void showFront() { showingFront = true; invalidate(); }
        public void showBack() { showingFront = false; invalidate(); }
        public void setMatched() { matched = true; invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float r = 16f;

            if (!showingFront) {
                canvas.drawRoundRect(2, 2, w - 2, h - 2, r, r, backPaint);
                canvas.drawRoundRect(2, 2, w - 2, h - 2, r, r, borderPaint);
                textPaint.setTextSize(h * 0.5f);
                textPaint.setColor(0xFF999999);
                canvas.drawText("?", w / 2f, h / 2f + h * 0.17f, textPaint);
            } else {
                if (matched) {
                    backPaint.setColor(0xFFE8F5E9);
                    canvas.drawRoundRect(2, 2, w - 2, h - 2, r, r, backPaint);
                    canvas.drawRoundRect(2, 2, w - 2, h - 2, r, r, matchBorderPaint);
                    backPaint.setColor(0xFFFFFFFF);
                } else {
                    canvas.drawRoundRect(2, 2, w - 2, h - 2, r, r, backPaint);
                    canvas.drawRoundRect(2, 2, w - 2, h - 2, r, r, borderPaint);
                }
                drawShape(canvas, w, h);
            }
        }

        private void drawShape(Canvas canvas, int w, int h) {
            float cx = w / 2f;
            float cy = h / 2f;
            float size = Math.min(w, h) * 0.35f;

            switch (shapeType) {
                case SHAPE_CIRCLE:
                    canvas.drawCircle(cx, cy, size, shapePaint);
                    break;
                case SHAPE_SQUARE:
                    canvas.drawRect(cx - size, cy - size, cx + size, cy + size, shapePaint);
                    break;
                case SHAPE_TRIANGLE: {
                    Path path = new Path();
                    path.moveTo(cx, cy - size);
                    path.lineTo(cx - size, cy + size);
                    path.lineTo(cx + size, cy + size);
                    path.close();
                    canvas.drawPath(path, shapePaint);
                    break;
                }
                case SHAPE_STAR:
                    drawStar(canvas, cx, cy, size);
                    break;
                case SHAPE_CROSS: {
                    shapePaint.setStyle(Paint.Style.STROKE);
                    shapePaint.setStrokeWidth(size * 0.4f);
                    shapePaint.setStrokeCap(Paint.Cap.ROUND);
                    canvas.drawLine(cx - size, cy - size, cx + size, cy + size, shapePaint);
                    canvas.drawLine(cx + size, cy - size, cx - size, cy + size, shapePaint);
                    shapePaint.setStyle(Paint.Style.FILL);
                    shapePaint.setStrokeCap(Paint.Cap.BUTT);
                    break;
                }
                case SHAPE_DIAMOND: {
                    Path path = new Path();
                    path.moveTo(cx, cy - size * 1.2f);
                    path.lineTo(cx + size, cy);
                    path.lineTo(cx, cy + size * 1.2f);
                    path.lineTo(cx - size, cy);
                    path.close();
                    canvas.drawPath(path, shapePaint);
                    break;
                }
                case SHAPE_HEART:
                    drawHeart(canvas, cx, cy, size);
                    break;
                case SHAPE_PLUS: {
                    float thickness = size * 0.45f;
                    canvas.drawRect(cx - thickness, cy - size, cx + thickness, cy + size, shapePaint);
                    canvas.drawRect(cx - size, cy - thickness, cx + size, cy + thickness, shapePaint);
                    break;
                }
            }
        }

        private void drawStar(Canvas canvas, float cx, float cy, float size) {
            Path path = new Path();
            double startAngle = -Math.PI / 2;
            for (int i = 0; i < 5; i++) {
                double outerAngle = startAngle + (i * 2 * Math.PI / 5);
                double innerAngle = outerAngle + Math.PI / 5;
                float ox = cx + (float) (size * Math.cos(outerAngle));
                float oy = cy + (float) (size * Math.sin(outerAngle));
                float ix = cx + (float) (size * 0.45f * Math.cos(innerAngle));
                float iy = cy + (float) (size * 0.45f * Math.sin(innerAngle));
                if (i == 0) path.moveTo(ox, oy);
                else path.lineTo(ox, oy);
                path.lineTo(ix, iy);
            }
            path.close();
            canvas.drawPath(path, shapePaint);
        }

        private void drawHeart(Canvas canvas, float cx, float cy, float size) {
            Path path = new Path();
            float top = cy - size * 0.5f;
            path.moveTo(cx, cy + size);
            path.cubicTo(cx - size * 2, cy - size * 0.5f,
                    cx - size * 0.5f, top - size,
                    cx, top + size * 0.2f);
            path.cubicTo(cx + size * 0.5f, top - size,
                    cx + size * 2, cy - size * 0.5f,
                    cx, cy + size);
            path.close();
            canvas.drawPath(path, shapePaint);
        }
    }
}
