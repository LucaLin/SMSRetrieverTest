package com.example.r30_a.otpretrievertest;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
                                                                GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient googleApiClient;
    private static final int RESOLVE_HINT = 50005;
    TextView txvNumber;
    Button btnsend;
    ArrayList<String> appSignatures;
    MybroadcastReciever mybroadcastReciever;
    Task<Void> task;
    String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        startSMSListenr();

    }

    private void startSMSListenr() {

        //註冊retriever client
        SmsRetrieverClient client = SmsRetriever.getClient(MainActivity.this);
        task = client.startSmsRetriever();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override//SMS-Retriever有成功接收到，要收從server寄來的簡訊內容
            public void onSuccess(Void aVoid) {

                MainActivity.this.registerReceiver(mybroadcastReciever,new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION));
                mybroadcastReciever.setOnMyBroadcaseRecieverListener(new MybroadcastReciever.MyBroadcastRecieverListener() {
                    @Override
                    public void onRecieved(String msg) {

                        //通知
                        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this,"1");
                        builder.setSmallIcon(R.drawable.icons8_sms_30)
                                .setContentText(msg)
                                .setContentTitle("假設這是簡訊");
                        Notification notification = builder.build();
                        manager.notify(1,notification);

                        txvNumber.setText(msg);//實際上需將此msg送出至伺服器做驗證動作，在此先做範例顯示
                        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mybroadcastReciever);
                    }
                    @Override
                    public void onTimeOut() {
                        txvNumber.setText("timeout!!");
                    }
                });
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //網路問題或其它未預期狀況導致task呼叫失敗時可在此呈現
            }
        });
    }

    private void initView() {

        txvNumber = (TextView)findViewById(R.id.txvNumber);
        mybroadcastReciever = new MybroadcastReciever();
        btnsend = (Button)findViewById(R.id.btnsend);
        //假裝開始驗證的button
        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //actually send phonenumber & appsignature to server

                //-----使用簡訊傳訊，測試簡訊內容格式
                Intent intent_sms = new Intent(Intent.ACTION_VIEW);
                intent_sms.setData( Uri.parse("smsto:"));
                intent_sms.setType("vnd.android-dir/mms-sms");
                intent_sms.putExtra("address",new String(phoneNumber));
                intent_sms.putExtra("sms_body","<#> your otp is 123456\n"+appSignatures.get(0));

                MainActivity.this.startActivity(intent_sms);

            }
        });
        //取得裝置signature
        AppSignatureHelper helper = new AppSignatureHelper(this);
        appSignatures = helper.getAppSignatures();

        //建立GoogleApiClient物件
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }
        //建立取得裝置號碼的提示框
    private void requestHint() {
        HintRequest hintRequest = new HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build();

        PendingIntent pendingIntent = Auth.CredentialsApi.getHintPickerIntent(googleApiClient,hintRequest);
        try {
            startIntentSenderForResult(pendingIntent.getIntentSender(),RESOLVE_HINT,
                    null,0,0,0);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestHint();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RESOLVE_HINT){
            if(resultCode == RESULT_OK){
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                phoneNumber = credential.getId();
                txvNumber.setText("要接收OTP的裝置號碼為："+phoneNumber +
                        ",\n 裝置signature為："+ appSignatures.get(0));
            }
        }
    }
}
