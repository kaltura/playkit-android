package com.kaltura.playkit.prefetch;

import android.app.Notification;
import android.content.Context;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloadService;
import com.kaltura.android.exoplayer2.scheduler.PlatformScheduler;
import com.kaltura.android.exoplayer2.ui.DownloadNotificationHelper;
import com.kaltura.android.exoplayer2.ui.DownloadNotificationUtil;
import com.kaltura.android.exoplayer2.util.NotificationUtil;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.R;

import java.util.List;

/** A service for downloading media. */
public class DemoDownloadService extends DownloadService {

  private static final PKLog log = PKLog.get("DemoDownloadService");

  public static final String DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel";
  private static final int JOB_ID = 1;
  private static final int FOREGROUND_NOTIFICATION_ID = 1;

  //public static final int DATA_TO_SAVA_BYTES = 2 * 1024 * 1024;
  public static final int DATA_TO_SAVA_BYTES = 1 * 1024 * 500;

  public DemoDownloadService() {
    super(
            FOREGROUND_NOTIFICATION_ID,
            DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            R.string.exo_download_notification_channel_name,
            /* channelDescriptionResourceId= */ 0);
  }


  @Override
  protected DownloadManager getDownloadManager() {
    return PrefetchSdk.shared(this).getDownloadManager();
  }

  @Override
  protected PlatformScheduler getScheduler() {
    return Util.SDK_INT >= 21 ? new PlatformScheduler(this, JOB_ID) : null;
  }

  @Override
  protected Notification getForegroundNotification(List<Download> downloads) {
    log.d("XXX downloadedBytes = " + downloads.get(0).getBytesDownloaded());
    log.d("XXX downloadPercentage = " + downloads.get(0).getPercentDownloaded());
    if ( downloads.get(0).getBytesDownloaded() > DATA_TO_SAVA_BYTES) {
      getDownloadManager().pauseDownloads();
    }
    return DownloadNotificationUtil.buildProgressNotification(
            /* context= */ this,
            R.drawable.exo_controls_play,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            /* contentIntent= */ null,
            /* message= */ null,
            downloads);
  }

//  @Override
//  protected Notification getForegroundNotification(List<Download> downloads) {
//    return ((DemoApplication) getApplication())
//            .getDownloadNotificationHelper()
//            .buildProgressNotification(
//                    R.drawable.ic_download, /* contentIntent= */ null, /* message= */ null, downloads);
//  }



//  @Override
//  protected Notification getForegroundNotification(TaskState[] taskStates) {
//    Log.d(TAG, "XXX downloadedBytes = " + taskStates[0].downloadedBytes);
//    Log.d(TAG, "XXX downloadPercentage = " + taskStates[0].downloadPercentage);
//    if (taskStates[0].downloadedBytes > DATA_TO_SAVA_BYTES) {
//      getDownloadManager().stopDownloads();
//    }
//    return DownloadNotificationUtil.buildProgressNotification(
//            /* context= */ this,
//            R.drawable.exo_controls_play,
//            CHANNEL_ID,
//            /* contentIntent= */ null,
//            /* message= */ null,
//            taskStates);
//  }
//
//  @Override
//  protected void onTaskStateChanged(TaskState taskState) {
//    if (taskState.action.isRemoveAction) {
//      return;
//    }
//    Notification notification = null;
//    if (taskState.state == TaskState.STATE_COMPLETED) {
//      notification =
//              DownloadNotificationUtil.buildDownloadCompletedNotification(
//                      /* context= */ this,
//                      R.drawable.exo_controls_play,
//                      CHANNEL_ID,
//                      /* contentIntent= */ null,
//                      Util.fromUtf8Bytes(taskState.action.data));
//    } else if (taskState.state == TaskState.STATE_FAILED) {
//      notification =
//              DownloadNotificationUtil.buildDownloadFailedNotification(
//                      /* context= */ this,
//                      R.drawable.exo_controls_play,
//                      CHANNEL_ID,
//                      /* contentIntent= */ null,
//                      Util.fromUtf8Bytes(taskState.action.data));
//    }
//    int notificationId = FOREGROUND_NOTIFICATION_ID + 1 + taskState.taskId;
//    Log.d(TAG, "XXX notificationId = " + notificationId);
//    NotificationUtil.setNotification(this, notificationId, notification);
//  }

  /**
   * Creates and displays notifications for downloads when they complete or fail.
   *
   * <p>This helper will outlive the lifespan of a single instance of {@link DemoDownloadService}.
   * It is static to avoid leaking the first {@link DemoDownloadService} instance.
   */
  private static final class TerminalStateNotificationHelper implements DownloadManager.Listener {

