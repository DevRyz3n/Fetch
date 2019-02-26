package dev.ryz3n.cloudmusicfetch

import android.Manifest
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
                .setDownloadConcurrentLimit(4)
                .setHttpDownloader(OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL))
                .setNamespace(FETCH_NAMESPACE)
                .setNotificationManager(DefaultFetchNotificationManager(this))
                .build()
        fetch = Fetch.getInstance(fetchConfiguration)
        checkStoragePermissions()
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
            enqueueDownloads()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownloads()
        } else {
            Snackbar.make(cl_root, R.string.permission_not_enabled, Snackbar.LENGTH_INDEFINITE).show()
        }
    }

    private fun enqueueDownloads() {
        val requests = Data.getFetchRequestWithGroupId(GROUP_ID)
        fetch.enqueue(requests, Func {  })

    }

    override fun onResume() {
        super.onResume()
        fetch.getDownloadsInGroup(GROUP_ID, Func {
            val list = ArrayList<Download>(it)
            list.sortWith(Comparator { first, second -> java.lang.Long.compare(first.created, second.created) })
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

        override fun onCompleted(download: Download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            super.onError(download, error, throwable)
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND)
        }

        override fun onProgress(download: Download, etaInMilliseconds: Long, downloadedBytesPerSecond: Long) {
            fileAdapter.update(download, etaInMilliseconds, downloadedBytesPerSecond)
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
