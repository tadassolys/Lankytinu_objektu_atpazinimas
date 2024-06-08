package com.example.lankytinu_objektu_atpazinimas

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class ObjectDetectedActivity : AppCompatActivity() {

    private lateinit var tvPrimaryInfo: TextView
    private lateinit var btnAdditionalInfo: Button
    private lateinit var webView: WebView
    private lateinit var btnMaps: Button
    private lateinit var btnReturn: Button
    private var isWebViewVisible = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_detected)

        tvPrimaryInfo = findViewById(R.id.tvPrimaryInfo)
        btnAdditionalInfo = findViewById(R.id.btnAdditionalInfo)
        webView = findViewById(R.id.webview)
        btnMaps = findViewById(R.id.btnMaps)
        btnReturn = findViewById(R.id.btnReturn)

        val gson = Gson()
        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        val detectedDataJson = intent.getStringExtra("detectedData")
        val detectedData: Map<String, Map<String, String>> = gson.fromJson(detectedDataJson, type)

        val firstEntry = detectedData.entries.firstOrNull()
        val info = firstEntry?.value?.get("info") ?: "Objektas neatpažintas, bandykite dar kartą."
        val url = firstEntry?.value?.get("url") ?: "https://google.com/badpage"
        val coordinates = firstEntry?.value?.get("coordinates") ?: "Kordinatės nepasiekiamos"

        tvPrimaryInfo.text = info

        btnAdditionalInfo.setOnClickListener {
            webView.apply {
                visibility = View.VISIBLE
                webViewClient = WebViewClient()
                loadUrl(url)
                isWebViewVisible = true
            }
        }

        btnMaps.setOnClickListener {
            if (coordinates == "No Coordinates") {
                Toast.makeText(this, "Kordinačių nėra.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val coords = coordinates.split(",")
            if (coords.size < 2) {
                Toast.makeText(this, "Netinkamas kordinačių formatas.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val latitude = coords[0].trim()
            val longitude = coords[1].trim()

            val gmmIntentUri = Uri.parse("geo:0,0?q=$latitude,$longitude(Label)")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val clipboard =
                    getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip =
                    android.content.ClipData.newPlainText("Coordinates", "$latitude, $longitude")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    this,
                    "Google Maps nepasiekama. Kordinatės nukopijuotos į iškarpinę.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


        btnReturn.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        if (isWebViewVisible) {
            webView.visibility = View.GONE
            isWebViewVisible = false
        } else {
            super.onBackPressed()
        }
    }
}
