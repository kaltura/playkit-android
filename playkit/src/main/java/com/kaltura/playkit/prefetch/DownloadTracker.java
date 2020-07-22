package com.kaltura.playkit.prefetch;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.RenderersFactory;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadCursor;
import com.kaltura.android.exoplayer2.offline.DownloadHelper;
import com.kaltura.android.exoplayer2.offline.DownloadIndex;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloadRequest;
import com.kaltura.android.exoplayer2.offline.DownloadService;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.PKLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.concurrent.CopyOnWriteArraySet;

/** Tracks media that has been downloaded. */
public class DownloadTracker {

  private static final PKLog log = PKLog.get("DownloadTracker");

  /** Listens for changes in the tracked downloads. */
  public interface Listener {

    /** Called when the tracked downloads changed. */
    void onDownloadsChanged();
  }

  private final Context context;
  private final DataSource.Factory dataSourceFactory;
  private final CopyOnWriteArraySet<Listener> listeners;
  private final HashMap<Uri, Download> downloads;
  private final DownloadIndex downloadIndex;
  private final DefaultTrackSelector.Parameters trackSelectorParameters;

  @Nullable private StartDownloadHelper startDownloadHelper;

  public DownloadTracker(
          Context context, DataSource.Factory dataSourceFactory, DownloadManager downloadManager) {
    this.context = context.getApplicationContext();
    this.dataSourceFactory = dataSourceFactory;
    listeners = new CopyOnWriteArraySet<>();
    downloads = new HashMap<>();
    downloadIndex = downloadManager.getDownloadIndex();
    trackSelectorParameters = DownloadHelper.getDefaultTrackSelectorParameters(context);
    downloadManager.addListener(new DownloadManagerListener());
    loadDownloads();
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public boolean isDownloaded(Uri uri) {
    Download download = downloads.get(uri);
    return download != null && download.state != Download.STATE_FAILED;
  }

  public DownloadRequest getDownloadRequest(Uri uri) {
    Download download = downloads.get(uri);
    return download != null && download.state != Download.STATE_FAILED ? download.request : null;
  }

  public void toggleDownload(
          String name,
          Uri uri,
          String extension,
          RenderersFactory renderersFactory) {
    Download download = downloads.get(uri);
    if (download != null) {
      DownloadService.sendRemoveDownload(
              context, DemoDownloadService.class, download.request.id, /* foreground= */ false);
    } else {
      if (startDownloadHelper != null) {
        startDownloadHelper.release();
      }
      startDownloadHelper =
              new StartDownloadHelper(getDownloadHelper(uri, extension, renderersFactory), name);
    }
  }

  private void loadDownloads() {
    try (DownloadCursor loadedDownloads = downloadIndex.getDownloads()) {
      while (loadedDownloads.moveToNext()) {
        Download download = loadedDownloads.getDownload();
        downloads.put(download.request.uri, download);
      }
    } catch (IOException e) {
      log.w("Failed to query downloads", e);
    }
  }

  private DownloadHelper getDownloadHelper(
          Uri uri, String extension, RenderersFactory renderersFactory) {
    int type = Util.inferContentType(uri, extension);
    switch (type) {
      case C.TYPE_DASH:
        return DownloadHelper.forDash(context, uri, dataSourceFactory, renderersFactory);
      case C.TYPE_SS:
        return DownloadHelper.forSmoothStreaming(context, uri, dataSourceFactory, renderersFactory);
      case C.TYPE_HLS:
        return DownloadHelper.forHls(context, uri, dataSourceFactory, renderersFactory);
      case C.TYPE_OTHER:
        return DownloadHelper.forProgressive(context, uri);
      default:
        throw new IllegalStateException("Unsupported type: " + type);
    }
  }

  private class DownloadManagerListener implements DownloadManager.Listener {

    @Override
    public void onDownloadChanged(DownloadManager downloadManager, Download download) {
      downloads.put(download.request.uri, download);
      for (Listener listener : listeners) {
        listener.onDownloadsChanged();
      }
    }

    @Override
    public void onDownloadRemoved(DownloadManager downloadManager, Download download) {
      downloads.remove(download.request.uri);
      for (Listener listener : listeners) {
        listener.onDownloadsChanged();
      }
    }
  }

  private final class StartDownloadHelper
          implements DownloadHelper.Callback {

    private final DownloadHelper downloadHelper;
    private final String name;

    private MappedTrackInfo mappedTrackInfo;

    public StartDownloadHelper(DownloadHelper downloadHelper, String name) {
      this.downloadHelper = downloadHelper;
      this.name = name;
      downloadHelper.prepare(this);
    }

    public void release() {
      downloadHelper.release();
    }

    // DownloadHelper.Callback implementation.

    @Override
    public void onPrepared(DownloadHelper helper) {
      if (helper.getPeriodCount() == 0) {
        log.d("No periods found. Downloading entire stream.");
        startDownload();
        downloadHelper.release();
        return;
      }
      mappedTrackInfo = downloadHelper.getMappedTrackInfo(/* periodIndex= */ 0);
      if (!willHaveContent(mappedTrackInfo)) {
        log.d("No dialog content. Downloading entire stream.");
        startDownload();
        downloadHelper.release();
        return;
      }
      for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
        downloadHelper.clearTrackSelections(periodIndex);
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            downloadHelper.addTrackSelectionForSingleRenderer(
                    periodIndex,
                    /* rendererIndex= */ i,
                    trackSelectorParameters,
                    new ArrayList<>());
        }
      }
      DownloadRequest downloadRequest = buildDownloadRequest();
      if (downloadRequest.streamKeys.isEmpty()) {
        // All tracks were deselected in the dialog. Don't start the download.
        return;
      }
      startDownload(downloadRequest);
    }

