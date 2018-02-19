package com.example.adamchisolm.morsecode

import android.app.Activity
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.lang.Math.round
import java.util.*
import kotlin.concurrent.timerTask


// Constant for generating sound waves
val SAMPLE_RATE = 44100

class MainActivity : AppCompatActivity() {

    var letToCodeDict: HashMap<String, String> = HashMap()
    var codeToLetDict: HashMap<String, String> = HashMap()

    val dotLength:Int = 50 // miliSeconds
    val dashLength:Int = dotLength * 3

    // Pregenerate the sine wave sound buffers for the dot and th edash sounds
    val dotSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dotLength)
    val dashSoundBuffer:ShortArray = genSineWaveSoundBuffer(550.0, dashLength)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        // For Scrolling
        mTextView.movementMethod = ScrollingMovementMethod();

        // Wire up button
        testButton.setOnClickListener { view ->
            appendTextAndScroll(inputText.text.toString());
            hideKeyboard();
        }

        // Get morse code into dictionaries for later use
        val morseJSON = loadMorseJSON()
        makeMorseDicts(morseJSON)

        // Button to show codes
        codeButton.setOnClickListener { view ->
            showCodes()
            hideKeyboard()
        }

        // Translate button
        translateButton.setOnClickListener { view ->
            var message = inputText.text.toString()
            if (isMorseCode(message)) {
                translateMorseToString(message)
            }
            else {
                message = message.toLowerCase()
                translateStringToMorse(message)
            }
        }

        // Play sound button
        playSoundButton.setOnClickListener { view ->
            var message = inputText.text.toString()
            if (isMorseCode(message))
                playString(message)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun appendTextAndScroll(text: String) {
        if (mTextView != null) {
            mTextView.append(text + "\n")
            val layout = mTextView.getLayout()
            if (layout != null) {
                val scrollDelta = (layout!!.getLineBottom(mTextView.getLineCount() - 1) - mTextView.getScrollY() - mTextView.getHeight())
                if (scrollDelta > 0)
                    mTextView.scrollBy(0, scrollDelta)
            }
        }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(if(currentFocus == null) View(this) else currentFocus)
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun loadMorseJSON() : JSONObject {
        val filePath = "morse.json";

        val jsonStr = application.assets.open(filePath).bufferedReader().use{
            it.readText()
        }

        val jsonObj = JSONObject(jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1))

        return jsonObj
    }

    fun makeMorseDicts(jsonObj : JSONObject) {
        for (k in jsonObj.keys()) {
            val code : String = jsonObj[k].toString()
            letToCodeDict.set(k.toString(), code)
            codeToLetDict.set(code, k.toString())
        }
    }
    fun showCodes() {
        appendTextAndScroll("Here are the codes:")
        for (k in letToCodeDict.keys.sorted()) {
            appendTextAndScroll("$k: ${letToCodeDict[k]}")
        }
    }

    fun translateStringToMorse(message : String) {
        var morseMessage = ""
        for (letter in message) {
            if (letter == ' ') {
                morseMessage += " / "
            }
            else if (letter.toString() in letToCodeDict) {
                morseMessage += letToCodeDict.get(letter.toString())
            }
            else {
                morseMessage += "?"
            }
        }
        appendTextAndScroll(morseMessage)
        hideKeyboard()
    }

    fun translateMorseToString(message : String) {
        var morse = message.split(' ')
        var translatedMessage = ""

        for (c in morse) {
            if (c == "/") {
                translatedMessage += " "
            }
            else if (c in codeToLetDict) {
                translatedMessage += codeToLetDict.get(c)
            }
            else {
                translatedMessage += "?"
            }
        }

        appendTextAndScroll(translatedMessage)
        hideKeyboard()
    }

    fun isMorseCode(message : String) : Boolean {
        for (c in message) {
            if ( c != '.' && c != '-' && c != ' ') {
                return false
            }
        }

        return true
    }

    fun playString(s:String, i: Int = 0) : Unit{
        // s = string of '.' and '-' to play
        // i is index of which char to play
        // This function is called recursively

        if (i > s.length - 1)
            return

        var mDelay : Long = 0

        // thenFun = lambda function that will switch back to main thread and play the next char
        var thenFun : () -> Unit = { ->
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                playString(s, i+1)
            })
        }

        var c = s[i]
        Log.d("Log", "Processing pos: " + i + " char: [" + c + "]")
        if (c == '.')
            playDot(thenFun)
        else if (c == '-')
            playDash(thenFun)
        else if (c == '/')
            pause(6 * dotLength, thenFun)
        else if (c == ' ')
            pause(2 * dotLength, thenFun)
    }

    // The following three functions are asynchronous. They return back immediately, but then they call the onDone function when they are done

    // Play dash sound and pause and then do onDone
    fun playDash(onDone : () -> Unit = { /*noop*/ }) {
        Log.d("DEBUG", "playDash")
        playSoundBuffer(dashSoundBuffer, { -> pause(dotLength, onDone)})
    }

    // Play dot sound and pause and then do onDone
    fun playDot(onDone : () -> Unit = { /*noop*/ }) {
        Log.d("Debug", "playDot")
        playSoundBuffer(dotSoundBuffer, { -> pause(dotLength, onDone)})
    }

    fun pause(durationMSec: Int, onDone: () -> Unit = { /*noop */ }) {
        // Pause for the given number of miliSeconds, then call onDone
        Log.d("Debug", "pause: " + durationMSec)
        Timer().schedule(timerTask {
            onDone()
        }, durationMSec.toLong())
    }

    // Sound code based on code from:
    // http://programminglife.io/generating-sine-wave-sound-with-android/
    // Converted to Kotlin and modified by Bob Bradley SP18

    private fun genSineWaveSoundBuffer(frequency: Double, durationMSec: Int) : ShortArray {
        // frequency - sin wave to generate
        // durationMSec (milliseconds) for sound to last

        val duration: Int = round((durationMSec / 1000.0) * SAMPLE_RATE).toInt()

        // Generate Sine wave
        var mSound : Double
        val mBuffer = ShortArray(duration)
        for (i in 0 until duration) {
            mSound = Math.sin(2.0 * Math.PI * i.toDouble() / (SAMPLE_RATE / frequency))
            mBuffer[i] = (mSound * java.lang.Short.MAX_VALUE).toShort()
        }
        return mBuffer
    }

    private fun playSoundBuffer(mBuffer:ShortArray, onDone : () -> Unit = { /* noop */ } ) {
        // mBuffer is the sound wave buffer to play
        // onDone is a lambda function to call when the sound is done
        var minBufferSize = SAMPLE_RATE/10
        if (minBufferSize < mBuffer.size) {
            minBufferSize = minBufferSize + minBufferSize *(Math.round(mBuffer.size.toFloat()) / minBufferSize.toFloat()).toInt()
        }

        // Copy data into new buffer that is the correct buffer size
        val nBuffer = ShortArray(minBufferSize)
        for (i in nBuffer.indices) {
            if (i < mBuffer.size)
                nBuffer[i] = mBuffer[i]
            else
                nBuffer[i] = 0
        }

        val mAudioTrack = AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM)

        mAudioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume())
        mAudioTrack.setNotificationMarkerPosition(mBuffer.size)
        mAudioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onPeriodicNotification(track: AudioTrack) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun onMarkerReached(track: AudioTrack) {
                Log.d("log", "Audio Track end of file reached...")
                mAudioTrack.stop(); mAudioTrack.release(); onDone()
            }
        })
        mAudioTrack.play()
        mAudioTrack.write(nBuffer, 0, minBufferSize)
    }
}
