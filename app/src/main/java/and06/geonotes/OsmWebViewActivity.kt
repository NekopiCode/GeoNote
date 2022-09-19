package and06.geonotes

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity


class OsmWebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_osm_web_view)

        val webview = findViewById<WebView>(R.id.osm_webview)
        webview.loadUrl("file:///android_res/raw/index.html")
        webview.settings.javaScriptEnabled = true

        val notizen = intent.getParcelableArrayListExtra<Notiz>(GatherActivity.NOTIZEN)
        val indexAktuelleNotiz = intent.extras?.getInt(GatherActivity.INDEX_AKTUELLE_NOTIZ) ?:0

        val myinterface = object {
            @JavascriptInterface
            fun getIndexAktuelleNotiz(): Int {
                return indexAktuelleNotiz
            }

            @JavascriptInterface
            fun getLatitude(i: Int): Double {
                return notizen!!.get(i).latitude
            }
            @JavascriptInterface
            fun getLongitude(i: Int): Double {
                return notizen!!.get(i).longitude
            }

            @JavascriptInterface
            fun getThema(i: Int): String {
                return notizen!!.get(i).thema
            }
            @JavascriptInterface
            fun getNotiz(i: Int): String {
                return notizen!!.get(i).notiz
            }

            @JavascriptInterface
            fun getNotizenCount(): Int {
                return notizen!!.count()
            }
        }

        webview.addJavascriptInterface(myinterface, "Android")

    }


}