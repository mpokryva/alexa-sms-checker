package com.example.miki.alexasmschecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Makes SMSService start at bootup (if initial setup has been completed).
 */

public class BootCompleteReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, SMSService.class);
        context.startService(startServiceIntent);
    }
}
