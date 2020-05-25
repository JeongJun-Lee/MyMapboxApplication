import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File
import java.io.FileOutputStream

// Database version number, you need to change it in case of any schema change.
const val dbVersionNumber = 1

class DBHelper(private val context: Context, private val dbName: String) : SQLiteOpenHelper(context, dbName, null, dbVersionNumber) {

    private var dataBase: SQLiteDatabase? = null

    init {
        // Check if the database already copied to the device.
        val dbExist = checkDatabase()
        if (dbExist) {
            // if already copied then don't do anything.
            Log.e("-----", "Database exist")
        } else {
            // else copy the database to the device.
            Log.e("-----", "Database doesn't exist")
            createDatabase()
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // if you want to do anything after the database created
        // like inserting default values you can do it here.
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // if you want to do anything after the database upgraded
        // like migrating values you can do it here.
    }

    // Copy the database
    private fun createDatabase() {
        copyDatabase()
    }

    // Check if the database already copied to the device.
    private fun checkDatabase(): Boolean {
        val dbFile = File(context.getDatabasePath(dbName).path)
        return dbFile.exists()
    }

    // Copy the database
    private fun copyDatabase() {

        val inputStream = context.assets.open("$dbName")

        val outputFile = File(context.getDatabasePath(dbName).path)
        val outputStream = FileOutputStream(outputFile)

        val bytesCopied = inputStream.copyTo(outputStream)
        Log.e("bytesCopied", "$bytesCopied")
        inputStream.close()

        outputStream.flush()
        outputStream.close()
    }

    // Open the database with read and write access mode.
    private fun openDatabase() {
        dataBase = SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
    }

    // Close the database.
    override fun close() {
        dataBase?.close()
        super.close()
    }
}