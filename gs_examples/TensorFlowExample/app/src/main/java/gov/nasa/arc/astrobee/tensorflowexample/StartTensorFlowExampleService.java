package gov.nasa.arc.astrobee.tensorflowexample;

import org.json.JSONException;
import org.json.JSONObject;
import org.ros.address.InetAddressFactory;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class StartTensorFlowExampleService extends StartGuestScienceService {
    private static final String TAG = StartTensorFlowExampleService.class.getSimpleName();
    private NodeConfiguration mNodeConfiguration;
    private NodeMainExecutor mNodeMainExecutor;
    private DetectionNode mDetectionNode;

    /**
     * This function is called when the GS manager starts your apk.
     * Put all of your start up code in here.
     */
    @Override
    public void onGuestScienceStart() {
        try {
            URI masterURI = new URI("http://llp:11311");
            mNodeConfiguration = NodeConfiguration
                    .newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            mNodeConfiguration.setMasterUri(masterURI);
            mNodeMainExecutor = DefaultNodeMainExecutor.newDefault();
            mDetectionNode = new DetectionNode(getApplicationContext(), getGuestScienceDataBasePath());
            mNodeMainExecutor.execute(mDetectionNode, mNodeConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Inform the GS Manager and the GDS that the app has been started.
        sendStarted("info");
    }

    /**
     * This function is called when the GS manager stops your apk.
     * Put all of your clean up code in here. You should also call the terminate helper function
     * at the very end of this function.
     */
    @Override
    public void onGuestScienceStop() {
        mNodeMainExecutor.shutdownNodeMain(mDetectionNode);
        mNodeMainExecutor.shutdown();
        mNodeConfiguration = null;
        mNodeMainExecutor = null;
        mDetectionNode = null;

        stopSelf();
        sendStopped("info");
        terminate();
    }

    /**
     * This function is called when the GS manager sends a custom command to your apk.
     * Please handle your commands in this function.
     *
     * @param command
     */
    @Override
    public void onGuestScienceCustomCmd(String command) {
        /* Inform the Guest Science Manager (GSM) and the Ground Data System (GDS)
         * that this app received a command. */
        sendReceivedCustomCommand("info");

        try {
            JSONObject jCommand = new JSONObject(command);
            JSONObject jResult = new JSONObject();

            String sCommand = jCommand.getString("name");

            switch (sCommand) {
                case "enable_detection":
                    mDetectionNode.enableImageProcessing(true);
                    jResult.put("Summary", "Detection ENABLED");
                    break;
                case "disable_detection":
                    mDetectionNode.enableImageProcessing(false);
                    jResult.put("Summary", "Detection DISABLED");
                    break;
                case "enable_image_saving":
                    mDetectionNode.enableImageSaving(true);
                    jResult.put("Summary", "Saving images ENABLED");
                    break;
                case "disable_image_saving":
                    mDetectionNode.enableImageSaving(false);
                    jResult.put("Summary", "Saving images DISABLED");
                    break;
                default:
                    sendData(MessageType.JSON, "data", "ERROR: Unrecognized command");
                    return;
            }

            sendData(MessageType.JSON, "data", jResult.toString());

        } catch (JSONException e) {
            sendData(MessageType.JSON, "data", "ERROR parsing JSON");
        } catch (Exception ex) {
            sendData(MessageType.JSON, "data", "Unrecognized ERROR");
        }
    }
}
