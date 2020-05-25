package com.example.mymapboxapplication

import DBHelper
import MBTilesSource
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

class MainActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    lateinit var map: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, "---Input your key---");
        Mapbox.setConnected(true);
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)

        mapView?.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                // Map is set up and the style has loaded. Now you can add data or make other map adjustments

                // Create a MBTilesSource to read existing Mbtiles file
                val path = "seoul.mbtiles" // file path
                DBHelper(this, path) // Load mbtiles from file

                val sourceId = "ID" // Nullable, used in Mapbox Source URL
                val mbSource = try {
                    MBTilesSource(this, path, sourceId).apply { activate() }
                } catch (e: MBTilesSourceError.CouldNotReadFileError) {
                    Log.e("-----", "Database Read Error", e)
                }
                mapboxMap.setStyle(Style.Builder().fromUri("asset://style-bright.json"));
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView?.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}
