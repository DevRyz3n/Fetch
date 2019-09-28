package dev.ryz3n.cloudmusicfetch

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object Network {

    private val gson: Gson = Gson()

    private const val LYRIC_URL_BASE = "http://music.163.com/api/song/media?id="

    fun getRealDownloadUrl(musicID: String): String {
        val originalUrl = Data.DL_URL_BASE + musicID
        val firstCon = URL(originalUrl).openConnection() as HttpURLConnection
        firstCon.instanceFollowRedirects = false
        val secondCon = URL(firstCon.getHeaderField("Location")).openConnection() as HttpURLConnection
        val realUrl = secondCon.url.toString()

        Log.i("fukin1st Url", firstCon.url.toString())
        Log.i("fukin2nd Url", secondCon.url.toString())
        firstCon.disconnect()
        secondCon.disconnect()
        return realUrl
    }

    fun getLyric(musicID: String): String {
        val jsonText = URL("$LYRIC_URL_BASE$musicID").readText()
        val lyric = gson.fromJson<Lyric>(jsonText, object : TypeToken<Lyric>() {}.type)
        return if (lyric.code == 200) {
            lyric.lyric
        } else {
            ""
        }
    }

    // http://music.163.com/api/song/detail/?id=541499418&ids=[541499418]
    fun getAlbumDetail(musicID: String): AlbumDetail {
        val jsonText = URL(getAlbumDetailUrl(musicID)).readText()
        val album = gson.fromJson<Album>(jsonText, object : TypeToken<Album>() {}.type)
        if (album.code == 200) {
            val albumX = album.songs[0].album
            val albumBlurPicUrl = URL(albumX.blurPicUrl)
            val conn = albumBlurPicUrl.openConnection()

            // picture input stream to byte array
            val inputStream = conn.getInputStream()
            val outputStream = ByteArrayOutputStream()

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            val blurPicByteArr = outputStream.toByteArray()

            // publish year
            val simpleDateFormat = SimpleDateFormat("yyyy")
            val lt = albumX.publishTime / 1000
            val date = Date(lt)
            val publishYear = simpleDateFormat.format(date)


            return AlbumDetail(albumX.name, albumX.company, publishYear, blurPicByteArr)

        } else {
            return AlbumDetail("","","", byteArrayOf())
        }
    }

    private fun getAlbumDetailUrl(musicID: String) = "http://music.163.com/api/song/detail/?id=$musicID&ids=[$musicID]"
}
