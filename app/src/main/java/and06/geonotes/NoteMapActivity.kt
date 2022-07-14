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
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)

        val extras = intent.extras
        if (extras == null) return
        val location = extras.getParcelable<Location>(GatherActivity.LOCATION)
        if (location == null) return
        val marker = Marker(map)
        val markerIcon = getDrawable(R.drawable.crosshair)
        marker.icon = markerIcon
        marker.rotation
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.position = GeoPoint(location.latitude, location.longitude)
        marker.title = extras.getString(GatherActivity.TITLE)
        marker.snippet = extras.getString(GatherActivity.SNIPPET)
        map.overlays.add(marker)
        val controller = map.controller
        controller.setCenter(marker.position)
        controller.setZoom(16.0)
        //map.setMapOrientation(45.0f)


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