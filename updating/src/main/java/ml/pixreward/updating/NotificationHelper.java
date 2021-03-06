package ml.pixreward.updating;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class NotificationHelper extends ContextWrapper {

    private NotificationManager manager;

    private static String CHANNEL_ID = "app_update";

    private static final int NOTIFICATION_ID = 0;

    public NotificationHelper(Context base) {
        super(base);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "Atualização de aplicativo", NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription("O aplicativo tem uma nova versão");
            mChannel.enableLights(true);
            getManager().createNotificationChannel(mChannel);
        }
    }

    public void showNotification(String content, String apkUrl) {

        Intent myIntent = new Intent(this, DownloadService.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myIntent.putExtra(Constants.APK_DOWNLOAD_URL, apkUrl);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = getNofity(content)
                .setContentIntent(pendingIntent);

        getManager().notify(NOTIFICATION_ID, builder.build());
    }


    public void updateProgress(int progress) {


        String text = this.getString(R.string.downloading, progress);

        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = getNofity(text)
                .setProgress(100, progress, false)
                .setContentIntent(pendingintent);

        getManager().notify(NOTIFICATION_ID, builder.build());
    }

    private NotificationCompat.Builder getNofity(String text) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setTicker(getString(R.string.new_update_click_to_update))
			.setContentTitle("Atualização de aplicativo")
                .setContentText(text)
                .setSmallIcon(getSmallIcon())
                .setLargeIcon(getLargeIcon())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    }

    public void cancel() {
        getManager().cancel(NOTIFICATION_ID);
    }


    private int getSmallIcon() {
        int icon = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
        if (icon == 0) {
            icon = getApplicationInfo().icon;
        }

        return icon;
    }

    private Bitmap getLargeIcon() {
        int bigIcon = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
        if (bigIcon != 0) {
            return BitmapFactory.decodeResource(getResources(), bigIcon);
        }
        return null;
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}
