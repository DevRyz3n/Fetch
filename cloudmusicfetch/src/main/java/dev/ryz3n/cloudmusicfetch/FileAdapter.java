package dev.ryz3n.cloudmusicfetch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Status;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    @NonNull
    private final List<DownloadData> downloads = new ArrayList<>();
    @NonNull
    private final ActionListener actionListener;

    FileAdapter(@NonNull final ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.actionButton.setOnClickListener(null);
        holder.actionButton.setEnabled(true);

        final DownloadData downloadData = downloads.get(position);
        String url = "";
        if (downloadData.download != null) {
            url = downloadData.download.getUrl();
        }
        final Uri uri = Uri.parse(url);
        final Status status = downloadData.download.getStatus();
        final Context context = holder.itemView.getContext();

        holder.titleTextView.setText(uri.getLastPathSegment());

        String s = downloadData.download.getFile();
        holder.titleTextView.setText(s.replace("/storage/emulated/0/Download/cloudmusic/", "")
                .replace("/storage/emulated/1/Download/cloudmusic", "")
                .replace(".mp3","")
                .replace('／','/'));

        holder.pathTextView.setText(("\uD83D\uDCF2  ")+s.replace("/storage/emulated/", ""));

        holder.statusTextView.setText(getStatusString(context, status));

        int progress = downloadData.download.getProgress();
        if (progress == -1) { // Download progress is undermined at the moment.
            progress = 0;
        }
        holder.progressBar.setProgress(progress);
        holder.progressTextView.setText(context.getString(R.string.percent_progress, progress));

        if (downloadData.eta == -1) {
            holder.timeRemainingTextView.setText("");
        } else {
            holder.timeRemainingTextView.setText(Utils.getETAString(context, downloadData.eta));
        }

        if (downloadData.downloadedBytesPerSecond == 0) {
            holder.downloadedBytesPerSecondTextView.setText("");
        } else {
            holder.downloadedBytesPerSecondTextView.setText(Utils.getDownloadSpeedString(context, downloadData.downloadedBytesPerSecond));
        }

        switch (status) {
            case COMPLETED: {
                holder.actionButton.setText(R.string.view);
                holder.actionButton.setOnClickListener(view -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        //Toast.makeText(context, "Downloaded Path:" + downloadData.download.getFile(), Toast.LENGTH_LONG).show();
                        //return;
                        try {
                            Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                            m.invoke(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    final File file = new File(downloadData.download.getFile());
                    final Uri uri1 = Uri.fromFile(file);
                    final Intent share = new Intent(Intent.ACTION_VIEW);
                    share.setDataAndType(uri1, "audio/*");
                    context.startActivity(share);
                });
                break;
            }
            case FAILED: {
                holder.actionButton.setText(R.string.retry);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onRetryDownload(downloadData.download.getId());
                });
                break;
            }
            case PAUSED: {
                holder.actionButton.setText(R.string.resume);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onResumeDownload(downloadData.download.getId());
                });
                break;
            }
            case DOWNLOADING:
            case QUEUED: {
                holder.actionButton.setText(R.string.pause);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onPauseDownload(downloadData.download.getId());
                });
                break;
            }
            case ADDED: {
                holder.actionButton.setText(R.string.download);
                holder.actionButton.setOnClickListener(view -> {
                    holder.actionButton.setEnabled(false);
                    actionListener.onResumeDownload(downloadData.download.getId());
                });
                break;
            }
            default: {
                break;
            }
        }

        //Set delete action
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.delete_title, downloadData.download.getFile()))
                    .setPositiveButton(R.string.delete, (dialog, which) -> actionListener.onRemoveDownload(downloadData.download.getId()))
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            return true;
        });

    }

    public void addDownload(@NonNull final Download download) {
        boolean found = false;
        DownloadData data = null;
        int dataPosition = -1;
        for (int i = 0; i < downloads.size(); i++) {
            final DownloadData downloadData = downloads.get(i);
            if (downloadData.id == download.getId()) {
                data = downloadData;
                dataPosition = i;
                found = true;
                break;
            }
        }
        if (!found) {
            final DownloadData downloadData = new DownloadData();
            downloadData.id = download.getId();
            downloadData.download = download;
            downloads.add(0, downloadData);
            notifyItemInserted(downloads.size() - 1);
        } else {
            data.download = download;
            notifyItemChanged(dataPosition);
        }
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    public void update(@NonNull final Download download, long eta, long downloadedBytesPerSecond) {
        for (int position = 0; position < downloads.size(); position++) {
            final DownloadData downloadData = downloads.get(position);
            if (downloadData.id == download.getId()) {
                switch (download.getStatus()) {
                    case REMOVED:
                    case DELETED: {
                        downloads.remove(position);
                        notifyItemRemoved(position);
                        break;
                    }
                    default: {
                        downloadData.download = download;
                        downloadData.eta = eta;
                        downloadData.downloadedBytesPerSecond = downloadedBytesPerSecond;
                        notifyItemChanged(position);
                    }
                }
                return;
            }
        }
    }

    private String getStatusString(Context context, Status status) {
        switch (status) {
            case COMPLETED:
                return context.getString(R.string.download_status_done);
            case DOWNLOADING:
                return context.getString(R.string.download_status_downloading);
            case FAILED:
                return context.getString(R.string.download_status_failed);
            case PAUSED:
                return context.getString(R.string.download_status_paused);
            case QUEUED:
                return context.getString(R.string.download_status_queued);
            case REMOVED:
                return context.getString(R.string.download_status_removed);
            case NONE:
                return context.getString(R.string.download_status_none);
            default:
                return context.getString(R.string.download_status_default);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView titleTextView;
        final TextView pathTextView;
        final TextView statusTextView;
        public final ProgressBar progressBar;
        public final TextView progressTextView;
        public final Button actionButton;
        final TextView timeRemainingTextView;
        final TextView downloadedBytesPerSecondTextView;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            pathTextView = itemView.findViewById(R.id.pathTextView);
            statusTextView = itemView.findViewById(R.id.status_TextView);
            progressBar = itemView.findViewById(R.id.progressBar);
            actionButton = itemView.findViewById(R.id.actionButton);
            progressTextView = itemView.findViewById(R.id.progress_TextView);
            timeRemainingTextView = itemView.findViewById(R.id.remaining_TextView);
            downloadedBytesPerSecondTextView = itemView.findViewById(R.id.downloadSpeedTextView);
        }

    }

    public static class DownloadData {
        public int id;
        @Nullable
        public Download download;
        long eta = -1;
        long downloadedBytesPerSecond = 0;

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            if (download == null) {
                return "";
            }
            return download.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof DownloadData && ((DownloadData) obj).id == id;
        }
    }

}
