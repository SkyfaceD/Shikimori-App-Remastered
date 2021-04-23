package com.gnoemes.shikimori.utils

import com.gnoemes.shikimori.BuildConfig
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import javax.net.ssl.HttpsURLConnection

fun handleTrackUrls(videoHosting: VideoHosting, url: String): String? {
    return when (videoHosting) {
        is VideoHosting.SIBNET -> handleSibnetRedirect(url)
        is VideoHosting.MYVI -> handleMyviEmbeddedUrl(url)
        else -> null
    }
}

// ╭────────────╮
// │   SIBNET   │
// ╰────────────╯

/**
 * Connect to passed [stringUrl] and return valid url if redirect found.
 *
 * @return
 * entry url if [stringUrl] host not sibnet.ru or if status code [HttpURLConnection.HTTP_OK],
 * null if location header not found or status code not [HttpURLConnection.HTTP_MOVED_TEMP],
 * valid url otherwise
 */
@Suppress("UsePropertyAccessSyntax", "UnnecessaryVariable")
fun handleSibnetRedirect(stringUrl: String): String? = runBlocking {
    return@runBlocking withContext(Dispatchers.IO) {
        val url = URL(stringUrl)
        if (!url.host.contains("sibnet.ru")) return@withContext stringUrl

        val connection: HttpsURLConnection = (url.openConnection() as HttpsURLConnection).apply {
            setConnectTimeout(15_000)
            setReadTimeout(15_000)
            setInstanceFollowRedirects(false)
            addRequestProperty("Referer", BuildConfig.VideoBaseUrl)
            disconnect()
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) return@withContext stringUrl
        if (responseCode != HttpURLConnection.HTTP_MOVED_TEMP) return@withContext null

        val videoUrl = connection.getHeaderField("Location")

        return@withContext videoUrl
    }
}

// ╭──────────╮
// │   MYVI   │
// ╰──────────╯

/**
 * Read html from passed [stringUrl], extract stream url and return valid url if redirect found.
 * Works with myvi.tv and myvi.top
 *
 * @return
 * entry url if [stringUrl] host not myvi or if status code not [HttpURLConnection.HTTP_OK],
 * null if one of methods [readRawHtml], [extractVideoUrlFromRawHtml], [handleMyviRedirect] returns null,
 * valid url otherwise
 */
@Suppress("UsePropertyAccessSyntax", "UnnecessaryVariable")
fun handleMyviEmbeddedUrl(stringUrl: String): String? = runBlocking {
    return@runBlocking withContext(Dispatchers.IO) {
        val url = URL(stringUrl)
        if (!url.host.contains("myvi")) return@withContext stringUrl

        val connection: HttpsURLConnection = (url.openConnection() as HttpsURLConnection).apply {
            setConnectTimeout(15_000)
            setReadTimeout(15_000)
            addRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36"
            )
            disconnect()
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) return@withContext stringUrl

        val rawHtml = connection.inputStream.use { readRawHtml(it) } ?: return@withContext null
        val extractedVideoUrl = extractVideoUrlFromRawHtml(rawHtml) ?: return@withContext null
        val videoUrl = handleMyviRedirect(extractedVideoUrl) ?: return@withContext null

        return@withContext videoUrl
    }
}

private fun readRawHtml(inputStream: InputStream): String? {
    val reader = BufferedReader(InputStreamReader(inputStream))
    val sb = StringBuilder()
    reader.useLines { sequence ->
        for (line in sequence.iterator()) {
            if (line.isBlank()) continue
            sb.append(line)
        }
    }
    if (sb.isBlank()) return null
    return sb.toString()
}

private fun extractVideoUrlFromRawHtml(rawHtml: String): String? {
    val document = Jsoup.parseBodyFragment(rawHtml)
    val scripts = document.getElementsByTag("script")
    val stringUrl = scripts.map(Element::data)
            .find { it.contains("PlayerLoader.CreatePlayer") }
            ?.substringAfter("PlayerLoader.CreatePlayer(\"v=")
            ?.replaceAfterLast("\");", "")
            ?.split("(?=https)".toRegex())
            ?.find { it.contains("stream") } ?: return null
    val url = URLDecoder.decode(stringUrl, "UTF-8")
            .substringBefore("\\u0026tp=", "")
    return if (url.startsWith("//")) "https:".plus(url) else url
}

/**
 * Connect to passed [stringUrl] and return valid url if redirect found.
 *
 * @return
 * entry url if status code [HttpURLConnection.HTTP_OK],
 * null if location header not found or status code not [HttpURLConnection.HTTP_MOVED_TEMP],
 * valid url otherwise
 */
@Suppress("UsePropertyAccessSyntax", "UnnecessaryVariable")
private suspend fun handleMyviRedirect(stringUrl: String): String? = withContext(Dispatchers.IO) {
    val url = URL(stringUrl)

    val connection: HttpsURLConnection = (url.openConnection() as HttpsURLConnection).apply {
        setConnectTimeout(15_000)
        setReadTimeout(15_000)
        setInstanceFollowRedirects(false)
        addRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36"
        )
        disconnect()
    }

    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) return@withContext stringUrl
    if (responseCode != HttpURLConnection.HTTP_MOVED_TEMP) return@withContext null

    val videoUrl = connection.getHeaderField("location")

    return@withContext videoUrl
}
