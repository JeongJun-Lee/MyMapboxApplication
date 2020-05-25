import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.MapRowParser
import org.jetbrains.anko.db.select

/**
 * Created by pham on 2018/1/7.
 */

sealed class MBTilesSourceError : Error() {
    class CouldNotReadFileError : MBTilesSourceError()
    class UnsupportedFormatError : MBTilesSourceError()
}

object MetadataParser : MapRowParser<Pair<String, String>> {
    override fun parseRow(columns: Map<String, Any?>): Pair<String, String> =
        columns["name"] as String to columns["value"] as String
}

object TilesParser : MapRowParser<ByteArray> {
    override fun parseRow(columns: Map<String, Any?>): ByteArray = columns["tile_data"] as ByteArray
}

class MBTilesSource(private val context: Context, filePath: String, id: String? = null) {

    var id = id ?: filePath.substringAfterLast("/").substringBefore(".")
    val url get() = "http://localhost:${MBTilesServer.port}/$id/{z}/{x}/{y}.$format"
    private val db: SQLiteDatabase = try {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(filePath).path, null)
    } catch (e: RuntimeException) {
        throw MBTilesSourceError.CouldNotReadFileError()
    }

    var isVector = false
    var format = String()
//    var tileSize: Int? = null
//    var layersJson: String? = ""
//    var attributions: String? = ""
//    var minZoom: Float? = null
//    var maxZoom: Float? = null
//    var bounds: LatLngBounds? = null

    init {
        try {
            format = db.select("metadata")
                .whereSimple("name = ?", "format")
                .parseSingle(MetadataParser).second

            isVector = when (format) {
                in validVectorFormats -> true
                in validRasterFormats -> false
                else -> throw MBTilesSourceError.UnsupportedFormatError()
            }

        } catch (error: MBTilesSourceError) {
            print(error.localizedMessage)
        }
    }

    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        return db.select("tiles")
            .whereArgs("(zoom_level = {z}) and (tile_column = {x}) and (tile_row = {y})",
                "z" to z, "x" to x, "y" to y)
            .parseList(TilesParser)
            .run { if (!isEmpty()) get(0) else null }
    }

    fun activate() {
        val source = this
        MBTilesServer.apply {
            sources[source.id] = source
            if (!isRunning) start()
        }
    }

    fun deactivate() {
        val source = this
        MBTilesServer.apply {
            sources.remove(source.id)
            if (isRunning && sources.isEmpty()) stop()
        }
    }

    companion object {
        val validRasterFormats = listOf("jpg", "png")
        val validVectorFormats = listOf("pbf", "mvt")
    }
}