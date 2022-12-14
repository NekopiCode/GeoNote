package and06.geonotes

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList


class GatherActivity : AppCompatActivity() {

    companion object {
        val LOCATION = "location"
        val TITLE = "titel"
        val SNIPPET = "snippet" //
        var minTime = 4000L // in Millisekunden
        var minDistance = 25.0f // in Metern
        val NOTIZEN = "notizen"
        val INDEX_AKTUELLE_NOTIZ = "index_aktuelle_notiz"
        val AKTUELLES_PROJEKT = "aktuelles_projekt"
        val AKTUELLE_NOTIZ = "aktuelle_notiz"

    }

    var aktuellesProjekt = Projekt(Date().getTime(), "")
    var aktuelleNotiz: Notiz? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gather)
        // onCreate Start
        // Permissions
        requestPermissions(
            arrayOf<String>(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ), 0
        )

        // Location Manager
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        Log.d(javaClass.simpleName, "Verfügbare Provider:")
        providers.forEach {
            Log.d(javaClass.simpleName, "Provider: $it")
        }

        // Spinnner
        val spinner = findViewById<Spinner>(R.id.spinner_provider)
        spinner.adapter = ArrayAdapter<String>(
            this,
            R.layout.activity_list_item,
            providers
        )
        spinner.onItemSelectedListener = SpinnerProviderItemSelectedListener()

        // Toggle
        val toggle: ToggleButton = findViewById(R.id.togglebutton_lokalisierung)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled
            } else {
                // The toggle is disabled
            }
        }
        if (providers.contains("gps"))
            spinner.setSelection(providers.indexOf("gps"))

        // Toolbar
        //val textview = findViewById<TextView>(R.id.textview_aktuelles_projekt)
        val textview = findViewById<TextView>(R.id.textview_Aktuelles_Projekt)
        textview.append(aktuellesProjekt.getDescription())
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (savedInstanceState != null) {
            aktuellesProjekt =
                savedInstanceState.getParcelable(AKTUELLES_PROJEKT)!!
            val notiz: Notiz? =
                savedInstanceState.getParcelable(AKTUELLE_NOTIZ)
            if (notiz != null) aktuelleNotiz = notiz!!
            return
        }

        val projektId = getSharedPreferences("PREFERENCES", Context.MODE_PRIVATE).getLong("ID_ZULETZT_GEOEFFNETES_PROJEKT", 0)

    }// onCreate End

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(AKTUELLES_PROJEKT, aktuellesProjekt)
        if (aktuelleNotiz != null)
            outState.putParcelable(AKTUELLE_NOTIZ, aktuelleNotiz)
    }

    private fun showProperties(manager: LocationManager, providerName: String): String {
        val locationProvider = manager.getProvider(providerName)
            ?: return "Kein Provider unter dem Namen $providerName verfügbar"
        return String.format(
            "Provider: %s\nHorizontale Genauigkeit: %s\nUnterstützt Höhenermittlung: %s\nErfordert Satellit: %s",
            providerName,
            if (locationProvider.accuracy == 1) "FINE"
            else "COARSE",
            locationProvider.supportsAltitude(),
            locationProvider.requiresSatellite()
        )
    }

    fun onToggleButtonLokalisierenClick(view: View?) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((view as ToggleButton).isChecked()) {
            val spinner = findViewById<Spinner>(R.id.spinner_provider)
            val provider = spinner.selectedItem as String?
            if (provider == null) {
                Toast.makeText(
                    this,
                    "Erforderliche Berechtigungen wurden nicht erteilt",
                    Toast.LENGTH_LONG
                ).show()
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
                Log.e(
                    javaClass.simpleName,
                    "Erforderliche Berechtigung ${ex.toString()} nicht erteilt"
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
                        locationListener
                    )
                } catch (ex: SecurityException) {
                    Log.e(
                        javaClass.simpleName,
                        "Erforderliche Berechtigung ${ex.toString()} nicht erteilt"
                    )
                }
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }
    }

    // Button - Standort anzeigen
    fun onButtonStandortAnzeigenClick(view: View?) {
        if (aktuelleNotiz == null) {
            Toast.makeText(this, "Bitte Notiz auswählen oder speichern",
                Toast.LENGTH_LONG).show()
            return
        }
       val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var notizen : List<Notiz>? = null
            withContext(Dispatchers.IO) {
                notizen = database.notizenDao().getNotizen(aktuellesProjekt.id)
            }
            if (!notizen.isNullOrEmpty()) {

                var intentNoteMap = Intent(this@GatherActivity, NoteMapActivity::class.java)
                intentNoteMap.putParcelableArrayListExtra(NOTIZEN, ArrayList<Notiz>(notizen!!))
                intentNoteMap.putExtra(INDEX_AKTUELLE_NOTIZ, notizen?.indexOf(aktuelleNotiz!!))

                val selectItemArray = resources.getStringArray(R.array.standort_Anzeige_Auswahl)
                var checkedItem = 0
                val alertDialog_Builder_Activity_Map_Show_Choice = AlertDialog.Builder(this@GatherActivity)
                alertDialog_Builder_Activity_Map_Show_Choice.setTitle("Anzeige des Standorts")
                alertDialog_Builder_Activity_Map_Show_Choice.setSingleChoiceItems(selectItemArray, checkedItem) {_, which: Int ->
                    checkedItem = which
                }
                alertDialog_Builder_Activity_Map_Show_Choice.setPositiveButton("OK"){ _,_->
                    if (checkedItem == 1){
                        intentNoteMap.setClass(this@GatherActivity, OsmWebViewActivity::class.java)
                        startActivity(intentNoteMap)
                        Toast.makeText(applicationContext, "OsmWebViewActivity wurde Aktiviert", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(intentNoteMap)
                        Toast.makeText(applicationContext, "NoteMapActivity wurde Aktiviert", Toast.LENGTH_SHORT).show()
                    }
                }
                alertDialog_Builder_Activity_Map_Show_Choice.setNegativeButton("ABBRECHEN"){_,_ ->
                }

                alertDialog_Builder_Activity_Map_Show_Choice.create()
                alertDialog_Builder_Activity_Map_Show_Choice.show()
                //startActivityForResult(intentNoteMap, 0)
            }
        }
        autoSave()
    }


    fun initSpinnerProviders() {
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
            Toast.makeText(
                this,
                "Standortbestimmung nicht erlaubt --\n" + "nur eingeschränkte Funktionalität verfügbar",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_gather_activity, menu)
        return true
    }

    //Toolbar onOptionsItemSelected - Menu value function
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.item_foot, R.id.item_bicycle, R.id.item_car, R.id.item_car_fast, R.id.item_nichtBewegen -> {
                item.setChecked(true)
                val map = hashMapOf<Int, Pair<Long, Float>>(
                    R.id.item_foot to (9000L to 10.0f),
                    R.id.item_bicycle to (4000L to 25.0f),
                    R.id.item_car to (4000L to 50.0f),
                    R.id.item_car_fast to (4000L to 100.0f),
                    R.id.item_nichtBewegen to (60000L to 1.0f)
                )
                val pair = map.get(id) ?: return true
                minTime = pair.first
                minDistance = pair.second
                return true
            }
            R.id.menu_projekt_bearbeiten -> { openProjektBearbeitenDialog() }
            R.id.menu_projekt_auswaehlen -> { openProjektAuswaehlenDialog() }
            R.id.menu_notiz_Loeschen -> { notizLoeschen() }
            R.id.menu_projekt_versenden -> { projektVersenden() }
            R.id.menu_osm_oeffnen -> { openWebView() }
            R.id.menu_in_googlemap_oeffnen -> { openGoogleMap() }

        }
        return super.onOptionsItemSelected(item)
    }

    // GPX versenden/Geo Daten
    fun projektVersenden() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.
            getExternalStorageState())) {
            Toast.makeText(this, "External Storage nicht verfügbar",
                Toast.LENGTH_LONG).show()
            return
        }
        val database = GeoNotesDatabase.getInstance(this)
        GlobalScope.launch {
            val notizen =
                database.notizenDao().getNotizen(aktuellesProjekt.id)
            if (notizen.isNullOrEmpty()) {
                Toast.makeText(this@GatherActivity, "keine Notizen vorhanden", Toast.LENGTH_LONG).show()
                    return@launch
            }
            val generator = GpxGenerator()
            val uri = generator.createGpxFile(this@GatherActivity,
                notizen, aktuellesProjekt.getDescription())
            with(Intent(Intent.ACTION_SEND)) {
                type = "text/xml"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("xxx@test.com"))
                putExtra(Intent.EXTRA_SUBJECT,
                    aktuellesProjekt.getDescription())
                putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(this, "Mail versenden"))
            }
        }
    }

    // Toolbar Button - Delete Current Note and last Project with last Note.
    fun notizLoeschen () {
        val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var notizen: List<Notiz>? = null
            withContext(Dispatchers.IO) {
                notizen = database.notizenDao().getNotizen(aktuellesProjekt.id)
            }
            if (aktuelleNotiz == null) {
                Toast.makeText(applicationContext, "Notiz wurde nicht ausgewählt!!", Toast.LENGTH_SHORT).show()
                return@launch
            } else {
                if (notizen?.size == 1){
                    with(AlertDialog.Builder(this@GatherActivity)){
                        setTitle("Löschen der letzten Notiz löscht das Projekt. Fortfahren?")
                        setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                            CoroutineScope(Dispatchers.Main).launch {
                                withContext(Dispatchers.IO){
                                    database.projekteDao().deleteAllNoteFromProjekt(aktuellesProjekt.id)
                                    database.projekteDao().deleteProjekt(aktuellesProjekt.id)
                                    aktuelleNotiz = null
                                    aktuellesProjekt = Projekt(Date().getTime(), "")
                                }
                            }
                            findViewById<TextView>(R.id.textview_Aktuelles_Projekt).setText("Projekt gelöscht!")
                            findViewById<TextView>(R.id.edittext_thema).setText("")
                            findViewById<TextView>(R.id.edittext_notiz).setText("")
                        })
                        setNegativeButton("Abbrechen", DialogInterface.OnClickListener { dialog, id ->
                        }).show()
                    }
                } else {
                    with(AlertDialog.Builder(this@GatherActivity)) {
                        setTitle("Aktuelle Notiz löschen?")
                        setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                            GlobalScope.launch {
                                database.notizenDao().deleteNoteWithID(aktuelleNotiz?.id)
                                aktuelleNotiz = null
                            }
                            findViewById<TextView>(R.id.edittext_thema).setText("")
                            findViewById<TextView>(R.id.edittext_notiz).setText("")
                        })
                        setNegativeButton("Abbrechen", DialogInterface.OnClickListener { dialog, id ->
                        }).show()
                    }
                }
            }
        }
    }


    // AlertDialog, Database zugang - Fenster zum bearbeiten von Projektbeschreibung
    fun openProjektBearbeitenDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_projekt_bearbeiten, null)
        val textView = dialogView.findViewById<TextView>(R.id.textview_dialog_projekt_bearbeiten)
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
        textView.text = dateFormat.format(Date(aktuellesProjekt.id))

        val editText = dialogView.findViewById<TextView>(R.id.edittext_dialog_projekt_bearbeiten)
        editText.text = aktuellesProjekt.beschreibung

        val database = GeoNotesDatabase.getInstance(this)
        with(AlertDialog.Builder(this)) {
            setView(dialogView)
            setTitle("Projektbeschreibung eingeben/ändern")
            setPositiveButton("Übernehmen", DialogInterface.OnClickListener { dialog, id ->
                // Projektebeschreibung Ändern
                aktuellesProjekt.beschreibung = editText.text.toString().trim()
                findViewById<TextView>(R.id.textview_Aktuelles_Projekt).text =
                    getString(R.string.aktuelles_projekt_prefix) + aktuellesProjekt.getDescription()
                // Projekt-Update in Datenbank
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

    // AlertDialog - Projekt auswahlliste
    // Coroutine Mainthreat wartet bist der innere Threat "Projekte" aus Datenbank geholt hat.
    // Projektbeschreibung aus der Datenbank wird in eine ArrayList<CharSequence> umkopiert.
    fun openProjektAuswaehlenDialog() {
        val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var projekte: List<Projekt>? = null
            withContext(Dispatchers.IO) {
                projekte = database.projekteDao().getProjekte()
            }
            if (projekte.isNullOrEmpty()) {
                Toast.makeText(this@GatherActivity, "Noch keine Projekte vorhanden", Toast.LENGTH_LONG).show()
                return@launch
            }
            val items = ArrayList<CharSequence>()
            projekte?.forEach {
                items.add(it.getDescription())
            }
            with(AlertDialog.Builder(this@GatherActivity)) {
                setTitle("Projekt auswählen")
                setItems(items.toArray(emptyArray()),
                    DialogInterface.OnClickListener { dialog, id ->
                        // Aktuelles Projekt auf ausgewähltes Projekt setzen
                        aktuellesProjekt = projekte?.get(id)!!
                        val textView = findViewById<TextView>(R.id.textview_Aktuelles_Projekt)
                        textView.text = getString(R.string.aktuelles_projekt_prefix) + aktuellesProjekt.getDescription()
                        // Notiz zum Projekt anzeigen
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                val notizen = database.notizenDao().getNotizen(aktuellesProjekt.id)
                                aktuelleNotiz = notizen.last()
                            }
                            findViewById<TextView>(R.id.edittext_thema).text = aktuelleNotiz?.thema
                            findViewById<TextView>(R.id.edittext_notiz).text = aktuelleNotiz?.notiz
                        }
                    })
                show()
            }
        }
    }

    //Notiz - Auto Save
    fun autoSave() {
        val themaText = findViewById<TextView>(R.id.edittext_thema).text.toString().trim()
        val notizText = findViewById<TextView>(R.id.edittext_notiz).text.toString().trim()
        val database = GeoNotesDatabase.getInstance(this)

        GlobalScope.launch {
            if (aktuelleNotiz != null) {
                if (notizText != aktuelleNotiz?.notiz.toString() || themaText != aktuelleNotiz?.thema.toString()) {
                    aktuelleNotiz?.thema = themaText
                    aktuelleNotiz?.notiz = notizText
                    database.notizenDao().insertNotiz(aktuelleNotiz!!)
                }
            }
        }
    }

    // Lifecyle - onPause - LocationManager Pause.
    override fun onPause() {
        super.onPause()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
        autoSave()
    }

    // Lifecyle - onResume - Update GPS & co.
    override fun onResume() {
        super.onResume()
        if (findViewById<ToggleButton>(R.id.togglebutton_lokalisierung).isChecked()) {
            val spinner = findViewById<Spinner>(R.id.spinner_provider)
            val provider = spinner.selectedItem as String
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    minTime,
                    minDistance,
                    locationListener
                )
            } catch (ex: SecurityException) {
                Log.e(
                    javaClass.simpleName,
                    "Erforderliche Berechtigung ${ex.toString()} nicht erteilt"
                )
            }
        }
    }

    // Lifecyle - onDestroy - Remove update from Location Manager
    override fun onDestroy() {
        super.onDestroy()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(locationListener)
    }

    val locationListener = NoteLocationListener()

    inner class NoteLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(javaClass.simpleName, "Empfangene Geodaten:\n$location")

        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
        override fun onProviderEnabled(provider: String) {
        }
        override fun onProviderDisabled(provider: String) {
        }
    }

    // Button - Notiz Speichern
    fun onButtonNotizSpeichernClick(view: View) {
        val themaText = findViewById<TextView>(R.id.edittext_thema).text.toString().trim()
        val notizText = findViewById<TextView>(R.id.edittext_notiz).text.toString().trim()
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
        } catch (ex: SecurityException) {
            Log.e(javaClass.simpleName, "Erforderliche Berechtigung$ex.toString() nicht erteilt")
        }
        if (lastLocation == null && aktuelleNotiz == null) {
            Toast.makeText(this, "Noch keine Geoposition ermittelt. Bitte später nochmal versuchen", Toast.LENGTH_LONG).show()
                return
        }
        val database = GeoNotesDatabase.getInstance(this)
        GlobalScope.launch {
                // Projekt speichern:
            val id = database.projekteDao().insertProjekt(aktuellesProjekt)
            Log.d(javaClass.simpleName, "Projekt $aktuellesProjekt mit id=$id in Datenbank geschrieben")
            if (aktuelleNotiz == null && lastLocation != null) {

                    // Location speichern:
                database.locationsDao().insertLocation(Location(
                    lastLocation.latitude, lastLocation.longitude, lastLocation.altitude, provider))

                aktuelleNotiz = Notiz(
                    null, aktuellesProjekt.id, lastLocation.latitude, lastLocation.longitude, themaText, notizText)

                aktuelleNotiz?.id = database.notizenDao().insertNotiz(aktuelleNotiz!!)
                Log.d(javaClass.simpleName, "Notiz $aktuelleNotiz gespeichert")
            } else if (aktuelleNotiz != null){
                // Notiz aktualisieren:
                aktuelleNotiz?.thema = themaText
                aktuelleNotiz?.notiz = notizText
                database.notizenDao().insertNotiz(aktuelleNotiz!!)
                Log.d(javaClass.simpleName, "Notiz $aktuelleNotiz aktualisiert")
            }
        }
        with(AlertDialog.Builder(this)) {
            setTitle("Notiz weiter bearbeiten?")
            setNegativeButton("Nein", DialogInterface.OnClickListener
            { dialog, id ->
                aktuelleNotiz = null
                findViewById<TextView>(R.id.edittext_thema).text = ""
                findViewById<TextView>(R.id.edittext_notiz).text = ""
            })
            setPositiveButton("Ja", DialogInterface.OnClickListener {
                    dialog, id -> }).show()
        }
    }

    // Button - Vorherige Notiz & Nächste Notiz
    fun onButtonVorherigeNotizClick(view: View) {
        val textViewThema = findViewById<TextView>(R.id.edittext_thema)
        val textViewNotiz = findViewById<TextView>(R.id.edittext_notiz)
        if (aktuelleNotiz == null) {
            if (textViewThema.text.isNotEmpty() || textViewNotiz.text.isNotEmpty()) {
                // Fall 1
                Toast.makeText(this, "Notiz wurde noch nicht gespeichert",
                    Toast.LENGTH_LONG).show()
                return
            }
        }
        val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var notizen : List<Notiz>? = null
            withContext(Dispatchers.IO) {
                if (aktuelleNotiz == null) {
                    notizen = database.notizenDao().getNotizen(aktuellesProjekt.id)
                    // Fall 2
                } else {
                    notizen = database.notizenDao().getPreviousNotizen(aktuelleNotiz?.id!!,
                        aktuellesProjekt.id) // Fall 3
                }
            }
            if (!notizen.isNullOrEmpty()) {
                aktuelleNotiz = notizen?.last()
                textViewThema.text = aktuelleNotiz?.thema
                textViewNotiz.text = aktuelleNotiz?.notiz
            }
        }
        autoSave()
    }


    fun onButtonNaechsteNotizClick(view: View) {
        val textViewThema = findViewById<TextView>(R.id.edittext_thema)
        val textViewNotiz = findViewById<TextView>(R.id.edittext_notiz)
        if (aktuelleNotiz == null) {
            if (textViewThema.text.isNotEmpty() || textViewNotiz.text.isNotEmpty()
            ) {
                // Fall 1
                Toast.makeText(this, "Notiz wurde noch nicht gespeichert", Toast.LENGTH_LONG).show()
            }
            return // Fall 1 und Fall 2
        }
        val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var notizen: List<Notiz>? = null
            withContext(Dispatchers.IO) {
                notizen = database.notizenDao().getNextNotizen(aktuelleNotiz?.id!!, aktuellesProjekt.id)
            }
            if (!notizen.isNullOrEmpty()) { // Fall 3a)
                aktuelleNotiz = notizen?.first()
                textViewThema.text = aktuelleNotiz?.thema
                textViewNotiz.text = aktuelleNotiz?.notiz
            } else { // Fall 3b)
                aktuelleNotiz = null
                textViewThema.text = ""
                textViewNotiz.text = ""
            }
        }
        return
        autoSave()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        if (requestCode != 0) return
        val extras = data?.getExtras() ?: return
        val notizID = extras.getLong(NoteMapActivity.AKTUELLE_NOTIZ_ID)
       var database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var notiz : Notiz? = null
            withContext(Dispatchers.IO) {
                notiz = database.notizenDao().getNotiz(notizID)
            }
            if (notiz != null) {
                aktuelleNotiz = notiz
                findViewById<TextView>(R.id.edittext_thema).text = aktuelleNotiz?.thema
                findViewById<TextView>(R.id.edittext_notiz).text = aktuelleNotiz?.notiz
            }
        }
    }

    fun openWebView() {
        if (aktuelleNotiz == null) {
            Toast.makeText(this, "Bitte Notiz auswählen oder speichern",
                Toast.LENGTH_LONG).show()
            return
        }
        val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var notizen: List<Notiz>? = null
            withContext(Dispatchers.IO) {
                notizen = database.notizenDao().getNotizen(aktuellesProjekt.id)
            }
            notizen?.also {
                val intent = Intent(this@GatherActivity, OsmWebViewActivity::class.java)
                intent.putParcelableArrayListExtra(NOTIZEN, ArrayList<Notiz>(it))
                intent.putExtra(INDEX_AKTUELLE_NOTIZ, it.indexOf(aktuelleNotiz!!))
                startActivity(intent)
            }
        }
    }

    //Intent - Google Map - not finish
    fun openGoogleMap() {
        if (aktuelleNotiz == null) {
            Toast.makeText(this, "Bitte Notiz auswählen oder speichern",
                Toast.LENGTH_LONG).show()
            return
        }
        val database = GeoNotesDatabase.getInstance(this)
        CoroutineScope(Dispatchers.Main).launch {
            var notizen: List<Notiz>? = null
            withContext(Dispatchers.IO) {
                notizen = database.notizenDao().getNotizen(aktuellesProjekt.id)
            }
            notizen?.also {
                var currentNoteLocation = "${aktuelleNotiz!!.latitude}, ${aktuelleNotiz!!.longitude}"
                //Toast.makeText(applicationContext, "$currentNoteLocation", Toast.LENGTH_SHORT).show()

                val gmmIntentUri = Uri.parse("geo:0,0?q=${currentNoteLocation}(Thema: ${aktuelleNotiz!!.thema}  |  Notiz: ${aktuelleNotiz!!.notiz} )")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)


            }
        }
    }

}




