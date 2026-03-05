package com.moritzsoft.ventra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // ── Zustand ──────────────────────────────────────────
    private var currentState = 0
    private var userName = ""

    // ── UI-Elemente: Normales Layout ─────────────────────
    private lateinit var normalLayout: LinearLayout
    private lateinit var sophieImageNormal: ImageView
    private lateinit var speechBubbleNormal: LinearLayout
    private lateinit var speechTextNormal: TextView
    private lateinit var inputRow: LinearLayout
    private lateinit var inputField: EditText
    private lateinit var fertigButton: Button
    private lateinit var weiterButtonNormal: Button

    // ── UI-Elemente: Ecken-Layout ────────────────────────
    private lateinit var cornerLayout: FrameLayout
    private lateinit var sophieImageCorner: ImageView
    private lateinit var speechTextCorner: TextView
    private lateinit var weiterButtonCorner: Button

    // ── System ───────────────────────────────────────────
    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val musicManager = MusicManager()

    // ── Erkennungs-Flags ─────────────────────────────────
    private var screenWasOff = false
    private var previousVolume = 0
    private var waitingForVolumeUp = false
    private var waitingForVolumeDown = false

    // ── Broadcast-Empfänger ──────────────────────────────
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (currentState == 3) {
                        screenWasOff = true
                    }
                }
            }
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_CONNECTED && currentState == 5) {
                handler.post { showChargingSuccess() }
            }
        }
    }

    // ── Lautstärke-Beobachter ────────────────────────────
    private val volumeObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVolume > previousVolume && waitingForVolumeUp) {
                waitingForVolumeUp = false
                previousVolume = currentVolume
                handler.post { showVolumeUpSuccess() }
            } else if (currentVolume < previousVolume && waitingForVolumeDown) {
                waitingForVolumeDown = false
                previousVolume = currentVolume
                handler.post { showVolumeDownSuccess() }
            }
            previousVolume = currentVolume
        }
    }

    // ═════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        bindViews()
        registerReceivers()

        showState(0)
    }

    override fun onResume() {
        super.onResume()
        if (currentState == 3 && screenWasOff) {
            screenWasOff = false
            showPowerSuccess()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicManager.release()
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(powerReceiver) } catch (_: Exception) {}
        try {
            contentResolver.unregisterContentObserver(volumeObserver)
        } catch (_: Exception) {}
    }

    // ═════════════════════════════════════════════════════
    //  INITIALISIERUNG
    // ═════════════════════════════════════════════════════

    private fun bindViews() {
        normalLayout = findViewById(R.id.normalLayout)
        sophieImageNormal = findViewById(R.id.sophieImageNormal)
        speechBubbleNormal = findViewById(R.id.speechBubbleNormal)
        speechTextNormal = findViewById(R.id.speechTextNormal)
        inputRow = findViewById(R.id.inputRow)
        inputField = findViewById(R.id.inputField)
        fertigButton = findViewById(R.id.fertigButton)
        weiterButtonNormal = findViewById(R.id.weiterButtonNormal)

        cornerLayout = findViewById(R.id.cornerLayout)
        sophieImageCorner = findViewById(R.id.sophieImageCorner)
        speechTextCorner = findViewById(R.id.speechTextCorner)
        weiterButtonCorner = findViewById(R.id.weiterButtonCorner)

        weiterButtonNormal.setOnClickListener { onWeiterClicked() }
        weiterButtonCorner.setOnClickListener { onWeiterClicked() }

        // Tastatur NICHT im Vollbild öffnen (wichtig für Landscape auf Tablets)
        inputField.imeOptions = EditorInfo.IME_ACTION_DONE or
            EditorInfo.IME_FLAG_NO_EXTRACT_UI or
            EditorInfo.IME_FLAG_NO_FULLSCREEN

        fertigButton.setOnClickListener { onFertigClicked() }

        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onFertigClicked()
                true
            } else {
                false
            }
        }
    }

    private fun registerReceivers() {
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        val powerFilter = IntentFilter(Intent.ACTION_POWER_CONNECTED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED)
            registerReceiver(powerReceiver, powerFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, screenFilter)
            registerReceiver(powerReceiver, powerFilter)
        }

        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )
    }

    // ═════════════════════════════════════════════════════
    //  ZUSTANDS-STEUERUNG
    // ═════════════════════════════════════════════════════

    private fun showState(state: Int) {
        currentState = state
        weiterButtonNormal.setOnClickListener { onWeiterClicked() }
        weiterButtonCorner.setOnClickListener { onWeiterClicked() }

        when (state) {
            0 -> showWelcome()
            1 -> showNameInput()
            2 -> showKeyboardPractice()
            3 -> showPowerButton()
            4 -> showVolume()
            5 -> showCharging()
            6 -> showFinish()
        }
    }

    private fun onWeiterClicked() {
        when (currentState) {
            0 -> showState(1)
            1 -> {}
            2 -> {}
            else -> showState(currentState + 1)
        }
    }

    private fun onFertigClicked() {
        hideKeyboard()

        when (currentState) {
            1 -> {
                val name = inputField.text.toString().trim()
                if (name.isNotEmpty()) {
                    userName = name
                    showNameSuccess()
                }
            }
            2 -> {
                val text = inputField.text.toString().trim()
                if (text == "Super!") {
                    showKeyboardSuccess()
                } else {
                    showKeyboardError()
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════
    //  LAYOUT-HELFER
    // ═════════════════════════════════════════════════════

    private fun useNormalLayout() {
        normalLayout.visibility = View.VISIBLE
        cornerLayout.visibility = View.GONE
    }

    private fun useCornerLayout() {
        normalLayout.visibility = View.GONE
        cornerLayout.visibility = View.VISIBLE
    }

    private fun setSophieNormal(resId: Int) {
        sophieImageNormal.setImageResource(resId)
    }

    private fun setSophieCorner(resId: Int) {
        sophieImageCorner.setImageResource(resId)
    }

    private fun setSpeechNormal(text: String) {
        speechTextNormal.text = text
        animateBubble(speechBubbleNormal)
    }

    private fun setSpeechCorner(text: String) {
        speechTextCorner.text = text
        animateBubble(findViewById(R.id.speechBubbleCorner))
    }

    private fun showInput(hint: String) {
        inputRow.visibility = View.VISIBLE
        inputField.hint = hint
        inputField.text.clear()
        inputField.requestFocus()
    }

    private fun hideInput() {
        inputRow.visibility = View.GONE
        inputField.text.clear()
    }

    private fun showButtonNormal(show: Boolean) {
        weiterButtonNormal.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    private fun showButtonCorner(show: Boolean) {
        weiterButtonCorner.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    private fun animateBubble(view: View) {
        val anim = AlphaAnimation(0f, 1f).apply {
            duration = 400
            fillAfter = true
        }
        view.startAnimation(anim)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(inputField.windowToken, 0)
    }

    // ═════════════════════════════════════════════════════
    //  STATE 0 – Begrüßung
    // ═════════════════════════════════════════════════════

    private fun showWelcome() {
        useNormalLayout()
        setSophieNormal(R.drawable.sophie_wave)
        hideInput()
        showButtonNormal(true)

        setSpeechNormal(
            "Hallo! Sch\u00f6n, dass du da bist!\n" +
            "Ich bin Sophie.\n\n" +
            "Ich helfe dir dabei, dein Tablet\n" +
            "kennenzulernen. Keine Sorge,\n" +
            "wir machen das ganz in Ruhe.\n\n" +
            "Dr\u00fccke unten auf den\n" +
            "schwarzen Knopf \u201EWeiter\u201C."
        )
    }

    // ═════════════════════════════════════════════════════
    //  STATE 1 – Name eingeben
    // ═════════════════════════════════════════════════════

    private fun showNameInput() {
        useNormalLayout()
        setSophieNormal(R.drawable.sophie_point_right)
        showButtonNormal(false)

        setSpeechNormal(
            "Zuerst m\u00f6chte ich gerne wissen,\n" +
            "wie du hei\u00dft!\n\n" +
            "Tippe unten in das wei\u00dfe Feld.\n" +
            "Dann \u00f6ffnet sich eine Tastatur\n" +
            "und du kannst deinen Namen\n" +
            "eintippen.\n\n" +
            "Wenn du fertig bist, dr\u00fccke\n" +
            "auf den gro\u00dfen schwarzen\n" +
            "Knopf \u201EFertig\u201C daneben."
        )

        showInput("Deinen Namen eingeben\u2026")
    }

    private fun showNameSuccess() {
        hideInput()
        showButtonNormal(true)

        weiterButtonNormal.setOnClickListener { showState(2) }

        setSpeechNormal(
            "Oh, $userName \u2013 was f\u00fcr ein\n" +
            "sch\u00f6ner Name!\n\n" +
            "Freut mich sehr, $userName.\n" +
            "Das hast du prima gemacht!\n\n" +
            "Dr\u00fccke auf \u201EWeiter\u201C."
        )
    }

    // ═════════════════════════════════════════════════════
    //  STATE 2 – Tastatur-Übung
    // ═════════════════════════════════════════════════════

    private fun showKeyboardPractice() {
        useNormalLayout()
        setSophieNormal(R.drawable.sophie_thumbs_up)
        showButtonNormal(false)

        setSpeechNormal(
            "Wunderbar, $userName!\n\n" +
            "Jetzt \u00fcben wir noch einmal\n" +
            "die Tastatur. Schreibe bitte\n" +
            "genau dieses Wort:\n\n" +
            "Super!\n\n" +
            "Tippe es unten ins wei\u00dfe Feld\n" +
            "und dr\u00fccke dann auf \u201EFertig\u201C."
        )

        showInput("Wort eingeben\u2026")
    }

    private fun showKeyboardError() {
        setSpeechNormal(
            "Hmm, das war leider noch\n" +
            "nicht ganz richtig.\n\n" +
            "Kein Problem! Schreibe bitte\n" +
            "nochmal genau:\n\n" +
            "Super!\n\n" +
            "Dann dr\u00fccke auf \u201EFertig\u201C."
        )
        inputField.text.clear()
        inputField.requestFocus()
    }

    private fun showKeyboardSuccess() {
        hideInput()
        showButtonNormal(true)

        weiterButtonNormal.setOnClickListener { showState(3) }

        setSpeechNormal(
            "Jaa, genau! Super!\n\n" +
            "Siehst du, $userName,\n" +
            "du kannst das!\n\n" +
            "Die Tastatur ist gar nicht\n" +
            "so schwer, oder?\n\n" +
            "Dr\u00fccke auf \u201EWeiter\u201C."
        )
    }

    // ═════════════════════════════════════════════════════
    //  STATE 3 – Ein-/Aus-Knopf
    // ═════════════════════════════════════════════════════

    private fun showPowerButton() {
        useCornerLayout()
        setSophieCorner(R.drawable.sophie_point_corner)
        showButtonCorner(false)
        screenWasOff = false

        setSpeechCorner(
            "Jetzt zeige ich dir etwas\n" +
            "Wichtiges, $userName!\n\n" +
            "Schau mal hier oben.\n" +
            "Da sind drei Kn\u00f6pfe.\n\n" +
            "Der gr\u00fcne Knopf ist der\n" +
            "Ein- und Aus-Knopf.\n\n" +
            "Dr\u00fccke ihn einmal kurz!\n" +
            "Dr\u00fccke ihn nach ein paar Sekunden nochmal kurz!\n"
        )
    }

    private fun showPowerSuccess() {
        useCornerLayout()
        setSophieCorner(R.drawable.sophie_thumbs_up)
        showButtonCorner(true)

        weiterButtonCorner.setOnClickListener { showState(4) }

        setSpeechCorner(
            "Perfekt, $userName!\n\n" +
            "Siehst du? So einfach geht das.\n" +
            "Jetzt wei\u00dft du, wie du den\n" +
            "Bildschirm aus- und wieder\n" +
            "einschalten kannst.\n\n" +
            "Dr\u00fccke auf \u201EWeiter\u201C."
        )
    }

    // ═════════════════════════════════════════════════════
    //  STATE 4 – Lautstärke
    // ═════════════════════════════════════════════════════

    private fun showVolume() {
        useCornerLayout()
        setSophieCorner(R.drawable.sophie_point_corner)
        showButtonCorner(false)
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        setSpeechCorner(
            "Hier oben ist auch eine Wippe,\n" +
            "$userName.\n\n" +
            "Damit kannst du den Ton\n" +
            "lauter und leiser machen.\n\n" +
            "Gleich spiele ich dir eine\n" +
            "kleine Melodie.\n\n" +
            "Dr\u00fccke die Wippe einmal\n" +
            "nach rechts \u2013 dann wird\n" +
            "es lauter!"
        )

        musicManager.startWithFadeIn()
        waitingForVolumeUp = true
    }

    private fun showVolumeUpSuccess() {
        setSpeechCorner(
            "Sehr gut, $userName!\n\n" +
            "H\u00f6rst du die Melodie?\n\n" +
            "Jetzt dr\u00fccke die Wippe\n" +
            "einmal nach links.\n" +
            "Dann wird es wieder leiser."
        )
        waitingForVolumeDown = true
    }

    private fun showVolumeDownSuccess() {
        musicManager.stopWithFadeOut {
            handler.post { showVolumeComplete() }
        }
    }

    private fun showVolumeComplete() {
        setSophieCorner(R.drawable.sophie_thumbs_up)
        showButtonCorner(true)

        weiterButtonCorner.setOnClickListener { showState(5) }

        setSpeechCorner(
            "Wunderbar, $userName!\n\n" +
            "Du hast gelernt, wie man\n" +
            "den Ton ver\u00e4ndert.\n\n" +
            "Das war doch gar nicht\n" +
            "so schwer, oder?\n\n" +
            "Dr\u00fccke auf \u201EWeiter\u201C."
        )
    }

    // ═════════════════════════════════════════════════════
    //  STATE 5 – Ladestation
    // ═════════════════════════════════════════════════════

    private fun showCharging() {
        useNormalLayout()
        setSophieNormal(R.drawable.sophie_point_right)
        showButtonNormal(false)
        hideInput()

        setSpeechNormal(
            "Du machst das richtig toll,\n" +
            "$userName!\n\n" +
            "Jetzt noch eine letzte Sache.\n\n" +
            "Lege dein Tablet bitte in\n" +
            "die Ladestation.\n\n" +
            "Lege es mit den goldenen\n" +
            "Punkten unten auf die\n" +
            "goldene Fl\u00e4che."
        )
    }

    private fun showChargingSuccess() {
        setSophieNormal(R.drawable.sophie_thumbs_up)
        showButtonNormal(true)

        weiterButtonNormal.setOnClickListener { showState(6) }

        setSpeechNormal(
            "Jawoll, $userName!\n\n" +
            "Dein Tablet l\u00e4dt jetzt.\n" +
            "Gut gemacht!\n\n" +
            "Dr\u00fccke auf \u201EWeiter\u201C."
        )
    }

    // ═════════════════════════════════════════════════════
    //  STATE 6 – Abschluss
    // ═════════════════════════════════════════════════════

    private fun showFinish() {
        useNormalLayout()
        setSophieNormal(R.drawable.sophie_wave)
        showButtonNormal(false)
        hideInput()

        setSpeechNormal(
            "Das war\u2019s, $userName!\n\n" +
            "Du hast alle Schritte geschafft.\n" +
            "Ich bin stolz auf dich!\n\n" +
            "Du kannst dein Tablet jetzt\n" +
            "ganz normal benutzen.\n\n" +
            "Dr\u00fccke den runden Knopf\n" +
            "unten am Bildschirm.\n\n" +
            "Falls er nicht sichtbar ist:\n" +
            "Wische vom unteren Rand\n" +
            "nach oben."
        )
    }
}
