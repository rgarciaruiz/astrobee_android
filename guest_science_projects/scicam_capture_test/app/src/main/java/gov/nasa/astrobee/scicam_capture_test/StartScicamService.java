package gov.nasa.astrobee.scicam_capture_test;

import android.content.Intent;
import android.os.IBinder;

import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

public class StartScicamService extends StartGuestScienceService {
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onGuestScienceCustomCmd(String s) {

    }

    @Override
    public void onGuestScienceStart() {
        startService(new Intent(this, ScicamRosService.class));
        sendStarted("info");
    }

    @Override
    public void onGuestScienceStop() {
        stopService(new Intent(this, ScicamRosService.class));
        sendStopped("info");
        terminate();
    }
}