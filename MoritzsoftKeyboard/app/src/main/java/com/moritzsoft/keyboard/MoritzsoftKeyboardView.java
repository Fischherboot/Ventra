package com.moritzsoft.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import java.util.List;

public class MoritzsoftKeyboardView extends KeyboardView {

    private Paint mKeyPaint;
    private Paint mBorderPaint;
    private Paint mLetterPaint;
    private Paint mGradientIconPaint;
    private Paint mGradientTextPaint;
    private Paint mPopupBgPaint;
    private Paint mPopupBorderPaint;
    private Paint mPopupTextPaint;
    private Paint mPopupHighlightPaint;
    private float mCornerRadius;
    private float mDensity;
    private Path mPath;
    private SoftKeyboard mService;

    // --- Direct touch tracking for press animation ---
    private int mTouchKeyCode = Integer.MIN_VALUE;

    // --- Space swipe ---
    private boolean mSpaceSwipeActive = false;
    private float mSpaceSwipeStartX = Float.MIN_VALUE;
    private float mSpaceSwipeLastX;
    private float mSpaceSwipeAccum;
    private boolean mSpaceTouchDown = false;
    private static final float SWIPE_THRESHOLD_DP = 14;
    private static final float SWIPE_START_THRESHOLD_DP = 8;

    // --- Custom popup ---
    private boolean mPopupActive = false;
    private boolean mPopupFromLongPress = false; // true = finger still held from long press
    private Key mPopupKey;
    private String[] mPopupChars;
    private RectF[] mPopupCellRects;
    private RectF mPopupStripRect;
    private int mPopupSelectedIndex = -1;

    private static final int COLOR_START = 0xFF8c52ff;
    private static final int COLOR_END = 0xFFff914d;

    // --- Custom Long Press ---
    private Runnable mLongPressRunnable;
    private float mDownX, mDownY;
    private static final int LONG_PRESS_TIMEOUT = 200;

    public MoritzsoftKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MoritzsoftKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setService(SoftKeyboard service) {
        mService = service;
    }

