package com.example.miki.alexasmschecker;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by mpokr on 1/12/2017.
 */

public class SMSInbox {

    private final Uri SMS_INBOX = Uri.parse("content://sms/inbox");
    private ContentResolver mResolver;

    public SMSInbox(ContentResolver contentResolver) {
        mResolver = contentResolver;
    }

    public int getUnreadCount() {
        Cursor c = mResolver.query(SMS_INBOX, null, "read = 0", null, null);
        int unreadCount = c.getCount();
        c.close();
        return unreadCount;
    }
}
