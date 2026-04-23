package app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MeshService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        // TODO: start mesh networking foreground service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: handle service commands
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
