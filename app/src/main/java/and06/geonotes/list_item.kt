package and06.geonotes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner

class list_item : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_item)

        val textview = findViewById<Spinner>(R.id.spinner_provider)
        val adapter = ArrayAdapter<String>(this, R.layout.activity_list_item,
            arrayOf("gps", "passiv", "network"))
        textview.setAdapter(adapter)

    }
}