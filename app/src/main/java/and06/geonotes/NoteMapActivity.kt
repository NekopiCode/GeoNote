package and06.geonotes


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

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
        val marker = Marker(map)
        marker.position =  GeoPoint(37.421, -122.099)
        marker.title = "Mein Standort"
        marker.snippet = "Dies ist ein Infotext"
        map.overlays.add(marker)

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