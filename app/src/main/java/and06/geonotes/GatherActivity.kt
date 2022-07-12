package and06.geonotes

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*

class GatherActivity : AppCompatActivity() {

    companion object {
        val MIN_TIME = 5000L // in Millisekunden
        val MIN_DISTANCE = 5.0f // in Metern
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gather)

        //Location Manager
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        Log.d(javaClass.simpleName, "Verfügbare Provider:")
        providers.forEach {
            Log.d(javaClass.simpleName, "Provider: $it")
        }

        //Spinnner
        val spinner = findViewById<Spinner>(R.id.spinner_provider)
        spinner.adapter = ArrayAdapter<String>(
            this,
            R.layout.activity_list_item,
            providers
        )
        spinner.onItemSelectedListener = SpinnerProviderItemSelectedListener()

        //Toggle
        val toggle: ToggleButton = findViewById(R.id.togglebutton_lokalisierung)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled

            } else {
                // The toggle is disabled
            }
        }

        if (providers.contains("gps"))
            spinner.setSelection(providers.indexOf("gps"));
    }

    private fun showProperties(manager: LocationManager, providerName: String): String {
        val locationProvider = manager.getProvider(providerName) ?:
        return "Kein Provider unter dem Namen $providerName verfügbar"
        return String.format("Provider: %s\nHorizontale Genauigkeit: %s\nUnterstützt Höhenermittlung: %s\nErfordert Satellit: %s",
            providerName,
            if (locationProvider.accuracy == 1) "FINE"
            else "COARSE",
            locationProvider.supportsAltitude(),
            locationProvider.requiresSatellite())
    }

    fun onToggleButtonLokalisierenClick(view: View?) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((view as ToggleButton).isChecked()) {
            val spinner = findViewById<Spinner>(R.id.spinner_provider)
            val provider = spinner.selectedItem as String?
            if (provider == null) {
                Toast.makeText(this, "Erforderliche Berechtigungen wurden nicht erteilt", Toast.LENGTH_LONG).show()
                    (view as ToggleButton).setChecked(false)
                    return
            }
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    MIN_TIME,
                    MIN_DISTANCE,
                    locationListener
                )
                Log.d(javaClass.simpleName, "Lokalisierung gestartet")
            } catch (ex: SecurityException) {
                Log.e(javaClass.simpleName, "Erforderliche Berechtigung $ex.toString() nicht erteilt"
                )
            }
            Log.d(javaClass.simpleName, showProperties(locationManager, provider))
        } else {
            locationManager.removeUpdates(locationListener)
            Log.d(javaClass.simpleName, "Lokalisierung beendet")
        }

    }

    inner class SpinnerProviderItemSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (findViewById<ToggleButton>(R.id.togglebutton_lokalisierung).isChecked()) {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    locationManager.removeUpdates(locationListener)
                val provider = (view as TextView).text.toString()
                try {
                    locationManager.requestLocationUpdates(
                        provider,
                        MIN_TIME,
                        MIN_DISTANCE,
                        locationListener)
                } catch (ex: SecurityException) {
                    Log.e(javaClass.simpleName, "Erforderliche Berechtigung ${ex.toString()} nicht erteilt")
                }
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }

    }

    inner class NoteLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(javaClass.simpleName, "Empfangene Geodaten:\n$location")
            val textview = findViewById<TextView>(R.id.textview_output)
            textview.append("\nEmpfangene Geodaten:\n$location \n")
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String) {

        }

        override fun onProviderDisabled(provider: String) {
        }
    }

    val locationListener = NoteLocationListener()

    override fun onDestroy() {
        super.onDestroy()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
    }

    fun onButtonStandortAnzeigenClick(view: View?){
        val intent = Intent(this, NoteMapActivity::class.java)
        startActivity(intent)
    }

}


