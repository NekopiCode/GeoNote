package and06.geonotes


import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MinimapOverlay
import java.io.File
import java.io.Serializable

class NoteMapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notemap)

        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName

        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath

        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        val map = findViewById<MapView>(R.id.mapview)
        map.setTileSource(TileSourceFactory.MAPNIK)

        val extras = intent.extras
        if (extras == null) return
        val location = extras.getParcelable<Location>(GatherActivity.LOCATION)
        if (location == null) return
        val marker = Marker(map)
        val markerIcon = getDrawable(R.drawable.crosshair)
       // marker.icon = markerIcon
        marker.rotation
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.position = GeoPoint(location.latitude, location.longitude)

        marker.title = extras.getString(GatherActivity.TITLE)
        marker.snippet = decimalToSexagesimal(location.latitude, location.longitude)
        marker.subDescription = extras.getString(GatherActivity.SNIPPET)

        //Back_UP Code
        //marker.title = extras.getString(GatherActivity.TITLE)
        //marker.snippet = extras.getString(GatherActivity.SNIPPET)


        map.overlays.add(marker)
        val controller = map.controller
        controller.setCenter(marker.position)
        controller.setZoom(16.0)
        //map.setMapOrientation(45.0f)


    }

    fun decimalToSexagesimal(latitude: Double, longitude: Double): String {
        val latDegrees = latitude.toInt()
        val lonDegrees = longitude.toInt()
        val latTempMinutes = Math.abs((latitude - latDegrees) * 60)
        val lonTempMinutes = Math.abs((longitude - lonDegrees) *
                60)
        val latMinutes = latTempMinutes.toInt()
        val lonMinutes = lonTempMinutes.toInt()
        val latTempSeconds = (latTempMinutes - latMinutes) * 60
        val lonTempSeconds = (lonTempMinutes - lonMinutes) * 60
        // auf drei Stellen runden
        val latSeconds = Math.round(latTempSeconds * 1000)/1000.0
        val lonSeconds = Math.round(lonTempSeconds * 1000)/1000.0
        val latDegreesString = Math.abs(latDegrees).toString() + if (latitude < 0) "°S " else "°N "
        val lonDegreesString = Math.abs(lonDegrees).toString() + if (longitude < 0) "°W " else "°O "
        return lonDegreesString + lonMinutes + "\' " + lonSeconds +
                "\'\' / " + latDegreesString + latMinutes + "\' " +
                latSeconds + "\'\'"
    }



    override fun onResume() {
        super.onResume()
        val map = findViewById<MapView>(R.id.mapview)
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        val map = findViewById<MapView>(R.id.mapview)
        map.onPause()
    }


}