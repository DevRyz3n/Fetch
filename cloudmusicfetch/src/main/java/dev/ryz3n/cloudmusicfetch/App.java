package dev.ryz3n.cloudmusicfetch;

import android.app.Application;

import com.tonyodev.dispatchandroid.DispatchQueueAndroid;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.HttpUrlConnectionDownloader;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;
import com.tonyodev.fetch2rx.RxFetch;

import okhttp3.OkHttpClient;
import timber.log.Timber;

import static com.tonyodev.dispatchandroid.DispatchQueueAndroid.initAndroidDispatchQueues;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DispatchQueueAndroid.initAndroidDispatchQueues();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .enableRetryOnNetworkGain(true)
                .setDownloadConcurrentLimit(4)
                .setHttpDownloader(new HttpUrlConnectionDownloader(Downloader.FileDownloaderType.SEQUENTIAL))
                // OR
                //.setHttpDownloader(getOkHttpDownloader())
                .build();
        Fetch.Impl.setDefaultInstanceConfiguration(fetchConfiguration);
        RxFetch.Impl.setDefaultRxInstanceConfiguration(fetchConfiguration);
    }

    private OkHttpDownloader getOkHttpDownloader() {
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        return new OkHttpDownloader(okHttpClient,
                Downloader.FileDownloaderType.SEQUENTIAL);
    }

}
