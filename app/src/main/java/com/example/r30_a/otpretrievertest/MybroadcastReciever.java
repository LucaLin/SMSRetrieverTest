package com.example.r30_a.otpretrievertest;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.CommonStatusCodes;

/**
 * Created by luca refer to developers on 2018/12/26.
 */

public class MybroadcastReciever extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        if(SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())){
            Bundle bundle = intent.getExtras();
            Status status =(Status)bundle.get(SmsRetriever.EXTRA_STATUS);

            switch (status.getStatusCode()){
                case CommonStatusCodes.SUCCESS:
                    //接收server寄來的msg
                    String message = (String) bundle.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                    myBroadcastRecieverListener.onRecieved(message);
                    break;

                case CommonStatusCodes.TIMEOUT:
                    myBroadcastRecieverListener.onTimeOut();
                    break;
            }
        }
    }
    public static MyBroadcastRecieverListener myBroadcastRecieverListener;

    public interface MyBroadcastRecieverListener{
        void onRecieved(String msg);
        void onTimeOut();
    }
    public static void setOnMyBroadcaseRecieverListener(MyBroadcastRecieverListener listener){
        myBroadcastRecieverListener = listener;
    }
}
