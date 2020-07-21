package com.mag_solutions.dooreyebox1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mag_solutions.dooreyebox1.Model.Ring;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Random;

import static com.mag_solutions.dooreyebox1.SplashActivity.generateId;

public class HandleCallActivity extends AppCompatActivity implements Camera2Fragment.CapturedImageListener {
    private static final String TAG = "HandleCallActivity";

    //firebase initiation
    private FirebaseAuth mAuth =  FirebaseAuth.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference callingRef,boxUsersRef, boxHistoryRef;
    private DatabaseReference instantImagePathRef;
    //-------------------

    private Ring ring;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_call);
        //change status bar to transparent
        MainActivity.statusBarTransparent(getWindow());
        String boxId =generateId(mAuth.getUid());
        callingRef = database.getReference("BoxList/"+boxId);
        boxUsersRef = database.getReference("BoxList/"+boxId+"/users");
        boxHistoryRef = database.getReference("BoxList/"+boxId+"/history/");
        instantImagePathRef = database.getReference().child("BoxList")
                .child(generateId(mAuth.getCurrentUser().getUid())).child("history").child("instantImagePath");

    }

    @Override
    protected void onStart() {
        super.onStart();
        //init camera to take picture
        initiateCameraFragment();
    }

    private void initiateCameraFragment() {
        Log.d(TAG, "initiateCameraFragment: ");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_camera,
                        new  Camera2Fragment(this,Camera2Fragment.CAMERA_FRAGMENT_FOR_RING),
                        "camera_fragment")
                .commit();
    }

    public void onBackClicked(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    public static Observer<Boolean> observeImageCaptured = aBoolean -> {
        Log.d(TAG, "observer:  "+aBoolean);
        if (aBoolean){
            //image already captured and saved to the firebase

        }
    };

    private void makeCall(){
        boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                for (DataSnapshot dp : dataSnapshot.getChildren())
                {
                    //generate both references for calling and ringing
                    //create a calling reference and define the numbers that this box is calling
                    //create a Ringing reference below all the user that this box is calling.
                    Log.d(TAG, "onDataChange: "+dp.getKey());
                    //send roomId to firebase(opentok roomId)
                    if (!dp.child("status").getValue().equals("waiting")) {
                        callingRef.child("Calling").child(dp.getKey()).setValue("Calling...");
                        boxUsersRef.child(dp.getKey()).child("Ringing").setValue("Ringing...");
                        boxUsersRef.child(dp.getKey()).child("pickup").setValue(false);
                    }
                }
                //handler for ending a call after an amount of seconds
                new Handler(Looper.getMainLooper()).postDelayed(() ->{
                    endCallAndRingNotificationManager();
                    },10 * 1000);

                boxUsersRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Log.d(TAG, "onChildChanged: pickup \n"+dataSnapshot.toString()+"\n"+s);
                        if (dataSnapshot.hasChild("pickup"))
                        {
                            if (dataSnapshot.child("pickup").getValue().equals(true))
                            {
                                // remove all other references
                                removeOtherRefs();

                                //---------------------------
                                //redirect user to the videoChatActivity on someone responded
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    Log.d(TAG, "onChildChanged: get a response and move to the chat activity");
                                    startActivity(new Intent(HandleCallActivity.this,VideoChatActivity.class));
                                    finish();
                                },3000);

                            }
                        }
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void removeOtherRefs() {
        boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dp : dataSnapshot.getChildren())
                {
                    if (dp.hasChild("Ringing")&& dp.hasChild("pickup"))
                    {
                        boxUsersRef.child(dp.getKey()).child("Ringing").removeValue();
                        if(dp.child("pickup").getValue().equals(false)) {
                            boxUsersRef.child(dp.getKey()).child("pickup").removeValue();
                        }
                    }


                }
                callingRef.child("Calling").removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endCallAndRingNotificationManager() {
        boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                boolean hasResponder = false;
                for (DataSnapshot dp : dataSnapshot1.getChildren())
                {
                    if (dp.hasChild("Ringing")&& dp.hasChild("pickup"))
                    {
                        //Removing all ringing references after 30sc
                        boxUsersRef.child(dp.getKey()).child("Ringing").removeValue();
                        if(dp.child("pickup").getValue().equals(false)) {
                            //Removing all the pickup references if they equal to false
                            boxUsersRef.child(dp.getKey()).child("pickup").removeValue();
                        }else hasResponder = true;
                    }else Log.d(TAG, "No one");
                    if (dp.hasChild("pickup"))
                    {
                        if(dp.child("pickup").getValue().equals(true))
                            hasResponder = true;
                    }

                }
                callingRef.child("Calling").removeValue();
                Log.d(TAG, "has responder: "+hasResponder);
                if (!hasResponder)
                {
                    Log.d(TAG, "adding to the history...");
                    // send to the history
                    Random random = new Random();
                    int id = random.nextInt(99999-10000)+10000;
                    //Date currentTime = Calendar.getInstance().getTime();
                    //get current date time with Date()
                    Date date = new Date();
                    ring = new Ring();
                    ring.setId(id);
                    ring.setEventTime(date);
                    ring.setStatus("Ring");
                    ring.setResponder("No one");
                    instantImagePathRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                            Log.d(TAG, "instant image path \n"+ dataSnapshot1.getValue());
                            if (dataSnapshot1.getValue() != null)
                                HandleCallActivity.this.ring.setVisitorImage(dataSnapshot1.getValue().toString());
                            boxHistoryRef.child("rings").child(String.valueOf(HandleCallActivity.this.ring.getEventTime()))
                                    .setValue(HandleCallActivity.this.ring);
                            instantImagePathRef.removeValue();
                            closeCameraFragment();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {


                        }
                    });
                    startActivity(new Intent(HandleCallActivity.this, MainActivity.class));

                    //Ring ring = new Ring(id, currentTime.toString(), imagePath);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
    public void isImageCaptured(boolean isCaptured) {
        Log.d(TAG, "isImageCaptured: " + isCaptured);
        Toast.makeText(this, ""+isCaptured, Toast.LENGTH_SHORT).show();
        if (isCaptured)
            makeCall();
    }
}