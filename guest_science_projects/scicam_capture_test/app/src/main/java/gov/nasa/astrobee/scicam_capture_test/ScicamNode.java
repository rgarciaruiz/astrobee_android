package gov.nasa.astrobee.scicam_capture_test;

import android.graphics.Bitmap;

import androidx.camera.core.ImageProxy;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.message.MessageFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import sensor_msgs.CompressedImage;
import sensor_msgs.Image;
import std_msgs.Header;

public class ScicamNode extends AbstractNodeMain {
    private Publisher<Image> pub;
    private Publisher<CompressedImage> pub_compressed;
    private MessageFactory factory;
    private ChannelBuffer channelBuffer;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("scicam_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        pub = connectedNode.newPublisher("/hw/cam_sci", Image._TYPE);
        pub_compressed = connectedNode.newPublisher("/hw/cam_sci/compressed", CompressedImage._TYPE);
        factory = connectedNode.getTopicMessageFactory();
        channelBuffer = MessageBuffers.dynamicBuffer();
    }

    public void publishImageCompressed(ImageProxy imageProxy) {
        CompressedImage image = pub_compressed.newMessage();
        Header header = factory.newFromType(Header._TYPE);
        header.setStamp(Time.fromMillis(System.currentTimeMillis()));

        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] compressedData = outputStream.toByteArray();

        channelBuffer.clear();
        channelBuffer.writeBytes(compressedData);
        image.setHeader(header);
        image.setData(channelBuffer);
        image.setFormat("jpeg");

        bitmap.recycle();
        imageProxy.close();
        pub_compressed.publish(image);
    }

    public void publishImage(ImageProxy imageProxy) {
        Image image = pub.newMessage();
        Header header = factory.newFromType(Header._TYPE);
        header.setStamp(Time.fromMillis(System.currentTimeMillis()));
        image.setHeight(imageProxy.getHeight());
        image.setWidth(imageProxy.getWidth());
        image.setEncoding("rgba8");
        image.setIsBigendian((byte) 0);
        image.setStep(imageProxy.getWidth() * 4);

        channelBuffer.clear();
        channelBuffer.writeBytes(imageProxy.getPlanes()[0].getBuffer());
        image.setData(channelBuffer);

        imageProxy.close();
        pub.publish(image);
        // Slow down rate to about 10Hz
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
