package nu.peg.ubnt.edgemax.util

import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

object KotlinResourceUtil {

    fun getResourceUrl(path: String): URL {
        return javaClass.classLoader.getResource(path)
    }

    fun getResourcePath(path: String): Path {
        return Paths.get(getResourceUrl(path).toURI())
    }

    fun getResourceStream(path: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(path)
    }
}