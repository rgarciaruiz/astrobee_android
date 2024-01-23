package gov.nasa.arc.astrobee.tensorflowexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sensor_msgs.CompressedImage;


public class DetectionNode extends AbstractNodeMain {
    private static final String TAG = DetectionNode.class.getSimpleName();

    private final String dockCamDataPath;
    private final Context context;
    private final Paint paint;

    private ObjectDetector objectDetector;
    private ImageProcessor imageProcessor;
    private boolean saveImages;
    private boolean processImages;

    public DetectionNode(Context context, String dataPath) {
        this.dockCamDataPath = dataPath + "/delayed/dock_images";
        this.context = context;
        this.saveImages = true;
        this.processImages = false;

        File directory = new File(dockCamDataPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
    }

    public void enableImageSaving(boolean enable) {
        saveImages = enable;
    }

    public void enableImageProcessing(boolean enable) {
        processImages = enable;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("tensorflow_dockcam");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        BaseOptions baseOptionsBuilder = BaseOptions.builder().setNumThreads(4).build();
        ObjectDetector.ObjectDetectorOptions optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.5f)
                .setMaxResults(3).setBaseOptions(baseOptionsBuilder).build();

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(context, "model.tflite", optionsBuilder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        imageProcessor = new ImageProcessor.Builder().build();

        Subscriber<CompressedImage> dockCamSub = connectedNode.newSubscriber(
                "/mgt/img_sampler/dock_cam/image_record/compressed", CompressedImage._TYPE);
        dockCamSub.addMessageListener(new MessageListener<CompressedImage>() {
            @Override
            public void onNewMessage(CompressedImage image) {
                // Images are mono8 compressed to JPEG
                if (!processImages) {
                    return;
                }
                // Image to bitmap
                ChannelBuffer buffer = image.getData();
                byte[] data = buffer.array();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, buffer.arrayOffset(),
                        buffer.readableBytes());

                // Bitmap to TensorFlow
                TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap));

                // Processing
                List<Detection> results = objectDetector.detect(tensorImage);
                if(saveImages) {
                    processResults(results, bitmap);
                } else {
                    processResults(results);
                }
            }
        });
    }

    @Override
    public void onShutdown(Node node) {
        if (!objectDetector.isClosed()) {
            objectDetector.close();
        }
    }

    public void processResults(List<Detection> detections) {
        for (Detection detection : detections) {
            Category category = detection.getCategories().get(0);
            RectF box = detection.getBoundingBox();
            Log.i(TAG, String.format("Detected: %s [%s]: %s, %s",
                    category.getLabel(), category.getScore(), box.centerX(), box.centerY()));
        }
    }

    public void processResults(List<Detection> detections, Bitmap bitmap) {
        // Making bitmap mutable
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        // Loading canvas
        Canvas canvas = new Canvas(bitmap);

        for (Detection detection : detections) {
            Category category = detection.getCategories().get(0);
            RectF box = detection.getBoundingBox();
            Log.i(TAG, String.format("Detected: %s [%s]: %s, %s",
                    category.getLabel(), category.getScore(), box.centerX(), box.centerY()));
            canvas.drawRect(box.left, box.top, box.right, box.bottom, paint);
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS",
                Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timestamp + ".jpg";
        File imageFile = new File(dockCamDataPath, imageFileName);
        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Log.i(TAG, "Image saved to: " + imageFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