    private void init(Context context) {
        mDensity = context.getResources().getDisplayMetrics().density;
        mCornerRadius = 8 * mDensity;
        mPath = new Path();

        mKeyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLetterPaint.setColor(0xFFFFFFFF);
        mLetterPaint.setTextAlign(Paint.Align.CENTER);

        mGradientIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGradientIconPaint.setStyle(Paint.Style.STROKE);
        mGradientIconPaint.setStrokeCap(Paint.Cap.ROUND);
        mGradientIconPaint.setStrokeJoin(Paint.Join.ROUND);

        mGradientTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGradientTextPaint.setTextAlign(Paint.Align.CENTER);
        mGradientTextPaint.setFakeBoldText(true);

        mPopupBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPopupBgPaint.setColor(0xFF1A1A1A);

        mPopupBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPopupBorderPaint.setStyle(Paint.Style.STROKE);

        mPopupTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPopupTextPaint.setColor(0xFFFFFFFF);
        mPopupTextPaint.setTextAlign(Paint.Align.CENTER);

        mPopupHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLongPressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mTouchKeyCode != Integer.MIN_VALUE) {
                    Keyboard keyboard = getKeyboard();
                    if (keyboard != null) {
                        for (Key key : keyboard.getKeys()) {
                            if (key.codes[0] == mTouchKeyCode) {
                                onLongPress(key);
                                break;
                            }
                        }
                    }
                }
            }
        };
    }

    // For SoftKeyboard callbacks - we don't use these for animation anymore
    public void setPressedKeyCode(int code) { }
    public void clearPressedKey() { }

    public void doHaptic() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    // ====================== KEY CLASSIFICATION ======================

    private static final int TYPE_LETTER = 0;
    private static final int TYPE_SPACE = 1;
    private static final int TYPE_ICON = 2;
    private static final int TYPE_SPECIAL_TEXT = 3;

    private int getKeyType(Key key) {
        int code = key.codes[0];
        if (code == 32) return TYPE_SPACE;
        if (code == Keyboard.KEYCODE_SHIFT || code == Keyboard.KEYCODE_DELETE
                || code == -3 || code == 10) return TYPE_ICON;
        if (code == Keyboard.KEYCODE_MODE_CHANGE) return TYPE_SPECIAL_TEXT;
        if (key.label != null) {
            String label = key.label.toString();
            if (label.equals(". ,") || label.equals(",") || label.equals("…"))
                return TYPE_SPECIAL_TEXT;
        }
        return TYPE_LETTER;
    }

    private boolean isKeyPressed(Key key) {
        return key.codes[0] == mTouchKeyCode;
    }

    private Key findKeyAt(float x, float y) {
        Keyboard keyboard = getKeyboard();
        if (keyboard == null) return null;
        for (Key key : keyboard.getKeys()) {
            if (x >= key.x && x < key.x + key.width
                    && y >= key.y && y < key.y + key.height)
                return key;
        }
        return null;
    }

    // ====================== CUSTOM POPUP ======================

    private void showPopup(Key key) {
        mPopupActive = true;
        mPopupFromLongPress = true;
        mPopupKey = key;

        String popups = (key.popupCharacters != null) ? key.popupCharacters.toString() : "";
        boolean shifted = isShifted();

        // ONLY derivatives, not the original letter
        mPopupChars = new String[popups.length()];
        for (int i = 0; i < popups.length(); i++) {
            String ch = String.valueOf(popups.charAt(i));
            if (shifted && Character.isLetter(popups.charAt(i))) {
                ch = ch.toUpperCase();
            }
            mPopupChars[i] = ch;
        }

        mPopupSelectedIndex = -1; // nothing selected initially
        calculatePopupLayout();
        invalidate();
    }

    private void calculatePopupLayout() {
        int count = mPopupChars.length;
        float cellW = 44 * mDensity;
        float cellH = mPopupKey.height * 0.82f;

        int cols, rows;
        if (count <= 3) {
            cols = count;
            rows = 1;
        } else {
            // 2-row grid
            cols = (count + 1) / 2; // ceil division
            rows = 2;
        }

        float stripW = cellW * cols;
        float stripH = cellH * rows;
        float screenW = getWidth();

        // Center on key
        float keyCx = mPopupKey.x + mPopupKey.width / 2f;
        float stripLeft = keyCx - stripW / 2f;
        float stripRight = stripLeft + stripW;

        // Clamp to screen edges
        if (stripLeft < 4 * mDensity) {
            stripLeft = 4 * mDensity;
            stripRight = stripLeft + stripW;
        }
        if (stripRight > screenW - 4 * mDensity) {
            stripRight = screenW - 4 * mDensity;
            stripLeft = stripRight - stripW;
        }

        float stripTop = mPopupKey.y - stripH - 4 * mDensity;

        // Fix for top row: if popup goes off-top, put it over the key (align top of popup to top of view + margin)
        if (stripTop < 4 * mDensity) {
            stripTop = 4 * mDensity;
        }

        float stripBot = stripTop + stripH;

        mPopupStripRect = new RectF(stripLeft, stripTop, stripRight, stripBot);
        mPopupCellRects = new RectF[count];
        for (int i = 0; i < count; i++) {
            int row = (rows == 1) ? 0 : (i / cols);
            int col = (rows == 1) ? i : (i % cols);
            float l = stripLeft + col * cellW;
            float t = stripTop + row * cellH;
            mPopupCellRects[i] = new RectF(l, t, l + cellW, t + cellH);
        }
    }

    private int findPopupCellAt(float x, float y) {
        if (mPopupCellRects == null) return -1;
        // Check popup strip area (with small margin)
        float margin = 6 * mDensity;
        RectF area = new RectF(
                mPopupStripRect.left - margin,
                mPopupStripRect.top - margin,
                mPopupStripRect.right + margin,
                mPopupStripRect.bottom + margin);
        if (!area.contains(x, y)) return -1;
        for (int i = 0; i < mPopupCellRects.length; i++) {
            if (x >= mPopupCellRects[i].left - margin && x < mPopupCellRects[i].right + margin
                    && y >= mPopupCellRects[i].top - margin && y < mPopupCellRects[i].bottom + margin) {
                return i;
            }
        }
        return -1;
    }

    private boolean isOverOriginalKey(float x, float y) {
        if (mPopupKey == null) return false;
        return x >= mPopupKey.x && x < mPopupKey.x + mPopupKey.width
                && y >= mPopupKey.y && y < mPopupKey.y + mPopupKey.height;
    }

    private void commitPopupChar(int index) {
        if (index >= 0 && index < mPopupChars.length) {
            String ch = mPopupChars[index];
            getOnKeyboardActionListener().onText(ch);
        }
        dismissPopup();
    }

    private void dismissPopup() {
        mPopupActive = false;
        mPopupFromLongPress = false;
        mPopupKey = null;
        mPopupChars = null;
        mPopupCellRects = null;
        mPopupStripRect = null;
        mPopupSelectedIndex = -1;
        invalidate();
    }

    // ====================== TOUCH HANDLING ======================

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        float x = me.getX();
        float y = me.getY();
        int action = me.getAction();

        // ======== POPUP MODE ========
        if (mPopupActive) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    // New touch while popup is open (finger was lifted before)
                    int cell = findPopupCellAt(x, y);
                    if (cell >= 0) {
                        // Tapped a popup cell → commit
                        commitPopupChar(cell);
                    } else {
                        // Tapped outside popup → just dismiss, no key action
                        dismissPopup();
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mPopupFromLongPress) {
                        // Finger still held from long press - update selection
                        int cell = findPopupCellAt(x, y);
                        if (cell != mPopupSelectedIndex) {
                            mPopupSelectedIndex = cell;
                            if (cell >= 0) doHaptic();
                            invalidate();
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (mPopupFromLongPress) {
                        // Finger lifted from long press
                        mPopupFromLongPress = false;
                        int cell = findPopupCellAt(x, y);
                        if (cell >= 0) {
                            // Released on a popup char → commit
                            commitPopupChar(cell);
                        } else {
                            // Released on original key or elsewhere → popup stays open
                            mPopupSelectedIndex = -1;
                            invalidate();
                        }
                    }
                    // Reset super state
                    MotionEvent cancel = MotionEvent.obtain(me);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(cancel);
                    cancel.recycle();
                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    dismissPopup();
                    return true;
                }
            }
            return true;
        }

        // ======== NORMAL MODE ========

        // Track which key is touched for press animation
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                Key key = findKeyAt(x, y);
                mTouchKeyCode = (key != null) ? key.codes[0] : Integer.MIN_VALUE;
                invalidate();

                mDownX = x;
                mDownY = y;
                removeCallbacks(mLongPressRunnable);
                if (key != null) {
                    postDelayed(mLongPressRunnable, LONG_PRESS_TIMEOUT);
                }

                // Space swipe tracking
                mSpaceSwipeActive = false;
                mSpaceTouchDown = false;
                mSpaceSwipeStartX = Float.MIN_VALUE;
                if (key != null && key.codes[0] == 32) {
                    mSpaceTouchDown = true;
                    mSpaceSwipeStartX = x;
                    mSpaceSwipeLastX = x;
                    mSpaceSwipeAccum = 0;
                }
                return super.onTouchEvent(me);
            }

            case MotionEvent.ACTION_MOVE: {
                float dist = (float) Math.hypot(x - mDownX, y - mDownY);
                if (dist > SWIPE_START_THRESHOLD_DP * mDensity) {
                    removeCallbacks(mLongPressRunnable);
                }

                // Update press tracking
                if (!mSpaceSwipeActive) {
                    Key key = findKeyAt(x, y);
                    int newCode = (key != null) ? key.codes[0] : Integer.MIN_VALUE;
                    if (newCode != mTouchKeyCode) {
                        mTouchKeyCode = newCode;
                        invalidate();
                        removeCallbacks(mLongPressRunnable);
                    }
                }

                // Space swipe
                if (mSpaceTouchDown && mSpaceSwipeStartX != Float.MIN_VALUE) {
                    float dx = x - mSpaceSwipeStartX;
                    float threshold = SWIPE_START_THRESHOLD_DP * mDensity;

                    if (!mSpaceSwipeActive && Math.abs(dx) > threshold) {
                        mSpaceSwipeActive = true;
                        mSpaceSwipeLastX = x;
                        mSpaceSwipeAccum = 0;
                        mTouchKeyCode = Integer.MIN_VALUE;
                        invalidate();
                        removeCallbacks(mLongPressRunnable);
                        MotionEvent cancel = MotionEvent.obtain(me);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.onTouchEvent(cancel);
                        cancel.recycle();
                        return true;
                    }

                    if (mSpaceSwipeActive) {
                        float stepPx = SWIPE_THRESHOLD_DP * mDensity;
                        mSpaceSwipeAccum += (x - mSpaceSwipeLastX);
                        mSpaceSwipeLastX = x;

                        while (Math.abs(mSpaceSwipeAccum) >= stepPx) {
                            if (mService != null) {
                                mService.moveCursor(mSpaceSwipeAccum > 0 ? 1 : -1);
                                doHaptic();
                            }
                            mSpaceSwipeAccum -= (mSpaceSwipeAccum > 0 ? stepPx : -stepPx);
                        }
                        return true;
                    }
                }
                return super.onTouchEvent(me);
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                removeCallbacks(mLongPressRunnable);

                // Clear press animation
                mTouchKeyCode = Integer.MIN_VALUE;
                invalidate();

                if (mSpaceSwipeActive) {
                    mSpaceSwipeActive = false;
                    mSpaceTouchDown = false;
                    mSpaceSwipeStartX = Float.MIN_VALUE;
                    return true; // eat UP → no space typed
                }
                mSpaceTouchDown = false;
                mSpaceSwipeStartX = Float.MIN_VALUE;
                return super.onTouchEvent(me);
            }
        }
        return super.onTouchEvent(me);
    }

    // ====================== LONG PRESS → POPUP ======================

    @Override
    protected boolean onLongPress(Key key) {
        if (key.popupCharacters != null && key.popupCharacters.length() > 0) {
            showPopup(key);
            return true;
        }
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(-100, null);
            return true;
        }
        return super.onLongPress(key);
    }

    // ====================== DRAWING ======================

    @Override
    public void onDraw(Canvas canvas) {
        Keyboard keyboard = getKeyboard();
        if (keyboard == null) return;

        List<Key> keys = keyboard.getKeys();
        float pad = 3 * mDensity;
        float borderWidth = 2.5f * mDensity;
        float textSize = getResources().getDimension(R.dimen.key_text_size);
        float labelSize = getResources().getDimension(R.dimen.key_label_size);
        boolean shifted = isShifted();

        mLetterPaint.setTextSize(textSize);
        mGradientTextPaint.setTextSize(labelSize);

        for (Key key : keys) {
            boolean pressed = isKeyPressed(key);
            int keyType = getKeyType(key);
            boolean special = (keyType == TYPE_ICON || keyType == TYPE_SPECIAL_TEXT);

            float shrink = pressed ? 2.5f * mDensity : 0;
            float left = key.x + pad + shrink;
            float top = key.y + pad + shrink;
            float right = key.x + key.width - pad - shrink;
            float bottom = key.y + key.height - pad - shrink;
            RectF rect = new RectF(left, top, right, bottom);

            if (special) {
                float bL = left - borderWidth;
                float bT = top - borderWidth;
                float bR = right + borderWidth;
                float bB = bottom + borderWidth;
                RectF borderRect = new RectF(bL, bT, bR, bB);

                LinearGradient borderGrad = new LinearGradient(
                        bL, bT, bR, bT, COLOR_START, COLOR_END, Shader.TileMode.CLAMP);
                mBorderPaint.setShader(borderGrad);
                canvas.drawRoundRect(borderRect, mCornerRadius, mCornerRadius, mBorderPaint);

                mKeyPaint.setShader(null);
                mKeyPaint.setColor(pressed ? 0xFF2A2A2A : 0xFF1A1A1A);
                canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mKeyPaint);

                if (keyType == TYPE_ICON) {
                    drawIcon(canvas, key, left, top, right, bottom);
                } else {
                    drawGradientText(canvas, key, left, top, right, bottom, labelSize);
                }
            } else if (keyType == TYPE_SPACE) {
                mKeyPaint.setShader(null);
                mKeyPaint.setColor(pressed ? 0xFF2A2A2A : 0xFF1A1A1A);
                canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mKeyPaint);
            } else {
                mKeyPaint.setShader(null);
                mKeyPaint.setColor(pressed ? 0xFF2A2A2A : 0xFF1A1A1A);
                canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mKeyPaint);

                if (key.label != null) {
                    String label = key.label.toString();
                    if (shifted) label = label.toUpperCase();
                    mLetterPaint.setTextSize(textSize);
                    Paint.FontMetrics fm = mLetterPaint.getFontMetrics();
                    float textY = (top + bottom) / 2f - (fm.ascent + fm.descent) / 2f;
                    canvas.drawText(label, (left + right) / 2f, textY, mLetterPaint);
                }
            }
        }

        // Draw popup on top
        if (mPopupActive && mPopupChars != null && mPopupCellRects != null) {
            drawPopup(canvas);
        }
    }

    private void drawPopup(Canvas canvas) {
        float borderW = 1.5f * mDensity;
        float radius = 6 * mDensity;

        // Gradient border
        LinearGradient popupGrad = new LinearGradient(
                mPopupStripRect.left, mPopupStripRect.top,
                mPopupStripRect.right, mPopupStripRect.top,
                COLOR_START, COLOR_END, Shader.TileMode.CLAMP);
        mPopupBorderPaint.setShader(popupGrad);
        mPopupBorderPaint.setStrokeWidth(borderW);
        canvas.drawRoundRect(mPopupStripRect, radius, radius, mPopupBorderPaint);

        // Dark fill
        RectF inner = new RectF(
                mPopupStripRect.left + borderW / 2,
                mPopupStripRect.top + borderW / 2,
                mPopupStripRect.right - borderW / 2,
                mPopupStripRect.bottom - borderW / 2);
        mPopupBgPaint.setColor(0xFF1A1A1A);
        canvas.drawRoundRect(inner, radius, radius, mPopupBgPaint);

        // Draw cells
        float textSize = getResources().getDimension(R.dimen.key_text_size) * 0.9f;
        mPopupTextPaint.setTextSize(textSize);
        Paint.FontMetrics fm = mPopupTextPaint.getFontMetrics();

        for (int i = 0; i < mPopupChars.length; i++) {
            RectF cell = mPopupCellRects[i];

            if (i == mPopupSelectedIndex) {
                LinearGradient hlGrad = new LinearGradient(
                        cell.left, cell.top, cell.right, cell.top,
                        COLOR_START, COLOR_END, Shader.TileMode.CLAMP);
                mPopupHighlightPaint.setShader(hlGrad);
                canvas.drawRoundRect(cell, radius, radius, mPopupHighlightPaint);
                mPopupTextPaint.setColor(0xFF000000);
            } else {
                mPopupTextPaint.setColor(0xFFFFFFFF);
            }

            float textY = (cell.top + cell.bottom) / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(mPopupChars[i], (cell.left + cell.right) / 2f, textY, mPopupTextPaint);
        }
    }

    // ====================== GRADIENT TEXT & ICONS ======================

    private void drawGradientText(Canvas canvas, Key key, float l, float t, float r, float b, float size) {
        if (key.label == null) return;
        mGradientTextPaint.setTextSize(size);
        LinearGradient tg = new LinearGradient(l, t, r, t, COLOR_START, COLOR_END, Shader.TileMode.CLAMP);
        mGradientTextPaint.setShader(tg);
        Paint.FontMetrics fm = mGradientTextPaint.getFontMetrics();
        float textY = (t + b) / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(key.label.toString(), (l + r) / 2f, textY, mGradientTextPaint);
    }

    private void drawIcon(Canvas canvas, Key key, float l, float t, float r, float b) {
        float cx = (l + r) / 2f;
        float cy = (t + b) / 2f;
        float w = (r - l);
        float h = (b - t);
        float iconSize = Math.min(w, h) * 0.38f;
        float strokeW = 1.8f * mDensity;

        LinearGradient iconGrad = new LinearGradient(
                cx - iconSize, cy, cx + iconSize, cy,
                COLOR_START, COLOR_END, Shader.TileMode.CLAMP);

        mGradientIconPaint.setShader(iconGrad);
        mGradientIconPaint.setStrokeWidth(strokeW);
        mGradientIconPaint.setStyle(Paint.Style.STROKE);
        mGradientIconPaint.setPathEffect(null);

        int code = key.codes[0];
        mPath.reset();

        if (code == Keyboard.KEYCODE_SHIFT) {
            drawShiftArrow(canvas, cx, cy, iconSize, strokeW);
        } else if (code == Keyboard.KEYCODE_DELETE) {
            drawBackspace(canvas, cx, cy, iconSize, strokeW);
        } else if (code == 10) {
            drawReturnArrow(canvas, cx, cy, iconSize, strokeW);
        } else if (code == -3) {
            drawCheckmark(canvas, cx, cy, iconSize, strokeW);
        }
    }

    private void drawShiftArrow(Canvas canvas, float cx, float cy, float s, float sw) {
        boolean shifted = isShifted();
        int shiftState = (mService != null) ? mService.getShiftState() : (shifted ? 1 : 0);

        mGradientIconPaint.setStyle(Paint.Style.STROKE);
        mGradientIconPaint.setStrokeWidth(sw);

        mPath.reset();
        if (shifted) {
            float tip = cy - s * 0.75f;
            float wing = cy + s * 0.05f;
            float stemBot = cy + s * 0.75f;
            mPath.moveTo(cx - s * 0.6f, wing);
            mPath.lineTo(cx, tip);
            mPath.lineTo(cx + s * 0.6f, wing);
            mPath.moveTo(cx, wing);
            mPath.lineTo(cx, stemBot);
            canvas.drawPath(mPath, mGradientIconPaint);

            if (shiftState == SoftKeyboard.SHIFT_LOCKED) {
                mPath.reset();
                float barY = cy + s * 0.95f;
                mPath.moveTo(cx - s * 0.45f, barY);
                mPath.lineTo(cx + s * 0.45f, barY);
                canvas.drawPath(mPath, mGradientIconPaint);
            }
        } else {
            float tip = cy + s * 0.75f;
            float wing = cy - s * 0.05f;
            float stemTop = cy - s * 0.75f;
            mPath.moveTo(cx - s * 0.6f, wing);
            mPath.lineTo(cx, tip);
            mPath.lineTo(cx + s * 0.6f, wing);
            mPath.moveTo(cx, stemTop);
            mPath.lineTo(cx, wing);
            canvas.drawPath(mPath, mGradientIconPaint);
        }
    }

    private void drawBackspace(Canvas canvas, float cx, float cy, float s, float sw) {
        float bodyRight = cx + s * 0.7f;
        float bodyTop = cy - s * 0.55f;
        float bodyBot = cy + s * 0.55f;
        float notchLeft = cx - s * 0.4f;
        float pointX = cx - s * 1.1f;

        mGradientIconPaint.setStyle(Paint.Style.STROKE);
        mGradientIconPaint.setStrokeWidth(sw);

        mPath.reset();
        mPath.moveTo(notchLeft, bodyTop);
        mPath.lineTo(bodyRight, bodyTop);
        mPath.lineTo(bodyRight, bodyBot);
        mPath.lineTo(notchLeft, bodyBot);
        mPath.lineTo(pointX, cy);
        mPath.close();
        canvas.drawPath(mPath, mGradientIconPaint);

        float xOff = s * 0.22f;
        float xCx = cx + s * 0.1f;
        canvas.drawLine(xCx - xOff, cy - xOff, xCx + xOff, cy + xOff, mGradientIconPaint);
        canvas.drawLine(xCx - xOff, cy + xOff, xCx + xOff, cy - xOff, mGradientIconPaint);
    }

    private void drawReturnArrow(Canvas canvas, float cx, float cy, float s, float sw) {
        mGradientIconPaint.setStyle(Paint.Style.STROKE);
        mGradientIconPaint.setStrokeWidth(sw);

        float startX = cx + s * 0.6f;
        float topY = cy - s * 0.5f;
        float botY = cy + s * 0.35f;
        float endX = cx - s * 0.6f;

        mPath.reset();
        mPath.moveTo(startX, topY);
        mPath.lineTo(startX, botY);
        mPath.lineTo(endX, botY);
        canvas.drawPath(mPath, mGradientIconPaint);

        float arrowSize = s * 0.3f;
        mPath.reset();
        mPath.moveTo(endX + arrowSize, botY - arrowSize);
        mPath.lineTo(endX, botY);
        mPath.lineTo(endX + arrowSize, botY + arrowSize);
        canvas.drawPath(mPath, mGradientIconPaint);
    }

    private void drawCheckmark(Canvas canvas, float cx, float cy, float s, float sw) {
        mGradientIconPaint.setStyle(Paint.Style.STROKE);
        mGradientIconPaint.setStrokeWidth(sw);

        mPath.reset();
        mPath.moveTo(cx - s * 0.55f, cy);
        mPath.lineTo(cx - s * 0.1f, cy + s * 0.45f);
        mPath.lineTo(cx + s * 0.6f, cy - s * 0.45f);
        canvas.drawPath(mPath, mGradientIconPaint);
    }
}
