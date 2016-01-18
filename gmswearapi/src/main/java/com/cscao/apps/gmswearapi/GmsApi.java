package com.cscao.apps.gmswearapi;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.cscao.apps.mlog.MLog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

/**
 * Created by qqcao on 1/16/16.
 *
 */

public class GmsApi implements DataApi.DataListener,MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mApiClient;

    private CapabilityApi.CapabilityListener mCapabilityListener;
    private String mCapability;

    private OnMessageReceivedListener onMessageReceivedListener;

    private Set<Node> mNodes;

    private Context mContext;
    private Handler mHandler;

    public GmsApi(Context context, Handler handler, String capability) {

        if (context == null) {
            throw new NullPointerException("context must be nonnull");
        }

        mContext = context;
        mHandler = handler;
        mCapability = capability;

        MLog.init(context);

        // Create GoogleApiClient
        mApiClient = new GoogleApiClient
                .Builder(context)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();

        mCapabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        mNodes = capabilityInfo.getNodes();
                    }
                };

        //register Capability listeners
        Wearable.CapabilityApi
                .addCapabilityListener(mApiClient, mCapabilityListener, capability);

        // Register Message listeners
        Wearable.MessageApi.addListener(mApiClient, this);

    }


    public void connect() {
        MLog.d("connect");
        int connectionResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext);

        if (connectionResult != ConnectionResult.SUCCESS) {
            String errorString = null;
            // Google Play Services is not working properly
            switch (connectionResult) {
                case ConnectionResult.API_UNAVAILABLE:
                    errorString = mContext.getString(R.string.common_google_play_services_api_unavailable_text);

                    break;
                case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                    errorString = mContext.getString(R.string.common_google_play_services_wear_update_text);
                    break;
                default:
                    MLog.d("other connection result code");
            }

            // show error toast
            MLog.d(errorString);
            showToast(errorString,Toast.LENGTH_SHORT);

        } else {
            if (!mApiClient.isConnecting() && !mApiClient.isConnected()) {
                mApiClient.connect();
            } else {
                MLog.d("already connected or trying to connect");
            }

        }
    }

    public void disconnect() {
        MLog.d("disconnect");
        Wearable.MessageApi.removeListener(mApiClient, this);
        Wearable.CapabilityApi.removeCapabilityListener(mApiClient, mCapabilityListener, mCapability);
        if (mApiClient.isConnected() || mApiClient.isConnecting()) {
            mApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        MLog.d("onConnected");
        Wearable.CapabilityApi.getCapability(
                mApiClient, mCapability,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(@NonNull CapabilityApi.GetCapabilityResult result) {
                        if (result.getStatus().isSuccess()) {
                            mNodes = result.getCapability().getNodes();
                            MLog.d(mNodes.toString());
                        } else {
                            MLog.d("Failed to get capabilities, "
                                    + "status: "
                                    + result.getStatus().getStatusMessage());
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        MLog.d("onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        MLog.d("onConnectionFailed");
        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE)
            showToast(mContext.getString(R.string.common_google_play_services_api_unavailable_text),Toast.LENGTH_SHORT);
    }

    public void showToast(final String notifyText, final int length ) {
        if (mHandler == null) {
            MLog.w("mHandler null!");
        } else if (notifyText != null) {
            final Context context = mContext;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, notifyText, length).show();
                }
            });
        }

    }

    private void sendMsgToNode(String node, final String path, byte[] msg) {
        Wearable.MessageApi.sendMessage(mApiClient, node, path, msg).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if (sendMessageResult.getStatus().isSuccess()) {
                    MLog.d("Message sent through:" + path);
                } else {
                    showToast(sendMessageResult.getStatus().getStatusMessage(),Toast.LENGTH_SHORT);
                    MLog.d("Message sent error from:" + path);
                }
            }
        });
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

    public void sendMsg(String path, byte[] msg) {
        sendMsg(path, msg, false);
    }

    public void sendMsg(String path, byte[] msg, boolean isSentToAllNodes) {
        if (isSentToAllNodes) {
            for (Node node : mNodes) {
                sendMsgToNode(node.getId(), path, msg);
            }
        } else {
            sendMsgToNode(pickBestNodeId(mNodes), path, msg);
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        onMessageReceivedListener.onMessageReceived(new MsgEnt() {
            @Override
            public int getRequestId() {
                return messageEvent.getRequestId();
            }

            @Override
            public String getPath() {
                return messageEvent.getPath();
            }

            @Override
            public byte[] getData() {
                return messageEvent.getData();
            }

            @Override
            public String getSourceNodeId() {
                return messageEvent.getSourceNodeId();
            }
        });
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(MsgEnt messageEvent);
    }

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        onMessageReceivedListener=listener;
    }

    public interface MsgEnt extends MessageEvent {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }


}
