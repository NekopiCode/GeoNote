package and06.geonotes

import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

class GatherActivity : AppCompatActivity() {

    companion object {
        val LOCATION = "location"
        val TITLE = "titel"
        val SNIPPET = "snippet"
        var minTime = 4000L // in Millisekunden
        var minDistance = 25.0f // in Metern
        var aktuellesProjekt = Projekt(Date().getTime(), "")
    }

    var aktuellesProjekt = Projekt(Date().getTime(), "")
    var aktuellesNotiz : Notiz? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gather)

        // TODO: Permissions
        requestPermissions(arrayOf<String>(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ), 0)

        // TODO: Datum "textview_aktuelles_projekt"
        val textview = findViewById<TextView>(R.id.textview_Aktuelles_Projekt)
        textview.append(aktuellesProjekt.getDescription())

        // TODO: Location Manager
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        Log.d(javaClass.simpleName, "Verfügbare Provider:")
        providers.forEach {
            Log.d(javaClass.simpleName, "Provider: $it")
        }

        // TODO: Spinnner
        val spinner = findViewById<Spinner>(R.id.spinner_provider)
        spinner.adapter = ArrayAdapter<String>(
            this,
            R.layout.activity_list_item,
            providers
        )
        spinner.onItemSelectedListener = SpinnerProviderItemSelectedListener()

        // TODO: Toggle
        val toggle: ToggleButton = findViewById(R.id.togglebutton_lokalisierung)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // TODO: The toggle is enabled
            } else {
                // TODO: The toggle is disabled
            }
        }
        if (providers.contains("gps"))
            spinner.setSelection(providers.indexOf("gps"));
        // TODO: Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)




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
                    minTime,
                    minDistance,
                    locationListener
                )
                Log.d(javaClass.simpleName, "Lokalisierung gestartet")
            } catch (ex: SecurityException) {
                Log.e(javaClass.simpleName, "Erforderliche Berechtigung ${ex.toString()} nicht erteilt"
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
                        minTime,
                        minDistance,
                        locationListener)
                } catch (ex: SecurityException) {
                    Log.e(javaClass.simpleName, "Erforderliche Berechtigung ${ex.toString()} nicht erteilt")
                }
            }
        }
        override fun onNothingSelected(p0: AdapterView<*>?) {
        }

    }

    // TODO: Button - Standort anzeigen
    fun onButtonStandortAnzeigenClick(view: View?){
        val spinner = findViewById<Spinner>(R.id.spinner_provider)
        if (spinner.count == 0) {
            Toast.makeText(this, "Erforderliche Berechtigungen wurden nicht erteilt", Toast.LENGTH_LONG).show()
            return
        }
        val provider = spinner.selectedItem as String
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            val lastLocation = locationManager.getLastKnownLocation(provider)
            if (lastLocation == null) {
                Toast.makeText(applicationContext, "Keine Geoposition vorhanden.\n Ist Standortermittlung Aktiv?", Toast.LENGTH_SHORT).show()
                return
            }
            /*
            val intent = Intent(this, NoteMapActivity::class.java)
            intent.putExtra(LOCATION, GeoPoint(lastLocation.latitude, lastLocation.longitude) as Serializable)
            startActivity(intent)
             */
            val intent = Intent(this, NoteMapActivity::class.java)
            intent.putExtra(
                LOCATION,
                lastLocation
            )
            intent.putExtra(
                TITLE,
                findViewById<TextView>(R.id.edittext_thema_input).text.toString()
            )
            intent.putExtra(
                SNIPPET,
                findViewById<TextView>(R.id.edittext_notiz_input).text.toString()
            )
            startActivity(intent)
        } catch (ex: SecurityException) {
            Log.e(javaClass.simpleName, "Erforderliche Berechtigung ${ex.toString()} nicht erteilt")
        }
    }

    fun initSpinnerProviders () {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        Log.d(javaClass.simpleName, "Verfügbare Provider:")
        providers.forEach {
            Log.d(javaClass.simpleName, "Provider:  $it")
        }
        val spinner = findViewById<Spinner>(R.id.spinner_provider)
        spinner.adapter = ArrayAdapter<String>(this, R.layout.activity_list_item, providers)
        spinner.onItemSelectedListener = SpinnerProviderItemSelectedListener()
        if (providers.contains("gps"))
            spinner.setSelection(providers.indexOf("gps"))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val locationIndex = permissions.indexOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (grantResults[locationIndex] == PackageManager.PERMISSION_GRANTED) {
            initSpinnerProviders()
        } else {
            Toast.makeText(this, "Standortbestimmung nicht erlaubt --\n" + "nur eingeschränkte Funktionalität verfügbar",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_gather_activity, menu)
        return true
    }

    //TODO: Toolbar onOptionsItemSelected - Menu value function
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id) {
            R.id.item_foot, R.id.item_bicycle, R.id.item_car, R.id.item_car_fast, R.id.item_nichtBewegen -> {
                item.setChecked(true)
                val map = hashMapOf<Int, Pair<Long, Float>>(
                    R.id.item_foot to  (9000L to 10.0f),
                    R.id.item_bicycle to (4000L to 25.0f),
                    R.id.item_car to (4000L to 50.0f),
                    R.id.item_car_fast to (4000L to 100.0f),
                    R.id.item_nichtBewegen to (60000L to 5.0f)
                )
                val pair = map.get(id) ?: return true
                minTime = pair.first
                minDistance = pair.second
               // Toast.makeText(this, "Neues GPS-Intervall \"$item.title\". Bitte Lokalisierung neu starten.", Toast.LENGTH_LONG).show()
                return true
            }

            R.id.menu_projekt_bearbeiten -> {
                openProjektBearbeitenDialog()
            }

            R.id.menu_projekt_auswaehlen -> {
                openProjektAuswaehlenDialog()
            }
            
        }
        return super.onOptionsItemSelected(item)
    }

    // TODO: AlertDialog, Database zugang - Fenster zum bearbeiten von Projektbeschreibung
    fun openProjektBearbeitenDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_projekt_bearbeiten, null)
        val textView = dialogView.findViewById<TextView>(R.id.textview_dialog_projekt_bearbeiten)
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
        textView.text = dateFormat.format(Date(aktuellesProjekt.id))
        val editText = dialogView.findViewById<TextView>(R.id.edittext_dialog_projekt_bearbeiten)
        editText.text = aktuellesProjekt.beschreibung
        val database = GeoNotesDatabase.getInstance(this)
        with(AlertDialog.Builder(this)){
            setView(dialogView)
            setTitle("Projektbeschreibung eingeben/ändern")
            setPositiveButton("Übernehmen", DialogInterface.OnClickListener{ dialog, id ->
            // TODO: Projektebeschreibung Ändern
                aktuellesProjekt.beschreibung = editText.text.toString().trim()
                findViewById<TextView>(R.id.textview_Aktuelles_Projekt).text =
                    getString(R.string.aktuelles_projekt_prefix) + aktuellesProjekt.getDescription()
                // TODO: Projekt-Update in Datenbank
                GlobalScope.launch {
                    database.projekteDao().updateProjekt(aktuellesProjekt)
                    Log.d(javaClass.simpleName, "Update Projekt $aktuellesProjekt")
                    val projekte = database.projekteDao().getProjekte()
                    projekte.forEach {
                        Log.d(javaClass.simpleName, "Projekt $it")
                    }
                }
            })
            setNegativeButton("Abbrechen", DialogInterface.OnClickListener { dialog, id ->

            }).show()
        }
    }

    // TODO: AlertDialog - Projekt auswahlliste
    // Coroutine Mainthreat wartet bist der innere Threat "Projekte" aus Datenbank geholt hat.
    //Projektbeschreibung aus der Datenbank wird in eine ArrayList<CharSequence> umkopiert.
    fun openProjektAuswaehlenDialog() {
        val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var projekte: List<Projekt>? = null
            withContext(Dispatchers.IO) {
                projekte = database.projekteDao().getProjekte()
            }
            if (projekte.isNullOrEmpty()){
                Toast.makeText(this@GatherActivity, "Noch keine Projekte vorhanden", Toast.LENGTH_LONG).show()
                return@launch
            }
            val items = ArrayList<CharSequence>()
            projekte?.forEach{
                items.add(it.getDescription())
            }
            with(AlertDialog.Builder(this@GatherActivity)) {
                setTitle("Projekt auswählen")
                setItems(items.toArray(emptyArray()),
                DialogInterface.OnClickListener { dialog, id ->
            // TODO: aktuelles Projekt auf ausgewähltes Projekt setzen
                    aktuellesProjekt = projekte?.get(id)!!
                    val textView = findViewById<TextView>(R.id.textview_Aktuelles_Projekt)
                    textView.text = getString(R.string.aktuelles_projekt_prefix) +
                            aktuellesProjekt.getDescription()
            // TODO: Notiz zum Projekt anzeigen
                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.IO) {
                            val notizen = database.notizenDao().getNotizen(aktuellesProjekt.id)
                            aktuellesNotiz = notizen.last()
                        }
                        findViewById<TextView>(R.id.edittext_thema_input).text =
                            aktuellesNotiz?.thema
                        findViewById<TextView>(R.id.edittext_thema_input).text =
                            aktuellesNotiz?.notiz
                    }
                })
                show()
            }
        }
    }

    //TODO: Lifecyle - onPause - LocationManager Pause.
    override fun onPause() {
        super.onPause()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
    }

    //TODO: Lifecyle - onResume - Update GPS & co.
    override fun onResume() {
        super.onResume()
        if (findViewById<ToggleButton>(R.id.togglebutton_lokalisierung).isChecked()){
            val spinner = findViewById<Spinner>(R.id.spinner_provider)
            val provider = spinner.selectedItem as String
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener)
            } catch (ex: SecurityException) {
                Log.e(javaClass.simpleName, "Erforderliche Berechtigung ${ex.toString()} nicht erteilt")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
    }

    val locationListener = NoteLocationListener()

    inner class NoteLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(javaClass.simpleName, "Empfangene Geodaten:\n$location")
            /*val textview = findViewById<TextView>(R.id.textview_output)
            textview.append("\nEmpfangene Geodaten:\n$location \n")
             */
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
        override fun onProviderEnabled(provider: String) {
        }
        override fun onProviderDisabled(provider: String) {
        }
    }

    // TODO: Button Notiz Speichern
    fun onButtonNotizSpeichernClick (view: View?) {
        val themaText = findViewById<TextView>(R.id.edittext_thema_input).text.toString().trim()
        val notizText = findViewById<TextView>(R.id.edittext_notiz_input).text.toString().trim()
        if (themaText.isEmpty() || notizText.isEmpty()) {
            Toast.makeText(this, "Thema und Notiz dürfen nicht leer sein", Toast.LENGTH_LONG).show()
            return
        }
        var lastLocation : Location? = null
        var provider = ""
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val spinner = findViewById<Spinner>(R.id.spinner_provider)
            provider = spinner.selectedItem as String
            lastLocation = locationManager.getLastKnownLocation(provider)
        } catch ( ex: SecurityException) {
            Log.e(javaClass.simpleName, "Erforderliche Berechtigung ${ex.toString()} nicht erteilt")
        }
        if (lastLocation == null && aktuellesNotiz == null) {
            Toast.makeText(this, "Noch keine Geoposition ermittelt. Bitte später nochmal versuchen", Toast.LENGTH_LONG).show()
            return
        }
        // TODO: Room Insert - Test
        //val testProjekt = Projekt(Date().getTime(), "")
        val database = GeoNotesDatabase.getInstance(this)
        GlobalScope.launch {
            val id = database.projekteDao().insertProjekt(aktuellesProjekt)
            Log.d(javaClass.simpleName, "Projekt $aktuellesProjekt mit id=$id in Datenbank geschrieben")
            if (aktuellesNotiz == null && lastLocation !== null) {
                // TODO: Location speichern
                database.locationsDao().insertLocation(
                    Location(
                    lastLocation.latitude,
                    lastLocation.longitude,
                    lastLocation.altitude,
                    provider
                ))
                // TODO: Notiz speichern
                aktuellesNotiz = Notiz(
                    null,
                    aktuellesProjekt.id,
                    lastLocation.latitude,
                    lastLocation.longitude,
                    themaText,
                    notizText
                )
                aktuellesNotiz?.id = database.notizenDao().insertNotiz(aktuellesNotiz!!)
                Log.d(javaClass.simpleName, "Notiz $aktuellesNotiz gespeichert")
            } else if (aktuellesNotiz != null){
                // TODO: Notiz aktualisieren
                aktuellesNotiz?.thema = themaText
                aktuellesNotiz?.notiz = notizText
                database.notizenDao().insertNotiz(aktuellesNotiz!!)
                Log.d(javaClass.simpleName, "Notiz $aktuellesNotiz aktualisiert")
            }
        }
        //Alert Dialog
        with(AlertDialog.Builder(this)) {
            setTitle("Notiz weiter bearbeiten?")
            setNegativeButton("Nein", DialogInterface.OnClickListener {
                    dialog, id ->
                aktuellesNotiz == null
                findViewById<TextView>(R.id.edittext_thema_input).text = ""
                findViewById<TextView>(R.id.edittext_notiz_input).text = ""
            })
            setPositiveButton("Ja", DialogInterface.OnClickListener{
                dialog, id ->
            })
            show()
        }
    }


}


