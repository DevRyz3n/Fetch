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


    private Data() {

    }


    @NonNull
    public static Request getFetchRequest(String musicID, String musicName, String musicProducer) {
        String DL_URL_BASE = "http://music.163.com/song/media/outer/url?id=";
        return new Request(DL_URL_BASE + musicID, getFilePath(musicProducer + " - " + musicName));
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
    public static List<Request> getGameUpdates() {
        final List<Request> requests = new ArrayList<>();
        final String url = "http://speedtest.ftp.otenet.gr/files/test100k.db";
        for (int i = 0; i < 10; i++) {
            final String filePath = getSaveDir() + "/gameAssets/" + "asset_" + i + ".asset";
            final Request request = new Request(url, filePath);
            request.setPriority(Priority.HIGH);
            requests.add(request);
        }
        return requests;
    }

    @NonNull
    public static String getSaveDir() {
        return Environment.getExternalStorageDirectory().toString() + "/netease/cloudmusic/";
    }

}
