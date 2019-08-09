package dev.ryz3n.cloudmusicfetch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import kotlinx.android.synthetic.main.activity_main.*
import android.media.MediaScannerConnection
import android.util.Log
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File

import java.io.File
import java.lang.Exception


class MainActivity : AppCompatActivity(), ActionListener {

    companion object {
        private const val STORAGE_PERMISSION_CODE = 200
        private const val UNKNOWN_REMAINING_TIME: Long = -1
        private const val UNKNOWN_DOWNLOADED_BYTES_PER_SECOND: Long = 0
        private val GROUP_ID = "listGroup".hashCode()
        const val FETCH_NAMESPACE = "MainActivity"
    }

    lateinit var fetch: Fetch

    lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpViews()
        val fetchConfiguration = FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(5)
                .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.SEQUENTIAL))
                .setNamespace(FETCH_NAMESPACE)
                .setNotificationManager(DefaultFetchNotificationManager(this))
                .build()
        fetch = Fetch.getInstance(fetchConfiguration)
        checkStoragePermissions()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        when {
            getIntent()?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == getIntent().type) {
                    handleSendText(getIntent())
                }
            }
        }

        fileAdapter.notifyDataSetChanged()

    }

    private fun isShareText(shareText: String): Boolean {
        return if (shareText.startsWith("分享") and shareText.endsWith("(来自@网易云音乐)")) {
            true
        } else {
            Snackbar.make(cl_root, ("\u274c ") + applicationContext.getString(R.string.share_text_error), Snackbar.LENGTH_LONG).show()
            false
        }
    }

    private fun receiveShareMusic() {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                }
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { shareText ->

            if (isShareText(shareText)) {

                // 分割格式化后的内容，list中0、1、2、3下标分别为 制作者、歌曲名、歌曲ID、分享者ID
                val musicInfo = shareText.shareFormat().split("--", "/?userid=")
                if (musicInfo.size != 4) {
                    Snackbar.make(cl_root, ("\u274c ") + applicationContext.getString(R.string.share_text_is_mv_error), Snackbar.LENGTH_LONG).show()
                    return
                }

                enqueueDownload(musicInfo[2].trim(), musicInfo[1].trim().musicNameFormat(), musicInfo[0].trim().musicNameFormat())
            }
        }
    }

    private fun setUpViews() {
        rv_list.layoutManager = LinearLayoutManager(this)
        fileAdapter = FileAdapter(this)
        rv_list.adapter = fileAdapter
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        } else {
            receiveShareMusic()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            receiveShareMusic()
        } else {
            checkStoragePermissions()
            Snackbar.make(cl_root, R.string.permission_not_enabled, Snackbar.LENGTH_INDEFINITE).show()
        }
    }

    private fun enqueueDownload(musicID: String, musicName: String, musicProducer: String) {
        val request = Data.getFetchRequest(musicID, musicName, musicProducer)
        fetch.enqueue(request, Func { })
    }

    override fun onResume() {
        super.onResume()
        fetch.getDownloads(Func {
            val list = ArrayList(it)
            list.sortWith(Comparator { first, second -> first.created.compareTo(second.created) })
            for (download in list) {
                fileAdapter.addDownload(download)
            }
        }).addListener(fetchListener)
    }

    override fun onPause() {
        super.onPause()
        fetch.removeListener(fetchListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        fetch.close()
    }

    private val fetchListener = object : AbstractFetchListener() {
        override fun onAdded(download: Download) {
            fileAdapter.addDownload(download)
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        @SuppressLint("LogNotTimber")
        override fun onCompleted(download: Download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)

            // add id3tag to mp3 file
            try {
                val downloadFilePath = download.file
                val mp3File = Mp3File(downloadFilePath)
                val mp3v2Tag = ID3v24Tag()
                val artistsAndMusicName = downloadFilePath.replace(Data.getSaveDir(), "").replace(".mp3", "").split(" - ")
                mp3v2Tag.artist = artistsAndMusicName.first().replace('_', '/')
                mp3v2Tag.title = artistsAndMusicName.last()
                mp3File.id3v1Tag = mp3v2Tag
                mp3File.id3v2Tag = mp3v2Tag

                val modifiedFilePath = downloadFilePath.replace(".mp3", "_NCM.mp3")
                mp3File.save(modifiedFilePath)
                // refresh media store
                MediaScannerConnection.scanFile(applicationContext, arrayOf(modifiedFilePath), arrayOf("audio/*")) { _, _ -> }

                // delete original file
                val file = File(downloadFilePath)
                file.delete()
                if (file.exists()) {
                    file.canonicalFile.delete()
                    if (file.exists()) {
                        applicationContext.deleteFile(file.name)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }




            Log.i("TEST", "file: ${download.file}, fileUri: ${download.fileUri}, url:${download.url}")
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            super.onError(download, error, throwable)
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            fileAdapter.update(download, etaInMilliSeconds, downloadedBytesPerSecond)
        }

        override fun onPaused(download: Download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onResumed(download: Download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onCancelled(download: Download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onRemoved(download: Download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onDeleted(download: Download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }
    }

    override fun onPauseDownload(id: Int) {
        fetch.pause(id)
    }

    override fun onResumeDownload(id: Int) {
        fetch.resume(id)
    }

    override fun onRemoveDownload(id: Int) {
        fetch.remove(id)
    }

    override fun onRetryDownload(id: Int) {
        fetch.retry(id)
    }
}



fun String.shareFormat() = this.trim().substring(2, this.length)
        .replace("的单曲《", "--")
        .replace("》:", "--")
        .replace("http://music.163.com/song/", "")
        .replace("https://music.163.com/song/", "")
        .replace(" (来自@网易云音乐)", "")

fun String.musicNameFormat() = this.replace('?', '？')
        .replace('/', '_')
        .replace('\\', '＼')
        .replace(':', '：')
        .replace('*', '﹡')
        .replace('\"', '”')
        .replace('<', '＜')
        .replace('>', '＞')
        .replace('|', '︱')