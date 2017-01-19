package com.example.miki.alexasmschecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by mpokr on 1/18/2017.
 */

public class BootCompleteReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, SMSService.class);
        context.startService(startServiceIntent);
    }
}
