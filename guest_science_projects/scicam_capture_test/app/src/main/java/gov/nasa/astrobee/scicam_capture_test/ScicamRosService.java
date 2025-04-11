package gov.nasa.astrobee.scicam_capture_test;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.camera2.CaptureRequest;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.camera2.interop.Camera2CameraControl;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.ros.android.RosService;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ScicamRosService extends RosService {
    // Texts for the notification
    private static final String NOTIFICATION_TITLE = "SciCam Publisher";
    private static final String NOTIFICATION_TICKER = "SciCam Publisher Service";

    private static final String TAG = "CameraXApp";

    // IP Address ROS Master and Hostname
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String ROS_HOSTNAME = "hlp";

    private Executor cameraExecutor = Executors.newSingleThreadExecutor();
    private ScicamNode scicamNode;
    private boolean isNodeRunning = false;

    private static void putOptExtra(Intent intent, String key, String value) {
        if (intent.hasExtra(key))
            return;
        intent.putExtra(key, value);
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    @Override
    public void onCreate() {
        super.onCreate();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (Exception e) {
                Log.e(TAG, "Failed to get CameraProvider", e);
                return;
            }
            ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setDefaultResolution(new Size(1280, 960))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            imageAnalyzer.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy imageProxy) {
                    if(isNodeRunning) {
                        //scicamNode.publishImage(imageProxy);
                        scicamNode.publishImageCompressed(imageProxy);
                    }
                }
            });

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            try {
                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        (LifecycleOwner) this, cameraSelector, imageAnalyzer);

                Camera2CameraControl camera2control = Camera2CameraControl.from(camera.getCameraControl());
                CaptureRequestOptions options = new CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                        .build();
                camera2control.setCaptureRequestOptions(options);
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }

        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!ACTION_START.equals(intent.getAction())) {
            intent.setAction(ACTION_START);
        }

        putOptExtra(intent, EXTRA_NOTIFICATION_TICKER, NOTIFICATION_TICKER);
        putOptExtra(intent, EXTRA_NOTIFICATION_TITLE, NOTIFICATION_TITLE);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(ROS_HOSTNAME);
        nodeConfiguration.setMasterUri(ROS_MASTER_URI);
        scicamNode = new ScicamNode();
        nodeMainExecutor.execute(scicamNode, nodeConfiguration);
        isNodeRunning = true;

    }
}