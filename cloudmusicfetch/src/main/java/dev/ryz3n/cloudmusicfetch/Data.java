package dev.ryz3n.cloudmusicfetch;

import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import java.util.ArrayList;
import java.util.List;


public final class Data {

    /*static ArrayList<String> musicIDs = new ArrayList<>();

    static ArrayList<String> musicNames = new ArrayList<>();

    static ArrayList<String> musicProducers = new ArrayList<>();*/

    public static String DL_URL_BASE = "http://music.163.com/song/media/outer/url?id=";

    private Data() {}

    @NonNull
    public static Request getFetchRequest(String url, String musicName, String musicProducer, String musicID) {
        return new Request(url, getFilePath(musicProducer + " - " + musicName + " - " + musicID));
    }

    /*@NonNull
    public static List<Request> getFetchRequestWithGroupId(final int groupId) {
        final List<Request> requests = getFetchRequests();
        for (Request request : requests) {
            request.setGroupId(groupId);
        }
        return requests;
    }*/

    @NonNull
    private static String getFilePath(@NonNull final String musicTitle) {
        final String dir = getSaveDir();
        return (dir + musicTitle + ".mp3");
    }

    @NonNull
    static String getNameFromUrl(final String url) {
        return Uri.parse(url).getLastPathSegment();
    }


    @NonNull
    public static String getSaveDir() {
        return Environment.getExternalStorageDirectory().toString() + "/Download/163MusicFetcher/";
    }

}
