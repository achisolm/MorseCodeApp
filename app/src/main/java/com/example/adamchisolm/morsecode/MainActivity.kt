package com.example.adamchisolm.morsecode

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    var letToCodeDict: HashMap<String, String> = HashMap()
    var codeToLetDict: HashMap<String, String> = HashMap()

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

        // Show code button
        codeButton.setOnClickListener { view ->
            showCodes()
            hideKeyboard()
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
}