    private final Context context;
    private final DownloadNotificationHelper notificationHelper;

    private int nextNotificationId;

    public TerminalStateNotificationHelper(
            Context context, DownloadNotificationHelper notificationHelper, int firstNotificationId) {
      this.context = context.getApplicationContext();
      this.notificationHelper = notificationHelper;
      nextNotificationId = firstNotificationId;
    }

    @Override
    public void onDownloadChanged(DownloadManager manager, Download download) {
      Notification notification;
      if (download.state == Download.STATE_COMPLETED) {
        notification =
                notificationHelper.buildDownloadCompletedNotification(
                       R.drawable.exo_icon_shuffle_on,
                        /* contentIntent= */ null,
                        Util.fromUtf8Bytes(download.request.data));
      } else if (download.state == Download.STATE_FAILED) {
        notification =
                notificationHelper.buildDownloadFailedNotification(
                        R.drawable.exo_icon_shuffle_off,
                        /* contentIntent= */ null,
                        Util.fromUtf8Bytes(download.request.data));
      } else {
        return;
      }
      NotificationUtil.setNotification(context, nextNotificationId++, notification);
    }
  }
}

//
//
//import android.app.Notification;
//import android.util.Log;
//
//import com.kaltura.android.exoplayer2.offline.DownloadManager;
//
//import com.kaltura.android.exoplayer2.offline.DownloadService;
//import com.kaltura.android.exoplayer2.scheduler.PlatformScheduler;
//import com.kaltura.android.exoplayer2.ui.DownloadNotificationUtil;
//import com.kaltura.android.exoplayer2.util.NotificationUtil;
//import com.kaltura.android.exoplayer2.util.Util;
//
///** A service for downloading media. */
//public class DemoDownloadService extends DownloadService {
//  private static final String TAG = DemoDownloadService.class.getSimpleName();
//
//  private static final String CHANNEL_ID = "download_channel";
//  private static final int JOB_ID = 1;
//  private static final int FOREGROUND_NOTIFICATION_ID = 1;
//  public static final int DATA_TO_SAVA_BYTES = 1 * 1024 * 500;
//
//  public DemoDownloadService() {
//    super(
//        R.string.exo_download_notification_channel_name);
//  }
//
//  @Override
//  protected DownloadManager getDownloadManager() {
//    return PrefetchSdk.shared(this).getDownloadManager();
//  }
//
//  @Override
//  protected PlatformScheduler getScheduler() {
//    return Util.SDK_INT >= 21 ? new PlatformScheduler(this, JOB_ID) : null;
//  }
//
//  @Override
//  protected Notification getForegroundNotification(TaskState[] taskStates) {
//    Log.d(TAG, "XXX downloadedBytes = " + taskStates[0].downloadedBytes);
//    Log.d(TAG, "XXX downloadPercentage = " + taskStates[0].downloadPercentage);
//    if (taskStates[0].downloadedBytes > DATA_TO_SAVA_BYTES) {
//      getDownloadManager().stopDownloads();
//    }
//    return DownloadNotificationUtil.buildProgressNotification(
//        /* context= */ this,
//        R.drawable.exo_controls_play,
//        CHANNEL_ID,
//        /* contentIntent= */ null,
//        /* message= */ null,
//        taskStates);
//  }
//
//  @Override
//  protected void onTaskStateChanged(TaskState taskState) {
//    if (taskState.action.isRemoveAction) {
//      return;
//    }
//    Notification notification = null;
//    if (taskState.state == TaskState.STATE_COMPLETED) {
//      notification =
//          DownloadNotificationUtil.buildDownloadCompletedNotification(
//              /* context= */ this,
//              R.drawable.exo_controls_play,
//              CHANNEL_ID,
//              /* contentIntent= */ null,
//              Util.fromUtf8Bytes(taskState.action.data));
//    } else if (taskState.state == TaskState.STATE_FAILED) {
//      notification =
//          DownloadNotificationUtil.buildDownloadFailedNotification(
//              /* context= */ this,
//              R.drawable.exo_controls_play,
//              CHANNEL_ID,
//              /* contentIntent= */ null,
//              Util.fromUtf8Bytes(taskState.action.data));
//    }
//    int notificationId = FOREGROUND_NOTIFICATION_ID + 1 + taskState.taskId;
//    Log.d(TAG, "XXX notificationId = " + notificationId);
//    NotificationUtil.setNotification(this, notificationId, notification);
//  }
//}


