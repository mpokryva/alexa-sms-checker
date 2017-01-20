package com.example.miki.alexasmschecker;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * The initial set up activity. User is supposed to match pin spoken out loud by Alexa in this activity.
 */
public class SetUpActivity extends AppCompatActivity {

    private EditText pinEditText;
    private EditText phoneNumEditText;
    private Button confirmButton;
    private SMSService mSMSService;
    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mRootRef = mDatabase.getReference();
    private DatabaseReference newPinsRef = mRootRef.child("newPins");
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mSMSService = ((SMSService.SMSBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSMSService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        pinEditText = (EditText) findViewById(R.id.edittext_pin);
        confirmButton = (Button) findViewById(R.id.confirm_button);
        phoneNumEditText = (EditText) findViewById(R.id.edittext_phone_num);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pinString = pinEditText.getText().toString();
                String processedPin = "";
                final String phoneNumber = phoneNumEditText.getText().toString();
                //Convert characters to lowercase (just in case).
                for (int i = 0; i < pinString.length(); i++) {
                    processedPin += Character.toLowerCase(pinString.charAt(i));
                }
                // Check that phone number is valid.
                if (PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                    String processedNumber = "";
                    for (int i = 0; i < phoneNumber.length(); i++) {
                        // If it is positive, then it is a number.
                        if (Character.getNumericValue(phoneNumber.charAt(i)) > 0) {
                            processedNumber += phoneNumber.charAt(i);
                        }
                    }
                    final DatabaseReference processedPinRef = newPinsRef.child(processedPin);
                    final String processedNumToSend = processedNumber;
                    processedPinRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Get the userId
                                String userId = (String) dataSnapshot.child("userId").getValue();
                                // Add the user.
                                mRootRef.child("users").child(userId).child("phoneNumber").setValue(processedNumToSend);
                                // Remove the pin from list of new pins.
                                Toast.makeText(SetUpActivity.this, "Pin matched. You're ready to go!", Toast.LENGTH_SHORT).show();

                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(getString(R.string.alexa_user_id), userId);
                                editor.apply();

                                startSMSService();
                                bindToService();

                                processedPinRef.removeValue();
                            } else {
                                Toast.makeText(SetUpActivity.this, "Pin does not exist.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                    Toast.makeText(SetUpActivity.this, "Invalid phone number. Try again.",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
    }



    public void startSMSService() {
        Intent i = new Intent(this, SMSService.class);
        startService(i);
    }

    public void stopMethod() {
        Intent i = new Intent(this, SMSService.class);
        stopService(i);
    }

    public void bindToService() {
        Intent intent = new Intent(this, SMSService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unbindService(mServiceConnection);
    }


}
