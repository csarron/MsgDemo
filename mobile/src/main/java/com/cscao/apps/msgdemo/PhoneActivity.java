package com.cscao.apps.msgdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cscao.apps.gmswearapi.GmsApi;
import com.cscao.apps.mlog.MLog;

public class PhoneActivity extends Activity
{

    private final String MESSAGE1_PATH = "/p11";
    private final String MESSAGE2_PATH = "/p22";

    private TextView mTextView;
    private View message1Button;
    private View message2Button;

    private static final String MSG_CAPABILITY_NAME = "msg_capability";
    private Handler handler;

    private GmsApi gmsApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_activity);
        MLog.init(this);
        handler = new Handler();

        gmsApi = new GmsApi(this, handler, MSG_CAPABILITY_NAME);

        gmsApi.setOnMessageReceivedListener(new GmsApi.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(final GmsApi.MsgEnt messageEvent) {

                if (messageEvent.getPath().equals(MESSAGE1_PATH)) {

                    updateUI(messageEvent, getString(R.string.received_message1));

                } else if (messageEvent.getPath().equals(MESSAGE2_PATH)) {

                    updateUI(messageEvent, getString(R.string.received_message2));
                }
            }
        });

        mTextView = (TextView) findViewById(com.cscao.apps.msgdemo.R.id.textView);
        message1Button = findViewById(R.id.message1Button);
        message2Button = findViewById(R.id.message2Button);

        // Set message1Button onClickListener to send message 1
        message1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MLog.d("send m1", null);
                gmsApi.sendMsg(MESSAGE1_PATH, getString(R.string.msg_info1).getBytes());
                gmsApi.showToast(getString(R.string.message_sent) + MESSAGE1_PATH, Toast.LENGTH_SHORT);
            }
        });

        // Set message2Button onClickListener to send message 2
        message2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gmsApi.sendMsg(MESSAGE2_PATH, getString(R.string.msg_info2).getBytes());
                gmsApi.showToast(getString(R.string.message_sent) + MESSAGE2_PATH, Toast.LENGTH_SHORT);
            }
        });

    }

    private void updateUI(final GmsApi.MsgEnt messageEvent, final String uiText) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String display = uiText + "\n" + new String(messageEvent.getData());
                MLog.d("display is:" + display);
                mTextView.setText(display);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MLog.d("onResume");

        gmsApi.connect();
        message1Button.setEnabled(true);
        message2Button.setEnabled(true);
    }

    @Override
    protected void onPause() {
        MLog.d("onPause");

        gmsApi.disconnect();
        message1Button.setEnabled(false);
        message2Button.setEnabled(false);

        super.onPause();
    }

}
