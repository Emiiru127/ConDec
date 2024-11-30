package com.example.condec;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check network connectivity status
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // Check if VPN is switched on
        SharedPreferences sharedPreferences = context.getSharedPreferences("condecPref", Context.MODE_PRIVATE);
        boolean isVpnOn = sharedPreferences.getBoolean("condecVPNServiceStatus", false);

        Log.d("NetworkChangeReceiver", "isConnected: " + isConnected);
        Log.d("NetworkChangeReceiver", "isVpnOn: " + isVpnOn);

        if (isConnected && isVpnOn) {
            // Check if connection is Wi-Fi or mobile data
            int networkType = activeNetwork.getType();
            if (networkType == ConnectivityManager.TYPE_WIFI || networkType == ConnectivityManager.TYPE_MOBILE) {
                // Reconnect VPN if internet is available and VPN is switched on
                Intent vpnIntent = new Intent(context, CondecVPNService.class);
                context.startService(vpnIntent);
                Log.d("NetworkChangeReceiver", "Internet is back on "
                        + (networkType == ConnectivityManager.TYPE_WIFI ? "Wi-Fi" : "Mobile Data") +
                        ", reconnecting VPN...");
            }
        } else {
            Log.d("NetworkChangeReceiver", "No internet connection or VPN is off.");
        }
    }
}
