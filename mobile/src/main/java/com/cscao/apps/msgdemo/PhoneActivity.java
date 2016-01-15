package com.cscao.apps.msgdemo;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cscao.apps.mlog.MLog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

public class PhoneActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final String MESSAGE1_PATH = "/p11";
    private final String MESSAGE2_PATH = "/p22";

    private TextView mTextView;
    private View message1Button;
    private View message2Button;
    private GoogleApiClient mPhoneApiClient;

    private String mPhoneRemoteNodeId;

    private static final String MSG_CAPABILITY_NAME = "msg_capability";
    private CapabilityApi.CapabilityListener mPhoneCapabilityListener;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.cscao.apps.msgdemo.R.layout.phone_activity);
        MLog.init(this);
        handler = new Handler();

        mTextView = (TextView) findViewById(com.cscao.apps.msgdemo.R.id.textView);
        message1Button = findViewById(com.cscao.apps.msgdemo.R.id.message1Button);
        message2Button = findViewById(com.cscao.apps.msgdemo.R.id.message2Button);

        // Set message1Button onClickListener to send message 1
        message1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MLog.d("send m1", null);
                sendMsg(MESSAGE1_PATH, getString(com.cscao.apps.msgdemo.R.string.msg_info1).getBytes());
            }
        });

        // Set message2Button onClickListener to send message 2
        message2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMsg(MESSAGE2_PATH, getString(com.cscao.apps.msgdemo.R.string.msg_info2).getBytes());
            }
        });

        setupApiConnection();
    }

    private void setupApiConnection() {
        mPhoneCapabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updateMsgCapability(capabilityInfo);
                    }
                };

        // Create GoogleApiClient
        mPhoneApiClient = new GoogleApiClient.Builder(getApplicationContext()).addConnectionCallbacks(this).addApi(Wearable.API).build();

        Wearable.CapabilityApi.addCapabilityListener(mPhoneApiClient, mPhoneCapabilityListener, MSG_CAPABILITY_NAME);

    }

    private void updateMsgCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        mPhoneRemoteNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void sendMsg(final String path, byte[] msg) {
        Wearable.MessageApi.sendMessage(mPhoneApiClient, mPhoneRemoteNodeId, path, msg).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if (sendMessageResult.getStatus().isSuccess())
                    Toast.makeText(getApplication(), getString(com.cscao.apps.msgdemo.R.string.message_sent) + path, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getApplication(), getString(com.cscao.apps.msgdemo.R.string.error_message) + path, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(MESSAGE1_PATH)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String display = getString(com.cscao.apps.msgdemo.R.string.received_message1) + "\n" + new String(messageEvent.getData());
                    MLog.d("display is:" + display);

                    mTextView.setText(display);
                }
            });
        } else if (messageEvent.getPath().equals(MESSAGE2_PATH)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String display = getString(com.cscao.apps.msgdemo.R.string.received_message2) + "\n" + new String(messageEvent.getData());
                    MLog.d("display is:" + display);

                    mTextView.setText(display);
                }
            });
        }
    }

    // from GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle bundle) {
        MLog.d("onConnected");
        // Register Message listeners
        Wearable.MessageApi.addListener(mPhoneApiClient, this);

//        // If there is a connected node, get it's id that is used when sending messages
//        Wearable.CapabilityApi.getCapability(mPhoneApiClient,MSG_CAPABILITY_NAME, CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallbacks<CapabilityApi.GetCapabilityResult>() {
//            @Override
//            public void onSuccess(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
//                updateMsgCapability(getCapabilityResult.getCapability());
//                if (mPhoneRemoteNodeId != null) {
//                    message1Button.setEnabled(true);
//                    message2Button.setEnabled(true);
//                }
//                MLog.d("result callback success");
//            }
//
//            @Override
//            public void onFailure(@NonNull Status status) {
//                MLog.d("result callback failed");
//            }
//        });

        Wearable.NodeApi.getConnectedNodes(mPhoneApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                if (getConnectedNodesResult.getStatus().isSuccess() && getConnectedNodesResult.getNodes().size() > 0) {
                    mPhoneRemoteNodeId = getConnectedNodesResult.getNodes().get(0).getId();
                    message1Button.setEnabled(true);
                    message2Button.setEnabled(true);
                }
            }
        });
    }

    //from GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnectionSuspended(int i) {
        MLog.d("onConnectionSuspended");
        message1Button.setEnabled(false);
        message2Button.setEnabled(false);
    }

    // from GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        MLog.d("onConnectionFailed");
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE)
            Toast.makeText(getApplicationContext(), getString(com.cscao.apps.msgdemo.R.string.wearable_api_unavailable), Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        MLog.d("onResume");
        int connectionResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());

        if (connectionResult != ConnectionResult.SUCCESS) {
            // Google Play Services is NOT available. Show appropriate error dialog
            GoogleApiAvailability.getInstance().showErrorDialogFragment(getParent(), connectionResult, 0, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
        } else {
            MLog.d("connect mPhoneApiClient");
            mPhoneApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        MLog.d("onPause");
        // Unregister Node and Message listeners, disconnect GoogleApiClient and disable buttons
        Wearable.MessageApi.removeListener(mPhoneApiClient, this);
        Wearable.CapabilityApi.removeCapabilityListener(mPhoneApiClient, mPhoneCapabilityListener, MSG_CAPABILITY_NAME);

        mPhoneApiClient.disconnect();

        message1Button.setEnabled(false);
        message2Button.setEnabled(false);

        super.onPause();
    }

}
