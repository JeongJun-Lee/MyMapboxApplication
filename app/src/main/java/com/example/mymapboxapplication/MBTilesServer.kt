import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.pow

/**
 * Created by pham on 2018/1/7.
 */

object MBTilesServer : Runnable {

    private var serverSocket: ServerSocket? = null
    var isRunning = false
    val sources: MutableMap<String, MBTilesSource> = mutableMapOf()
    const val port = 8888

    fun start() {
        isRunning = true
        Thread(this).start()
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }

    override fun run() {
        try {
            serverSocket = ServerSocket(port)
            while (isRunning) {
                val socket = serverSocket?.accept() ?: throw Error()
                Log.d(javaClass.simpleName, "request handled start")
                handle(socket)
                socket.close()
                Log.d(javaClass.simpleName, "request handled in while")
            }
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, e.localizedMessage)
        } finally {
            Log.d(javaClass.simpleName, "request handled")
        }
    }

    @Throws
    private fun handle(socket: Socket) {
        var reader: BufferedReader? = null
        var output: PrintStream? = null

        try {
            var route: String? = null
            reader = socket.getInputStream().reader().buffered()

            // Read HTTP headers and parse out the route.
            do {
                val line = reader.readLine() ?: ""
                if (line.startsWith("GET")) {
                    // the format for route should be {source}/{z}/{x}/{y}
                    route = line.substringAfter("GET /").substringBefore(".")
                    break
                }
            } while (!line.isEmpty())

            // the source which this request target to
            val source = sources[route?.substringBefore("/")] ?: return

            // Output stream that we send the response to
            output = PrintStream(socket.getOutputStream())

            // Prepare the content to send.
            if (null == route) {
                writeServerError(output)
                return
            }

            val bytes = loadContent(source, route) ?: run {
                writeServerError(output)
                return
            }

            // Send out the content.
            output.apply {
                println("HTTP/1.0 200 OK")
                println("Content-Type: " + detectMimeType(source.format))
                println("Content-Length: " + bytes.size)
                if (source.isVector) println("Content-Encoding: gzip")
                println()
                write(bytes)
                flush()
            }
        } finally {
            if (null != output) output.close()
            reader?.close()
        }
    }

    @Throws
    private fun loadContent(source: MBTilesSource, route: String): ByteArray? {
        val (z, x, y) = route.split("/").subList(1, 4).map { it.toInt() }

        try {
            val output = ByteArrayOutputStream()
            val content = source.getTile(z, x, (2.0.pow(z)).toInt() - 1 - y) ?: return null
            output.write(content)
            output.flush()
            return output.toByteArray()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
    }

    private fun writeServerError(output: PrintStream) {
        output.println("HTTP/1.0 500 Internal Server Error")
        output.flush()
    }

    private fun detectMimeType(format: String): String? = when (format) {
        "jpg" -> "image/jpeg"
        "png" -> "image/png"
        "mvt" -> "application/x-protobuf"
        "pbf" -> "application/x-protobuf"
        else -> "application/octet-stream"
    }
}