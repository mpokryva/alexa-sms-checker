package com.example.miki.alexasmschecker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.ArraySet;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service that communicates with Alexa, and carries out intent requests.
 */

public class SMSService extends Service {

    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mRootRef = mDatabase.getReference();
    private SharedPreferences mSharedPref;
    private String alexaUserId;
    private DatabaseReference mUserIdRef;
    private DatabaseReference intentQueue;
    private SMSInbox mInbox;
    private Handler mHandler;
    private boolean initialDataLoaded;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInbox = new SMSInbox(getContentResolver());
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        alexaUserId = mSharedPref.getString(getString(R.string.alexa_user_id), null);
        mHandler = new Handler();
        if (alexaUserId != null) {
            mUserIdRef = mRootRef.child("users").child(alexaUserId);
            intentQueue = mUserIdRef.child("intentQueue");
            intentQueue.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    switch ((String) dataSnapshot.child("intentName").getValue()) {
                        case ("GetUnreadMessageCount"):
                            int unreadCount = getUnreadMessageCount();
                            System.out.println(dataSnapshot.getKey());
                            intentQueue.child(dataSnapshot.getKey()).child("result").setValue(unreadCount);
                            intentQueue.child(dataSnapshot.getKey()).child("done").setValue(true);
                            break;
                        case ("MakeCall"):
                            // Get value of recipient from Firebase.
                            String recipient = (String) dataSnapshot.child("recipient").getValue();
                            // Check if recipient is a phone number or a name
                            String phoneNumber;
                            boolean numberFound;
                            if (!PhoneNumberUtils.isGlobalPhoneNumber(recipient)) {
                                String contactName = recipient;
                                phoneNumber = getPhoneNumber(contactName);
                                if (phoneNumber == null) {
                                    // If Alexa spelled name wrong, find name variations, and try to match.
                                    NameFinder nameFinder = new NameFinder(contactName);
                                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                                    StrictMode.setThreadPolicy(policy);
                                    ArrayList<String> nameVars = nameFinder.findVariations();
                                    int i = 0;
                                    while (i < nameVars.size() && phoneNumber == null) {
                                        phoneNumber = getPhoneNumber(nameVars.get(i));
                                        i++;
                                    }
                                }
                                numberFound = (phoneNumber != null);
                            } else {
                                phoneNumber = recipient;
                                numberFound = true;
                            }
                            intentQueue.child(dataSnapshot.getKey()).child("numberFound").setValue(numberFound);
                            if (numberFound) {
                                Intent callIntent = new Intent(Intent.ACTION_CALL);
                                callIntent.setData(Uri.parse("tel:" + phoneNumber));
                                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                callIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                                startActivity(callIntent);
                            }
                            break;
                        case "SendSMS":
                            String messageBody = (String) dataSnapshot.child("messageBody").getValue();
                            String contactName = (String) dataSnapshot.child("recipient").getValue();
                            String phoneNum = getPhoneNumber(contactName);
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(phoneNum, null, messageBody, null, null);
                            break;
                    }
                    //intentQueue.child(dataSnapshot.getKey()).removeValue();
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            Intent notificationIntent = new Intent(this, SetUpActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            Notification notification = new NotificationCompat.Builder(this).
                    setSmallIcon(R.drawable.ic_insert_emoticon_black_24dp)
                    .setContentTitle("Alexa SMS App")
                    .setContentText("Doing work...")
                    .setContentIntent(pendingIntent).build();
            startForeground(1337, notification);
        }
    }


    public class SMSBinder extends Binder {

        public SMSService getService() {
            return SMSService.this;
        }
    }



    private String getPhoneNumber(String contactName) {
        String phoneNumber = getPhoneNumberHelper(contactName);
        if (phoneNumber == null) {
            // If Alexa spelled name wrong, find name variations, and try to match.
            NameFinder nameFinder = new NameFinder(contactName);
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            ArrayList<String> nameVars = nameFinder.findVariations();
            int i = 0;
            while (i < nameVars.size() && phoneNumber == null) {
                phoneNumber = getPhoneNumberHelper(nameVars.get(i));
                i++;
            }
        }
        return phoneNumber;

    }

    private String getPhoneNumberHelper(String name) {
        String result = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like'%" + name + "%'";
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, null, null);
        if (c.moveToFirst()) {
            int i = 0;
            while (!c.isAfterLast()) {
                result = c.getString(i);
                System.out.println(result);
                c.moveToNext();

            }
        }
        c.close();
        if (result == null) {
            System.out.println("Result is NULLLLLLLLLLL");
        }
        return result;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private int getUnreadMessageCount() {
        Cursor c = getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, null, "read = 0", null, null);
        int unreadCount = c.getCount();
        c.close();
        return unreadCount;
    }

}

