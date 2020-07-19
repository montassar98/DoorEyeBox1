package com.mag_solutions.dooreyebox1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.mag_solutions.dooreyebox1.Model.CustomProgress;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import org.json.JSONException;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.mag_solutions.dooreyebox1.MainActivity.isChecking;
import static com.mag_solutions.dooreyebox1.MainActivity.statusBarTransparent;

public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener {
    private static String API_KEY ="" ;
    private static String SESSION_ID ="" ;
    private static String TOKEN ="";
    private static final String TAG = "VideoChatActivity";
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    private Session mSession;
    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private FirebaseAuth mAuth;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor editor;
    boolean playWithoutVideo = false;
    private LottieAnimationView mAnim;

    private CustomProgress  mProgressDialog = CustomProgress.getInstance();


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);
        statusBarTransparent(getWindow());

        mSharedPreferences = getBaseContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        editor = mSharedPreferences.edit();
        mAuth = FirebaseAuth.getInstance();
        mAnim = findViewById(R.id.anim_blink);

        requestPermissions();
        mPublisherViewContainer = (FrameLayout)findViewById(R.id.publisher_container);
        mSubscriberViewContainer = (FrameLayout)findViewById(R.id.subscriber_container);
        if (mPublisher !=null){mPublisher.destroy();}
        if (mSubscriber !=null){mSubscriber.destroy();}
        mProgressDialog.showProgress(this, "Waiting For The Connection", false);


    }

    public String generateId(String uid) {

        if (uid != null) {
            StringBuilder id = new StringBuilder();
            for (int i = uid.length() - 6; i < uid.length(); i++) {
                id.append(uid.charAt(i));
            }
            return id.toString();
        }else return null;
    }

    public void fetchSessionConnectionData() {
        RequestQueue reqQueue = Volley.newRequestQueue(this);
        String roomId = generateId(mAuth.getUid());
        //int roomId= mSharedPreferences.getInt("ROOM_ID",100000);
        String url ="https://dooreye.herokuapp.com";
        if(isChecking){
            url = "https://dooreyebox.herokuapp.com";
            isChecking=false;
            playWithoutVideo =true;
        }
                reqQueue.add(new JsonObjectRequest(Request.Method.GET,
                url + "/room/:"+roomId,
                null, response -> {
                    try {
                        API_KEY = response.getString("apiKey");
                        SESSION_ID = response.getString("sessionId");
                        TOKEN = response.getString("token");

                        Log.i(TAG, "API_KEY: " + API_KEY);
                        Log.i(TAG, "SESSION_ID: " + SESSION_ID);
                        Log.i(TAG, "TOKEN: " + TOKEN);

                        mSession = new Session.Builder(VideoChatActivity.this, API_KEY, SESSION_ID).build();
                        mSession.setSessionListener(VideoChatActivity.this);
                        mSession.connect(TOKEN);

                    } catch (JSONException error) {
                        Log.e(TAG, "Web Service error: " + error.getMessage());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Web Service error: " + error.getMessage());
            }
        }));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = { Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        if (EasyPermissions.hasPermissions(this, perms)) {
            // initialize view objects from your layout
            mPublisherViewContainer = (FrameLayout) findViewById(R.id.publisher_container);
            mSubscriberViewContainer = (FrameLayout) findViewById(R.id.subscriber_container);

            // initialize and connect to the session
            fetchSessionConnectionData();

        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", RC_VIDEO_APP_PERM, perms);
        }
    }


    @Override
    public void onConnected(Session session) {
        Log.i(TAG, "onConnected: ");
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(this);

        mPublisherViewContainer.addView(mPublisher.getView());

        if (mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);


    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(TAG, "onDisconnected: ");
        Toast.makeText(VideoChatActivity.this, "onDisconnected", Toast.LENGTH_LONG).show();

        session.disconnect();
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(TAG, "onStreamReceived: ");
        if (mSubscriber == null) {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewContainer.addView(mSubscriber.getView());

            mProgressDialog.hideProgress();
            if (playWithoutVideo) {
                mPublisherViewContainer.setVisibility(View.GONE);
                mSubscriberViewContainer.setVisibility(View.GONE);
                mAnim.setVisibility(View.VISIBLE);
                playWithoutVideo = false;
            }


        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(TAG, "onStreamDropped: ");
        if (mSubscriber != null || mPublisher != null ) {
            mSubscriber = null;
            mPublisher=null;
            mPublisherViewContainer.removeAllViews();
            mSubscriberViewContainer.removeAllViews();
        }
        session.disconnect();
        startActivity(new Intent(this,MainActivity.class));
        finish();



    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(TAG, "onError: "+opentokError.getMessage());
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Log.i(TAG, "onPointerCaptureChanged: ");
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(TAG, "onStreamCreated: ");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(TAG, "onStreamDestroyed: ");
        stream.getSession().unpublish(publisherKit);
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.i(TAG, "onError: ");
    }

}