    @Override
    public void onPrepareError(DownloadHelper helper, IOException e) {
      log.e(
              e instanceof DownloadHelper.LiveContentUnsupportedException
                      ? "Downloading live content unsupported"
                      : "Failed to start download",
              e);
    }

    /**
     * Returns whether a track selection dialog will have content to display if initialized with the
     * specified {@link DefaultTrackSelector} in its current state.
     */
    public  boolean willHaveContent(DefaultTrackSelector trackSelector) {
      MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
      return mappedTrackInfo != null && willHaveContent(mappedTrackInfo);
    }

    /**
     * Returns whether a track selection dialog will have content to display if initialized with the
     * specified {@link MappedTrackInfo}.
     */
    public  boolean willHaveContent(MappedTrackInfo mappedTrackInfo) {
      for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
        if (showTabForRenderer(mappedTrackInfo, i)) {
          return true;
        }
      }
      return false;
    }

    private boolean showTabForRenderer(MappedTrackInfo mappedTrackInfo, int rendererIndex) {
      TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
      if (trackGroupArray.length == 0) {
        return false;
      }
      int trackType = mappedTrackInfo.getRendererType(rendererIndex);
      return isSupportedTrackType(trackType);
    }

    private  boolean isSupportedTrackType(int trackType) {
      switch (trackType) {
        case C.TRACK_TYPE_VIDEO:
        case C.TRACK_TYPE_AUDIO:
        case C.TRACK_TYPE_TEXT:
          return true;
        default:
          return false;
      }
    }
    // Internal methods.

//    public void startDownload(Map<String, String> entries) {
//      if (entries.isEmpty()) {
//        return;
//      }
//      final Set<Map.Entry<String, String>> set = entries.entrySet();
//      final Map.Entry<String, String> first = set.iterator().next();
//      startDownload(first.getKey(), Uri.parse(first.getValue()));
//    }


    public void startDownload() {
      startDownload(buildDownloadRequest());
    }

    private void startDownload(DownloadRequest downloadRequest) {
      DownloadService.sendAddDownload(
              context, DemoDownloadService.class, downloadRequest, /* foreground= */ false);
    }

    private DownloadRequest buildDownloadRequest() {
      return downloadHelper.getDownloadRequest(Util.getUtf8Bytes(name));
    }
  }
}
