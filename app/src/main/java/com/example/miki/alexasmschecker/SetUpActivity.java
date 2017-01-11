package com.example.miki.alexasmschecker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.method.CharacterPickerDialog;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SetUpActivity extends AppCompatActivity {

    private EditText pinEditText;
    private EditText phoneNumEditText;
    private Button confirmButton;

    // Write a message to the database
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = database.getReference();
    private DatabaseReference newPinsRef = rootRef.child("newPins");
    private DatabaseReference verificationQueueRef = rootRef.child("verificationQueue");

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
                    final String pinFinal = processedPin;
                    processedPinRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                processedPinRef.removeValue();
                                rootRef.child("users").child(pinFinal).child("phoneNumber").setValue(processedNumToSend);
                                Toast.makeText(SetUpActivity.this, "Pin matched. You're ready to go!", Toast.LENGTH_SHORT).show();
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

    private void addToVerificationQueue(String phoneNumber){

    }




}
