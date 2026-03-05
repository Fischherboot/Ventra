package com.moritzsoft.keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class SoftKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private InputMethodManager mInputMethodManager;
    private MoritzsoftKeyboardView mInputView;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private int mLastDisplayWidth;

    private MoritzsoftKeyboard mSymbolsKeyboard;
    private MoritzsoftKeyboard mSymbolsShiftedKeyboard;
    private MoritzsoftKeyboard mQwertzKeyboard;
    private MoritzsoftKeyboard mCurKeyboard;

    private String mWordSeparators;

    // Shift state: 0=off, 1=single (auto-unshift after one letter), 2=caps lock
    static final int SHIFT_OFF = 0;
    static final int SHIFT_ONCE = 1;
    static final int SHIFT_LOCKED = 2;
    private int mShiftState = SHIFT_OFF;
    private long mLastShiftTap = 0;
    private static final long DOUBLE_TAP_MS = 400;

    @Override
    public void onCreate() {
        super.onCreate();
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
    }

    @Override
    public void onInitializeInterface() {
        if (mQwertzKeyboard != null) {
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertzKeyboard = new MoritzsoftKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new MoritzsoftKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new MoritzsoftKeyboard(this, R.xml.symbols_shift);
    }

    @Override
    public View onCreateInputView() {
        mInputView = (MoritzsoftKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setPreviewEnabled(false);
        mInputView.setKeyboard(mQwertzKeyboard);
        mInputView.setService(this);
        return mInputView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        mComposing.setLength(0);
        mPredictionOn = false;

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                mCurKeyboard = mSymbolsKeyboard;
                break;
            case InputType.TYPE_CLASS_TEXT:
                mCurKeyboard = mQwertzKeyboard;
                mShiftState = SHIFT_OFF;
                updateShiftVisual();
                break;
            default:
                mCurKeyboard = mQwertzKeyboard;
                mShiftState = SHIFT_OFF;
                updateShiftVisual();
        }
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        mComposing.setLength(0);
        mCurKeyboard = mQwertzKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
        }
    }

    private void updateShiftVisual() {
        if (mInputView != null && mQwertzKeyboard == mInputView.getKeyboard()) {
            mInputView.setShifted(mShiftState != SHIFT_OFF);
            mInputView.invalidateAllKeys();
        }
    }

    public int getShiftState() {
        return mShiftState;
    }

    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(
                            String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    /** Called from KeyboardView when space-swipe moves cursor */
    public void moveCursor(int direction) {
        try {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;

            // Use ExtractedText to get current cursor position
            android.view.inputmethod.ExtractedText et = ic.getExtractedText(
                    new android.view.inputmethod.ExtractedTextRequest(), 0);
            if (et == null) return;

            int pos = et.selectionStart;
            int newPos = pos + direction;
            if (newPos < 0) newPos = 0;

            // Get total text length
            CharSequence text = et.text;
            if (text != null && newPos > text.length()) {
                newPos = text.length();
            }

            ic.setSelection(newPos, newPos);
        } catch (Exception e) {
            // Some apps may not support ExtractedText - ignore
        }
    }

    // --- OnKeyboardActionListener ---

    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL || primaryCode == -3) {
            handleClose();
        } else if (primaryCode == -100) {
            // options
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                mInputView.setKeyboard(mQwertzKeyboard);
            } else {
                mInputView.setKeyboard(mSymbolsKeyboard);
                mSymbolsKeyboard.setShifted(false);
            }
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
    }

    private void handleShift() {
        if (mInputView == null) return;

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertzKeyboard == currentKeyboard) {
            long now = System.currentTimeMillis();

            if (mShiftState == SHIFT_LOCKED) {
                // Caps locked -> turn off
                mShiftState = SHIFT_OFF;
            } else if (mShiftState == SHIFT_ONCE) {
                // Already in single-shift
                if (now - mLastShiftTap < DOUBLE_TAP_MS) {
                    // Quick double-tap -> caps lock
                    mShiftState = SHIFT_LOCKED;
                } else {
                    // Slow second tap -> turn off
                    mShiftState = SHIFT_OFF;
                }
            } else {
                // SHIFT_OFF
                if (now - mLastShiftTap < DOUBLE_TAP_MS) {
                    // Quick double-tap from off -> caps lock
                    mShiftState = SHIFT_LOCKED;
                } else {
                    // Single tap -> shift once
                    mShiftState = SHIFT_ONCE;
                }
            }
            mLastShiftTap = now;
            updateShiftVisual();
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mShiftState != SHIFT_OFF) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        getCurrentInputConnection().commitText(
                String.valueOf((char) primaryCode), 1);

        // Auto-unshift after one letter if in single-shift mode
        if (mShiftState == SHIFT_ONCE) {
            mShiftState = SHIFT_OFF;
            updateShiftVisual();
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    public boolean isWordSeparator(int code) {
        return mWordSeparators.contains(String.valueOf((char) code));
    }

    public void swipeRight() { }
    public void swipeLeft() { }
    public void swipeDown() { }
    public void swipeUp() { }

    public void onPress(int primaryCode) {
        if (mInputView != null) {
            mInputView.setPressedKeyCode(primaryCode);
            mInputView.doHaptic();
        }
    }

    public void onRelease(int primaryCode) {
        if (mInputView != null) {
            mInputView.clearPressedKey();
        }
    }

    public void pickSuggestionManually(String suggestion) {
        if (mComposing.length() > 0) {
            getCurrentInputConnection().commitText(suggestion, 1);
            mComposing.setLength(0);
        } else {
            getCurrentInputConnection().commitText(suggestion, 1);
        }
    }
}
