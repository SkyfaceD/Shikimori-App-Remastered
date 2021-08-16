package com.gnoemes.shikimori.utils

import com.gnoemes.shikimori.entity.series.domain.PlayerType
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

//TODO Update docs
fun handleTrackUrls(
    videoHosting: VideoHosting,
    url: String,
    playerType: PlayerType = PlayerType.EMBEDDED
): String? {
    return when (videoHosting) {
        is VideoHosting.SIBNET -> handleSibnetEmbeddedUrl(playerType, url)
        is VideoHosting.MYVI -> handleMyviEmbeddedUrl(url)
        else -> null
    }
}

// ╭────────────╮
// │   Sibnet   │
// ╰────────────╯

/**
 * Read html from passed [stringUrl], extract valid video url and return it.
 *
 * @return
 * entry url if [stringUrl] host not sibnet or if status code not [HttpURLConnection.HTTP_OK],
 * null if one of methods [readRawHtml], [extractSibnetVideoUrl], [handleSibnetRedirect] returns null,
 * valid url otherwise
 */
@Suppress("UsePropertyAccessSyntax", "UnnecessaryVariable")
fun handleSibnetEmbeddedUrl(playerType: PlayerType, stringUrl: String): String? = runBlocking {
    return@runBlocking withContext(Dispatchers.IO) {
        val url = URL(stringUrl)

        if (url.host.contains("video.sibnet.ru") && playerType == PlayerType.EMBEDDED) return@withContext stringUrl

        return@withContext if (url.host.contains("video.sibnet.ru")) {
            handleSibnetRedirect(stringUrl)
        } else {
            val connection: HttpsURLConnection =
                (url.openConnection() as HttpsURLConnection).apply {
                    setConnectTimeout(15_000)
                    setReadTimeout(15_000)
                    disconnect()
                }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) return@withContext stringUrl

            val rawHtml = connection.inputStream.use { readRawHtml(it) } ?: return@withContext null
            val extractedVideoUrl =
                extractSibnetVideoUrl(rawHtml) ?: return@withContext null

            handleSibnetRedirect(extractedVideoUrl)
        }
    }
}

private fun extractSibnetVideoUrl(rawHtml: String): String? {
    val document = Jsoup.parseBodyFragment(rawHtml)
    val scripts = document.getElementsByTag("script")
    val scriptWithSrc = scripts.map(Element::data)
        .find { it.contains("player.src") } ?: return null
    val startWord = "player.src([{src: \""
    val path = scriptWithSrc.substring(scriptWithSrc.indexOf(startWord) + startWord.length,
        scriptWithSrc.indexOf("\", type: "))

    //FIXME Hardcoded domain
    return "https://video.sibnet.ru$path"
}

/**
 * Connect to passed [stringUrl] and return valid url if redirect found.
 *
 * @return
 * entry url if [stringUrl] host not sibnet.ru or if status code [HttpURLConnection.HTTP_OK],
 * null if location header not found or status code not [HttpURLConnection.HTTP_MOVED_TEMP],
 * valid url otherwise
 */
@Suppress("UsePropertyAccessSyntax", "UnnecessaryVariable")
private suspend fun handleSibnetRedirect(stringUrl: String): String? = withContext(Dispatchers.IO) {
    val url = URL(stringUrl)

    val connection: HttpsURLConnection = (url.openConnection() as HttpsURLConnection).apply {
        setConnectTimeout(15_000)
        setReadTimeout(15_000)
        setInstanceFollowRedirects(false)
        addRequestProperty("Referer", "https://video.sibnet.ru/")
        disconnect()
    }

    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) return@withContext stringUrl
    if (responseCode != HttpURLConnection.HTTP_MOVED_TEMP) return@withContext null

    val location = connection.getHeaderField("Location")
    //FIXME Hardcoded protocol
    val videoUrl = if (location.startsWith("//")) "https:".plus(location) else location

    return@withContext handleSibnetRedirect(videoUrl)
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
