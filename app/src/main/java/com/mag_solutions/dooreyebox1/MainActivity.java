package com.mag_solutions.dooreyebox1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mag_solutions.dooreyebox1.Model.Motion;

import java.util.Date;
import java.util.Random;

import static com.mag_solutions.dooreyebox1.CameraFragment.isCameraRunning;
import static com.mag_solutions.dooreyebox1.SplashActivity.generateId;

public class MainActivity extends AppCompatActivity implements Camera2Fragment.CapturedImageListener {

    private static String TAG = "MainActivity";
    public static Boolean isChecking = false;
    public static Boolean visitorCalled = false;
    private MutableLiveData<Boolean> ringData, motionData;
    private Motion motion;


    private FirebaseAuth mAuth =  FirebaseAuth.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference instantImagePathRef = database.getReference().child("BoxList")
            .child(generateId(mAuth.getCurrentUser().getUid())).child("history").child("instantImagePath");

    private DatabaseReference boxHistoryRef = database.getReference("BoxList/"+generateId(mAuth.getUid())+"/history/");
    private DatabaseReference boxUsersRef = database.getReference("BoxList/"+generateId(mAuth.getUid())+"/users");

    DatabaseReference mHardwareTriggerRef = database.getReference().child("BoxList")
            .child(generateId(mAuth.getCurrentUser().getUid())).child("hardware");

    private CardView cvCameraHolder;
    private RelativeLayout rlRing;
    private static boolean isFirstOpen = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusBarTransparent(getWindow());

        cvCameraHolder = findViewById(R.id.cv_img_holder);
        rlRing = findViewById(R.id.rl_ring);
        cvCameraHolder.setVisibility(View.GONE);

        initTriggers();


    }

    private void initTriggers() {
        mHardwareTriggerRef.removeValue();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //initiate the mutable data variables for rings and calls
        ringData = new MutableLiveData<>();
        motionData = new MutableLiveData<>();

        //setup a trigger for ring calls
        ringData.observeForever(ringObserver);
        //setup a trigger for motion detection
        motionData.observeForever(motionObserver);
        visitorCalled = false;
        checkFrontDoor();
        initAnim();
        hardwareTriggers();
    }

    private void hardwareTriggers() {

        mHardwareTriggerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "hardwareTriggers -> data: " + snapshot.toString());
                if (snapshot.hasChild("motion") ||snapshot.hasChild("ring") ){

                    if (snapshot.hasChild("ring")){
                        if (snapshot.child("ring").getValue().equals(true)){
                            Log.d(TAG, "hardwareTriggers -> data: call received");
                            //TODO call received logic
                            ringData.setValue(true);
                            isChecking = false;

                            snapshot.getRef().removeValue();
                            return;
                        }
                    }
                    if (snapshot.child("motion").getValue().equals(true)){
                        Log.d(TAG, "hardwareTriggers -> data: motion detected");
                        //TODO motion detected logic
                        motionData.setValue(true);
                        rlRing.setVisibility(View.GONE);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> rlRing.setVisibility(View.VISIBLE) ,5000);


                        snapshot.getRef().child("motion").removeValue();
                    }


                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    private void initAnim() {
        ImageView imgLogo = findViewById(R.id.img_logo);
        TextView txtAppName = findViewById(R.id.txt_app_name);
        AnimatorSet mSetAnim =(AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.main_ani_set);
        AnimatorSet mTitleSetAnim =(AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.txt_set_anim);
        mSetAnim.setTarget(imgLogo);
        mTitleSetAnim.setTarget(txtAppName);
        mSetAnim.start();
        mTitleSetAnim.start();
    }


    private Observer<Boolean> motionObserver = aBoolean -> {
        Log.d(TAG, "motionObserver : motion triggered! ");
       if(!isCameraRunning) {
           if (aBoolean) {
               new Handler(Looper.getMainLooper()).postDelayed(() -> {
                   if (!visitorCalled) {
                       Toast.makeText(this, "motion detected", Toast.LENGTH_SHORT).show();
                       cvCameraHolder.setVisibility(View.VISIBLE);
                       initiateCameraFragment();

                       new Handler(Looper.getMainLooper()).postDelayed(() -> {
                           closeCameraFragment();
                           cvCameraHolder.setVisibility(View.GONE);

                       }, 30000);
                       new Handler(Looper.getMainLooper()).postDelayed(this::createMotionHistory, 10000);
                   }
               }, 2 * 1000);

           }
       }

    };
    private Observer<Boolean> ringObserver = aBoolean -> {
        Log.d(TAG, "ringObserver : call triggered! ");
        if (aBoolean){
            Toast.makeText(this, "call received", Toast.LENGTH_SHORT).show();
            visitorCalled = true;
            MediaPlayer media = MediaPlayer.create(this, R.raw.doorbell_rington);
            media.start();
           startActivity(new Intent(MainActivity.this, HandleCallActivity.class));
           finish();
        }
    };


    private void createMotionHistory() {
        Random random = new Random();
        int id = random.nextInt(99999-10000)+10000;
        Date date = new Date();
        motion = new Motion();
        motion.setId(id);
        motion.setEventTime(date);
        motion.setStatus("Motion");
        instantImagePathRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                Log.d(TAG, "instant image path \n"+ dataSnapshot1.getValue());
                if (dataSnapshot1.getValue() != null)
                    MainActivity.this.motion.setVisitorImage(dataSnapshot1.getValue().toString());

                boxHistoryRef.child("motions").child(String.valueOf(MainActivity.this.motion.getEventTime()))
                        .setValue(MainActivity.this.motion);

                instantImagePathRef.removeValue();
                //startActivity(new Intent(MainActivity.this,MainActivity.class));
                //finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                startActivity(new Intent(MainActivity.this,MainActivity.class));
                finish();

            }
        });
        //Ring ring = new Ring(id, currentTime.toString(), imagePath);

    }
    public static void statusBarTransparent(Window w){
        //Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    public void onUserCalled(View view) {
        Log.d(TAG, "onUserCalled: ");
        ringData.setValue(true);
        isChecking = false;

    }


    public void onMotionCreated(View view) {
        Log.d(TAG, "onMotionCreated: ");
        motionData.setValue(true);
        rlRing.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> rlRing.setVisibility(View.VISIBLE) ,5000);


    }


    //camera fragment
    private void initiateCameraFragment() {
        Log.d(TAG, "initiateCameraFragment: ");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_camera,
                        new Camera2Fragment(this,Camera2Fragment.CAMERA_FRAGMENT_FOR_MOTION),
                        "camera_fragment")
                .commit();
    }
    private void closeCameraFragment() {
        Fragment frm = getSupportFragmentManager().findFragmentByTag("camera_fragment");
        if(frm!=null){
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(frm);
            fragmentTransaction.commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        motionData.removeObserver(motionObserver);
        ringData.removeObserver(ringObserver);

    }

    @Override
    public void isImageCaptured(boolean isCaptured) {
        Log.d(TAG, "isImageCaptured: "+isCaptured);
    }

    private void checkFrontDoor() {
        boxUsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dp : dataSnapshot.getChildren())
                {
                    if (dp.hasChild("checking")) {
                        //Log.d(TAG, "WhoPickedUp \n" + dp.child("Ringing").getValue().toString());
                        //if (boxUsersRef.child(dp.getKey()).child("Ringing").child("pickup").getKey())
                        isChecking = true;
                        startActivity(new Intent(MainActivity.this, VideoChatActivity.class));
                        //TODO finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: "+databaseError.toString());
            }
        });
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
        System.exit(0);
    }
}