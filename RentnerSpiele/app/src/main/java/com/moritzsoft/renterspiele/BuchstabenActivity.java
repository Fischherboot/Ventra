package com.moritzsoft.renterspiele;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class BuchstabenActivity extends AppCompatActivity {

    // Word lists - hundreds of common German words (no duplicates)
    private static final String[] ALL_WORDS = {
        // Natur & Landschaft
        "HAUS", "BAUM", "HUND", "KATZE", "BERG", "WALD", "FISCH", "VOGEL",
        "REGEN", "SONNE", "STERN", "WOLKE", "BLUME", "WIESE", "FLUSS", "MEER",
        "INSEL", "STRAND", "STEIN", "SAND", "ERDE", "MOOS", "TEICH", "BACH",
        "BUCHT", "KLIPPE", "FELS", "TAL", "UFER", "QUELLE", "WELLE", "HALDE",
        "HEIDE", "STEPPE", "SUMPF", "GRABEN", "MULDE", "AHORN", "EFEU",

        // Essen & Trinken
        "BROT", "MILCH", "APFEL", "BIRNE", "KUCHEN", "SAHNE", "BUTTER", "HONIG",
        "ZUCKER", "SALZ", "KAFFEE", "KAKAO", "SAFT", "WASSER", "WEIN",
        "SALAT", "SUPPE", "TORTE", "NUDEL", "REIS", "MEHL", "QUARK",
        "GURKE", "BOHNE", "LINSE", "OLIVE", "MINZE", "ZIMT", "ESSIG",
        "SENF", "PFEFFER", "LAUCH", "SELLERIE", "KIRSCHE", "PFLAUME",
        "TRAUBE", "MANGO", "BANANE", "ZITRONE", "ORANGE", "ANANAS",
        "MELONE", "NUSS", "MANDEL", "ROSINE", "DATTEL", "FEIGE",
        "SCHINKEN", "SPECK", "LACHS", "FORELLE", "KREBS",

        // Moebel & Haushalt
        "TISCH", "STUHL", "LAMPE", "SOFA", "SPIEGEL", "KISSEN", "DECKE",
        "TEPPICH", "GARDINE", "REGAL", "SCHRANK", "TRUHE", "HOCKER", "LIEGE",
        "SEIFE", "BESEN", "EIMER", "KORB", "DOSE", "KANNE",
        "MESSER", "TOPF", "PFANNE", "OFEN", "HERD", "GABEL", "TASSE", "GLAS",
        "TELLER", "BECKEN", "BETT", "MATRATZE", "POLSTER", "LEHNE",

        // Kleidung & Schmuck
        "SCHUH", "KLEID", "JACKE", "HEMD", "HOSE", "RING", "KETTE", "BRILLE",
        "MANTEL", "MUETZE", "SCHAL", "STIEFEL", "SOCKE", "WESTE",
        "BLUSE", "GURT", "KAPPE", "HELM", "NADEL", "KNOPF",

        // Berufe
        "ARZT", "KOCH", "MALER", "BAUER", "PILOT", "LEHRER", "RICHTER",
        "JUNGE", "DAME", "GRAF", "RITTER", "WEBER", "WIRT",

        // Tiere
        "ADLER", "RABE", "SPATZ", "FUCHS", "HASE", "RIND", "PFERD",
        "TIGER", "ZEBRA", "PANDA", "AFFE", "ESEL", "TAUBE", "MEISE",
        "STORCH", "REIHER", "FALKE", "LUCHS", "DACHS", "OTTER", "BIBER",
        "IGEL", "DROSSEL", "KRAEHE", "ELSTER", "FINK", "AMSEL",
        "GANS", "ENTE", "SCHWAN", "HUHN", "KALB", "LAMM", "ZIEGE",
        "SPINNE", "FLIEGE", "BIENE", "WESPE", "RAUPE", "AMEISE",
        "KREUZ", "GRILLE", "HUMMEL", "MOTTE", "LAUS", "SCHNECKE",
        "WURM", "FROSCH", "MOLCH", "EIDECHSE",

        // Pflanzen
        "EICHE", "BIRKE", "TANNE", "ROSE", "TULPE", "LILIE", "NELKE",
        "KLEE", "FARN", "PALME", "BUCHE", "LINDE", "ERLE", "WEIDE",
        "KIEFER", "FICHTE", "MALVE", "ASTER", "MOHN", "VEILCHEN",
        "DISTEL", "DAHLIE",

        // Gebaeude & Orte
        "GARTEN", "KIRCHE", "SCHULE", "MARKT", "BRUECKE", "TURM",
        "MAUER", "ZAUN", "DACH", "FENSTER", "KELLER",
        "FABRIK", "HAFEN", "PLATZ", "SCHEUNE", "STALL",
        "RATHAUS", "KAPELLE", "KLOSTER", "FESTUNG", "PALAST",
        "HUETTE", "VILLA",

        // Zeit & Wetter
        "WINTER", "SOMMER", "HERBST", "MONTAG", "FREITAG",
        "NACHT", "MORGEN", "ABEND", "MITTAG", "STUNDE",
        "DONNER", "BLITZ", "HAGEL", "NEBEL", "FROST",
        "SCHNEE", "STURM", "HITZE", "KALT",

        // Musik & Kultur
        "FARBE", "LICHT", "MUSIK", "TANZ", "LIED", "CHOR",
        "KLAVIER", "GEIGE", "ORGEL", "HARFE", "GLOCKE",
        "TROMMEL", "LAUTE", "HORN", "PAUKE",

        // Schule & Wissen
        "BUCH", "STIFT", "KERZE", "VASE", "TAFEL",
        "KREIDE", "PAPIER", "SEITE", "BRIEF", "KARTE",
        "MAPPE", "FEDER", "TINTE", "BLOCK",

        // Abstrakt & Gefuehle
        "FELD", "HELD", "GELD", "ZELT", "WELT", "HALT",
        "LAND", "BAND", "HAND", "RAND", "WAND",
        "LAUF", "KAUF", "ANGEL", "NAGEL", "REGEL", "PENDEL",
        "OASE", "WAGEN", "LADEN", "FADEN", "BODEN", "NORDEN",
        "OSTEN", "WESTEN", "KRONE", "HALLE", "STELLE",
        "FALLE", "WOLLE", "ROLLE", "BLATT", "DRAHT", "KRAFT",
        "SCHAR", "SCHAF", "HAUCH", "RAUCH",
        "KAMPF", "DAMPF", "KOPF", "ZOPF",
        "MITTE", "RATTE", "SORTE",
        "PERLE", "NARBE", "ERBE", "KERBE",
        "TRAUM", "SCHAUM", "RAUM", "SAUM", "ZAUM",
        "FORM", "NORM",
        "TEIL", "SEIL", "BEIL", "HEIL", "PFEIL",
        "TIER", "BIER", "VIER", "KLASSE", "STRASSE",
        "MAGEN", "LAGEN", "SAGEN", "TRAGEN", "FRAGEN",

        // Weitere Woerter
        "WOLF", "LOEWE", "ANKER", "SEGEL", "MAST", "BUG",
        "HECK", "DOCK", "RIEGEL", "GRIFF", "SCHLOSS",
        "TASTE", "HEBEL", "BOLZEN", "SCHRAUBE",
        "GEIST", "SEELE", "SINN", "WILLE", "TRIEB", "DRANG",
        "STOLZ", "EHRE", "RUHM", "GLUECK", "FREUDE", "LUST",
        "ANGST", "SORGE", "WAHN", "GRAM", "PEIN", "LEID",
        "GUNST", "HULD", "GNADE",
        "WERT", "PREIS", "LOHN", "ZINS", "PACHT",
        "RECHT", "PFLICHT", "MACHT", "RANG",
        "GRENZE", "MARKE", "LINIE", "PUNKT", "KREIS",
        "ECKE", "KANTE", "SPITZE", "BOGEN",
        "STEG", "PFAD", "GASSE", "ALLEE",
        "RAMPE", "TREPPE", "LEITER",
        "FRACHT", "LAST", "LADUNG",
        "WAFFE", "SCHILD", "SPEER", "DOLCH", "LANZE",
        "FLAGGE", "FAHNE", "BANNER",
        "HAUBE", "DECKEL", "KASTEN",
        "SCHNUR", "KABEL", "FASER",
        "SAMT", "SEIDE", "LEINEN", "FILZ", "STOFF",
        "PUDER", "STAUB", "ASCHE", "RUHE",
        "GLANZ", "SCHEIN", "FLECK",
        "KERN", "MARK", "PULPA",
        "STOCK", "STAB", "STANGE", "BALKEN",
        "BRETT", "PLANKE", "BOHLE", "SCHEIBE",
        "KUGEL", "WALZE", "TONNE", "FASS",
        "KESSEL", "WANNE", "RINNE",
        "GIEBEL", "PFEILER",
        "SAITE", "STIMME", "KLANG", "SCHALL",
        "ECHO", "LAUT", "REIM", "VERS", "PROSA",
        "REDE", "SPRUCH",
        "KUNDE", "GAST", "MIETER", "ZEUGE",
        "BUSCH", "HECKE", "STAUDE", "HALM", "RANKE",
        "SAMEN", "SPORE", "KNOSPE",
        "LAUB", "RINDE", "STAMM", "ZWEIG",
        "PILZ", "ALGE", "FLECHTE",
        "GOLD", "SILBER", "KUPFER", "EISEN", "STAHL",
        "ZINK", "BLEI", "CHROM",
        "KOHLE", "KALK", "LEHM",
        "BASALT", "GRANIT", "MARMOR",
        "RUBIN", "DIAMANT", "JADE", "OPAL",
        "FUNKE", "FLAMME", "GLUT", "BRAND",
        "QUALM", "DUNST", "DUFT",
        "ATEM", "PUSTE",
        "SCHWEISS", "BLUT", "KNOCHEN", "MUSKEL",
        "NERV", "ADER", "ORGAN",
        "NABEL", "KINN", "WANGE", "STIRN",
        "BRAUE", "WIMPER", "LOCKE", "BART",
        "DAUMEN", "FAUST", "GELENK",
        "KNOTEN", "MASCHE", "NAHT",
        "SOHLE", "ABSATZ", "LASCHE",
        "TASCHE", "BEUTEL", "SACK",
        "GURTE", "RIEMEN",
        "REISE", "FLUCHT", "FAHRT",
        "MARSCH", "WANDEL", "RITT",
        "SPRUNG", "STURZ", "FLUG",
        "JAGD", "FANG", "BEUTE",
        "ERNTE", "SAAT", "FRUCHT",
        "BRUT", "NEST", "HORST",
        "HERDE", "RUDEL", "SCHWARM",
        "MEUTE", "TRUPP", "BANDE",
        "FEST", "FEIER", "JUBEL",
        "SPIEL", "SCHERZ", "WITZ",
        "RAUSCH", "FIEBER", "SCHMERZ",
    };

    // Button colors matching MenschActivity style
    private static final int COLOR_BG         = 0xFFA6A6A6; // #A6A6A6 grey background
    private static final int COLOR_BTN_BACK   = 0xFF787878; // darker grey for Zurueck
    private static final int COLOR_BTN_NEW    = 0xFF43A047; // green for Neues Spiel
    private static final int COLOR_BTN_HINT   = 0xFFEF6C00; // orange for Tipp
    private static final int COLOR_BTN_CLEAR  = 0xFFD32F2F; // red for Entfernen
    private static final int COLOR_BTN_LETTER = 0xFF8A8A8A; // letter buttons grey
    private static final int BTN_TEXT_COLOR   = 0xFFFFFFFF;

    // Grid: 0=black(invalid), >0 = number representing a letter
    private int gridRows, gridCols;
    private int[][] grid; // 0=black, positive=number-code
    private Map<Character, Integer> letterToNumber = new HashMap<>();
    private Map<Integer, Character> numberToLetter = new HashMap<>();
    private Map<Integer, Character> playerGuess = new HashMap<>(); // player's current assignment
    private Set<Integer> revealedNumbers = new HashSet<>(); // pre-given hints
    private int selectedNumber = -1;
    private List<String> placedWords = new ArrayList<>();
    private int difficulty = 0;

    private CrosswordView crosswordView;
    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();

        // Force #A6A6A6 on window background BEFORE setContentView
        getWindow().setBackgroundDrawable(new ColorDrawable(COLOR_BG));
        getWindow().getDecorView().setBackgroundColor(COLOR_BG);

        setContentView(R.layout.activity_buchstaben);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Force #A6A6A6 on EVERY possible parent view
        forceBackground(findViewById(android.R.id.content));
        forceBackground(findViewById(R.id.difficultyScreen));
        forceBackground(findViewById(R.id.gameScreen));
        // Walk the entire view tree and set background on all layouts
        forceBackgroundRecursive(getWindow().getDecorView());

        // Hide the "Zahl antippen" text completely
        TextView txtSelectedInfo = findViewById(R.id.txtSelectedNumber);
        if (txtSelectedInfo != null) {
            txtSelectedInfo.setVisibility(View.GONE);
            txtSelectedInfo.setText("");
            txtSelectedInfo.setHeight(0);
        }

        // --- Difficulty screen buttons (BIG, colored) ---
        Button btnEasy = findViewById(R.id.btnEasy);
        Button btnMedium = findViewById(R.id.btnMedium);
        Button btnHard = findViewById(R.id.btnHard);
        Button btnBackDiff = findViewById(R.id.btnBackDiff);

        styleButton(btnEasy, COLOR_BTN_NEW, BTN_TEXT_COLOR, 24);
        styleButton(btnMedium, COLOR_BTN_HINT, BTN_TEXT_COLOR, 24);
        styleButton(btnHard, COLOR_BTN_CLEAR, BTN_TEXT_COLOR, 24);
        styleButton(btnBackDiff, COLOR_BTN_BACK, BTN_TEXT_COLOR, 24);
        setBtnMinHeight(btnEasy, 64);
        setBtnMinHeight(btnMedium, 64);
        setBtnMinHeight(btnHard, 64);
        setBtnMinHeight(btnBackDiff, 64);

        btnEasy.setOnClickListener(v -> { vibrate(30); difficulty = 0; startGame(); });
        btnMedium.setOnClickListener(v -> { vibrate(30); difficulty = 1; startGame(); });
        btnHard.setOnClickListener(v -> { vibrate(30); difficulty = 2; startGame(); });
        btnBackDiff.setOnClickListener(v -> { vibrate(30); finish(); });

        // --- Game screen buttons (BIG, colored like Mensch aerger dich nicht) ---
        Button btnBack = findViewById(R.id.btnBack);
        Button btnNewGame = findViewById(R.id.btnNewGame);
        Button btnHint = findViewById(R.id.btnHint);
        Button btnClearLetter = findViewById(R.id.btnClearLetter);

        // Set text labels (no emojis!) - single line, elongated buttons
        btnBack.setText("ZURÜCK");
        btnClearLetter.setText("ENTFERNEN");
        btnHint.setText("TIPP");
        btnNewGame.setText("NEUE RUNDE");

        styleButton(btnBack, COLOR_BTN_BACK, BTN_TEXT_COLOR, 17);
        styleButton(btnNewGame, COLOR_BTN_NEW, BTN_TEXT_COLOR, 17);
        styleButton(btnHint, COLOR_BTN_HINT, BTN_TEXT_COLOR, 17);
        styleButton(btnClearLetter, COLOR_BTN_CLEAR, BTN_TEXT_COLOR, 17);

        // All buttons single line, elongated (not square)
        btnBack.setSingleLine(true);
        btnClearLetter.setSingleLine(true);
        btnHint.setSingleLine(true);
        btnNewGame.setSingleLine(true);
        btnBack.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        btnClearLetter.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        btnHint.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        btnNewGame.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        btnNewGame.setGravity(Gravity.CENTER);

        // --- Restructure game screen: puzzle left, buttons right, letters below ---
        restructureGameLayout(btnBack, btnNewGame, btnHint, btnClearLetter);

        btnBack.setOnClickListener(v -> { vibrate(30); finish(); });
        btnNewGame.setOnClickListener(v -> { vibrate(30); startGame(); });
        btnHint.setOnClickListener(v -> { vibrate(30); giveHint(); });
        btnClearLetter.setOnClickListener(v -> {
            vibrate(30);
            if (selectedNumber > 0 && !revealedNumbers.contains(selectedNumber)) {
                playerGuess.remove(selectedNumber);
                updateAll();
            }
        });
    }

    /** Recursively force #A6A6A6 background on all layout containers */
    private void forceBackgroundRecursive(View v) {
        if (v == null) return;
        // Set bg on layout containers, not on buttons/textviews/custom views
        if (v instanceof ViewGroup) {
            // Only set on generic layouts, not on specific widgets
            if (v instanceof LinearLayout || v instanceof FrameLayout || v instanceof ScrollView) {
                v.setBackgroundColor(COLOR_BG);
            }
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                forceBackgroundRecursive(vg.getChildAt(i));
            }
        }
    }

    private void forceBackground(View v) {
        if (v != null) v.setBackgroundColor(COLOR_BG);
    }

    /** Apply colored rounded button style - BIG text, BIG padding */
    private void styleButton(Button btn, int bgColor, int textColor, int textSizeSp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dpToPx(10));
        btn.setBackground(bg);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setAllCaps(false);
        btn.setStateListAnimator(null);
        btn.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
    }

    /** Set minimum height in dp for bigger senior-friendly buttons */
    private void setBtnMinHeight(Button btn, int heightDp) {
        btn.setMinHeight(dpToPx(heightDp));
        btn.setMinimumHeight(dpToPx(heightDp));
    }

    /**
     * Programmatically restructure the game screen:
     * Top area = horizontal: [puzzleContainer (expanded)] + [vertical button strip on right]
     * Bottom = letterGrid
     */
    private void restructureGameLayout(Button btnBack, Button btnNewGame, Button btnHint, Button btnClearLetter) {
        // Remove buttons from their current parent
        ViewGroup backParent = (ViewGroup) btnBack.getParent();
        if (backParent != null) backParent.removeView(btnBack);
        ViewGroup newParent = (ViewGroup) btnNewGame.getParent();
        if (newParent != null) newParent.removeView(btnNewGame);
        ViewGroup hintParent = (ViewGroup) btnHint.getParent();
        if (hintParent != null) hintParent.removeView(btnHint);
        ViewGroup clearParent = (ViewGroup) btnClearLetter.getParent();
        if (clearParent != null) clearParent.removeView(btnClearLetter);

        // Get the gameScreen and its key children
        LinearLayout gameScreen = (LinearLayout) findViewById(R.id.gameScreen);
        FrameLayout puzzleContainer = findViewById(R.id.puzzleContainer);
        GridLayout letterGrid = findViewById(R.id.letterGrid);

        // Remove puzzle and letter grid from current parents
        ViewGroup puzzleParent = (ViewGroup) puzzleContainer.getParent();
        if (puzzleParent != null) puzzleParent.removeView(puzzleContainer);
        ViewGroup letterParent = (ViewGroup) letterGrid.getParent();
        if (letterParent != null) letterParent.removeView(letterGrid);

        // Also remove any empty parent containers left behind (like the old button row)
        // Clear the gameScreen completely
        gameScreen.removeAllViews();

        // Create horizontal layout: puzzle (left, weight=1) + buttons (right, vertical strip)
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setBackgroundColor(COLOR_BG);
        LinearLayout.LayoutParams topRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        topRow.setLayoutParams(topRowParams);

        // Puzzle container takes remaining space
        LinearLayout.LayoutParams puzzleParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        puzzleContainer.setLayoutParams(puzzleParams);
        topRow.addView(puzzleContainer);

        // Vertical button strip on the right side - wide enough for senior-friendly buttons
        LinearLayout buttonStrip = new LinearLayout(this);
        buttonStrip.setOrientation(LinearLayout.VERTICAL);
        buttonStrip.setGravity(Gravity.CENTER_VERTICAL);
        buttonStrip.setBackgroundColor(COLOR_BG);
        int stripWidth = dpToPx(190);
        LinearLayout.LayoutParams stripParams = new LinearLayout.LayoutParams(
                stripWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        stripParams.setMargins(dpToPx(2), 0, dpToPx(4), 0);
        buttonStrip.setLayoutParams(stripParams);

        // Add buttons vertically - BIG, well-spaced for seniors
        int btnMarginH = dpToPx(6);
        int btnMarginV = dpToPx(10);
        Button[] buttons = {btnBack, btnHint, btnClearLetter, btnNewGame};
        for (Button btn : buttons) {
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(btnMarginH, btnMarginV, btnMarginH, btnMarginV);
            btn.setLayoutParams(btnParams);
            btn.setMinHeight(dpToPx(64));
            btn.setMinimumHeight(dpToPx(64));
            buttonStrip.addView(btn);
        }

        topRow.addView(buttonStrip);

        // Add top row (puzzle + buttons) and letter grid below
        gameScreen.addView(topRow);

        // Letter grid at bottom
        LinearLayout.LayoutParams letterParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        letterParams.setMargins(0, dpToPx(4), 0, 0);
        letterGrid.setLayoutParams(letterParams);
        gameScreen.addView(letterGrid);
    }

    private void startGame() {
        findViewById(R.id.difficultyScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);

        // Force bg again after screen switch
        forceBackground(findViewById(R.id.gameScreen));

        // Keep info text hidden
        TextView txtSel = findViewById(R.id.txtSelectedNumber);
        if (txtSel != null) {
            txtSel.setVisibility(View.GONE);
            txtSel.setText("");
        }

        int wordCount;
        switch (difficulty) {
            case 0: gridRows = 8; gridCols = 12; wordCount = 12; break;
            case 1: gridRows = 10; gridCols = 15; wordCount = 18; break;
            default: gridRows = 10; gridCols = 17; wordCount = 24; break;
        }

        letterToNumber.clear();
        numberToLetter.clear();
        playerGuess.clear();
        revealedNumbers.clear();
        selectedNumber = -1;
        placedWords.clear();

        generateCrossword(wordCount);
        revealHints();
        buildLetterButtons();

        FrameLayout container = findViewById(R.id.puzzleContainer);
        container.removeAllViews();
        crosswordView = new CrosswordView(this);
        container.addView(crosswordView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        updateAll();
    }

    private void generateCrossword(int targetWords) {
        grid = new int[gridRows][gridCols];
        placedWords.clear();

        letterToNumber.clear();
        numberToLetter.clear();
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 26; i++) numbers.add(i);
        Collections.shuffle(numbers, random);
        for (int i = 0; i < 26; i++) {
            char letter = (char) ('A' + i);
            int num = numbers.get(i);
            letterToNumber.put(letter, num);
            numberToLetter.put(num, letter);
        }

        Set<String> seen = new HashSet<>();
        List<String> words = new ArrayList<>();
        for (String w : ALL_WORDS) {
            String upper = w.toUpperCase();
            if (upper.length() >= 3
                    && upper.length() <= Math.max(gridRows, gridCols) - 1
                    && !seen.contains(upper)) {
                boolean valid = true;
                for (char c : upper.toCharArray()) {
                    if (c < 'A' || c > 'Z') { valid = false; break; }
                }
                if (valid) {
                    seen.add(upper);
                    words.add(upper);
                }
            }
        }
        Collections.shuffle(words, random);

        int placed = 0;
        int maxAttempts = targetWords + 30;
        for (String word : words) {
            if (placed >= maxAttempts) break;

            if (placed == 0) {
                int row = gridRows / 2;
                int col = (gridCols - word.length()) / 2;
                if (col >= 0 && col + word.length() <= gridCols) {
                    for (int i = 0; i < word.length(); i++) {
                        grid[row][col + i] = letterToNumber.get(word.charAt(i));
                    }
                    placedWords.add(word);
                    placed++;
                }
            } else {
                if (tryPlaceWord(word)) placed++;
            }
        }
    }

    private boolean tryPlaceWord(String word) {
        List<int[]> options = new ArrayList<>();

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            int num = letterToNumber.get(ch);

            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    if (grid[r][c] != num) continue;

                    int startR = r - i;
                    if (canPlaceWord(word, startR, c, 1, 0, r, c)) {
                        options.add(new int[]{startR, c, 1, 0});
                    }
                    int startC = c - i;
                    if (canPlaceWord(word, r, startC, 0, 1, r, c)) {
                        options.add(new int[]{r, startC, 0, 1});
                    }
                }
            }
        }

        if (options.isEmpty()) return false;
        int[] chosen = options.get(random.nextInt(options.size()));
        for (int i = 0; i < word.length(); i++) {
            int r = chosen[0] + i * chosen[2];
            int c = chosen[1] + i * chosen[3];
            grid[r][c] = letterToNumber.get(word.charAt(i));
        }
        placedWords.add(word);
        return true;
    }

    private boolean canPlaceWord(String word, int startR, int startC, int dR, int dC, int crossR, int crossC) {
        if (startR < 0 || startC < 0) return false;
        int endR = startR + (word.length() - 1) * dR;
        int endC = startC + (word.length() - 1) * dC;
        if (endR >= gridRows || endC >= gridCols) return false;

        int beforeR = startR - dR, beforeC = startC - dC;
        if (beforeR >= 0 && beforeR < gridRows && beforeC >= 0 && beforeC < gridCols && grid[beforeR][beforeC] != 0)
            return false;
        int afterR = endR + dR, afterC = endC + dC;
        if (afterR >= 0 && afterR < gridRows && afterC >= 0 && afterC < gridCols && grid[afterR][afterC] != 0)
            return false;

        boolean hasIntersection = false;
        for (int i = 0; i < word.length(); i++) {
            int r = startR + i * dR;
            int c = startC + i * dC;
            int num = letterToNumber.get(word.charAt(i));

            if (grid[r][c] == num) {
                hasIntersection = true;
                continue;
            }
            if (grid[r][c] != 0) return false;

            if (dR == 0) {
                if (r > 0 && grid[r - 1][c] != 0 && !(r - 1 == crossR && c == crossC)) return false;
                if (r < gridRows - 1 && grid[r + 1][c] != 0 && !(r + 1 == crossR && c == crossC)) return false;
            } else {
                if (c > 0 && grid[r][c - 1] != 0 && !(r == crossR && c - 1 == crossC)) return false;
                if (c < gridCols - 1 && grid[r][c + 1] != 0 && !(r == crossR && c + 1 == crossC)) return false;
            }
        }
        return hasIntersection;
    }

    private void revealHints() {
        List<Integer> usedNumbers = new ArrayList<>();
        for (int r = 0; r < gridRows; r++)
            for (int c = 0; c < gridCols; c++)
                if (grid[r][c] > 0 && !usedNumbers.contains(grid[r][c]))
                    usedNumbers.add(grid[r][c]);

        Collections.shuffle(usedNumbers, random);
        int toReveal = Math.min(4, usedNumbers.size());
        for (int i = 0; i < toReveal; i++) {
            int num = usedNumbers.get(i);
            revealedNumbers.add(num);
            playerGuess.put(num, numberToLetter.get(num));
        }
    }

    private void buildLetterButtons() {
        GridLayout letterGrid = findViewById(R.id.letterGrid);
        letterGrid.removeAllViews();
        letterGrid.setBackgroundColor(COLOR_BG);

        int columns = 7;
        letterGrid.setColumnCount(columns);

        // 56dp height = BIG senior-friendly letter buttons
        int btnHeight = dpToPx(56);
        int margin = dpToPx(3);

        for (int i = 0; i < 26; i++) {
            final char letter = (char) ('A' + i);
            Button btn = new Button(this);
            btn.setText(String.valueOf(letter));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            btn.setTextColor(BTN_TEXT_COLOR);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(0, 0, 0, 0);
            btn.setMinHeight(0);
            btn.setMinimumHeight(0);
            btn.setStateListAnimator(null);
            btn.setAllCaps(false);

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(COLOR_BTN_LETTER);
            btnBg.setCornerRadius(dpToPx(8));
            btn.setBackground(btnBg);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = GridLayout.spec(i % columns, 1f);
            params.rowSpec = GridLayout.spec(i / columns);
            params.width = 0;
            params.height = btnHeight;
            params.setMargins(margin, margin, margin, margin);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                vibrate(30);
                if (selectedNumber > 0 && !revealedNumbers.contains(selectedNumber)) {
                    playerGuess.put(selectedNumber, letter);
                    updateAll();
                    checkWin();
                }
            });
            letterGrid.addView(btn);
        }
    }

    private void updateAll() {
        if (crosswordView != null) crosswordView.invalidate();
    }

    private void giveHint() {
        List<Integer> usedNumbers = new ArrayList<>();
        for (int r = 0; r < gridRows; r++)
            for (int c = 0; c < gridCols; c++)
                if (grid[r][c] > 0) {
                    int num = grid[r][c];
                    if (!revealedNumbers.contains(num) && !usedNumbers.contains(num)) {
                        Character guess = playerGuess.get(num);
                        if (guess == null || guess != numberToLetter.get(num)) {
                            usedNumbers.add(num);
                        }
                    }
                }

        if (!usedNumbers.isEmpty()) {
            int num = usedNumbers.get(random.nextInt(usedNumbers.size()));
            revealedNumbers.add(num);
            playerGuess.put(num, numberToLetter.get(num));
            updateAll();
            checkWin();
        }
    }

    private void checkWin() {
        Set<Integer> usedNumbers = new HashSet<>();
        for (int r = 0; r < gridRows; r++)
            for (int c = 0; c < gridCols; c++)
                if (grid[r][c] > 0) usedNumbers.add(grid[r][c]);

        for (int num : usedNumbers) {
            Character guess = playerGuess.get(num);
            if (guess == null || guess != numberToLetter.get(num)) return;
        }
        vibrate(500);
        Toast.makeText(this, getString(R.string.game_won), Toast.LENGTH_LONG).show();
        handler.postDelayed(this::finish, 2500);
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

    // ========== Crossword View ==========
    private class CrosswordView extends View {
        private Paint blackPaint, whitePaint, selectedPaint, givenPaint, sameNumPaint;
        private Paint numberPaint, letterPaint, givenLetterPaint, borderPaint;
        private float cellSize, offsetX, offsetY;

        public CrosswordView(Context ctx) {
            super(ctx);
            blackPaint = mp(0xFF333333);
            whitePaint = mp(0xFFFFFFFF);
            selectedPaint = mp(0xFFFFEB3B);
            givenPaint = mp(0xFFE8F5E9);
            sameNumPaint = mp(0xFFE3F2FD);
            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(0xFFAAAAAA);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1.5f);
            numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            numberPaint.setColor(0xFF888888);
            numberPaint.setTextAlign(Paint.Align.LEFT);
            letterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            letterPaint.setColor(0xFF1565C0);
            letterPaint.setTextAlign(Paint.Align.CENTER);
            letterPaint.setTypeface(Typeface.DEFAULT_BOLD);
            givenLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            givenLetterPaint.setColor(0xFF1A1A1A);
            givenLetterPaint.setTextAlign(Paint.Align.CENTER);
            givenLetterPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        private Paint mp(int color) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color); p.setStyle(Paint.Style.FILL); return p;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;

            float maxCellW = (float) w / gridCols;
            float maxCellH = (float) h / gridRows;
            cellSize = Math.min(maxCellW, maxCellH);
            offsetX = (w - cellSize * gridCols) / 2f;
            offsetY = (h - cellSize * gridRows) / 2f;

            numberPaint.setTextSize(cellSize * 0.25f);
            letterPaint.setTextSize(cellSize * 0.55f);
            givenLetterPaint.setTextSize(cellSize * 0.55f);

            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    float left = offsetX + c * cellSize;
                    float top = offsetY + r * cellSize;
                    RectF rect = new RectF(left, top, left + cellSize, top + cellSize);

                    if (grid[r][c] == 0) {
                        canvas.drawRect(rect, blackPaint);
                    } else {
                        int num = grid[r][c];
                        boolean isGiven = revealedNumbers.contains(num);
                        boolean isSelected = (num == selectedNumber);

                        if (isSelected) {
                            canvas.drawRect(rect, selectedPaint);
                        } else if (isGiven) {
                            canvas.drawRect(rect, givenPaint);
                        } else {
                            canvas.drawRect(rect, whitePaint);
                        }
                        canvas.drawRect(rect, borderPaint);

                        canvas.drawText(String.valueOf(num), left + 3, top + numberPaint.getTextSize() + 2, numberPaint);

                        Character guess = playerGuess.get(num);
                        if (guess != null) {
                            Paint lp = isGiven ? givenLetterPaint : letterPaint;
                            float cx = left + cellSize / 2f;
                            float cy = top + cellSize * 0.62f - (lp.descent() + lp.ascent()) / 2f;
                            canvas.drawText(String.valueOf(guess), cx, cy, lp);
                        }
                    }
                }
            }
            Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
            cp.setColor(0x88888888); cp.setTextSize(12f); cp.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Moritzsoft", w - 8, h - 4, cp);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();
                int col = (int) ((x - offsetX) / cellSize);
                int row = (int) ((y - offsetY) / cellSize);
                if (row >= 0 && row < gridRows && col >= 0 && col < gridCols && grid[row][col] > 0) {
                    selectedNumber = grid[row][col];
                    vibrate(20);
                    updateAll();
                }
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
