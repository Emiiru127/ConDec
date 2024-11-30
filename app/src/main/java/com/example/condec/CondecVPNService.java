package com.example.condec;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import com.example.condec.Database.*;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.room.Room;

import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.pcap4j.packet.factory.PacketFactories;
import org.pcap4j.util.ByteArrays;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CondecVPNService extends VpnService {

    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    private static final String VPN_ADDRESS = "10.0.0.2";
    private static final String VPN_ROUTE = "0.0.0.0";
    private static final int MTU = 1500;
    private CondecDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(),
                CondecDatabase.class, "CondecDatabase").build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        vpnThread = new Thread(() -> {
            try {
                setupVpn();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "CondecVPNThread");
        vpnThread.start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        closeInterface();
        super.onDestroy();
    }

    private void setupVpn() throws IOException {
        VpnService.Builder builder = new VpnService.Builder();
        builder.setMtu(MTU);
        builder.addAddress(VPN_ADDRESS, 24);
        builder.addRoute(VPN_ROUTE, 0);
        builder.setSession("CondecVPNService");

        // Configure the VPN interface
        vpnInterface = builder.establish();
        if (vpnInterface == null) {
            throw new IllegalStateException("VPN interface is null");
        }

        // Open a DatagramChannel
        DatagramChannel tunnel = DatagramChannel.open();
        try {
            tunnel.connect(new InetSocketAddress("8.8.8.8", 53)); // Example: DNS server
            tunnel.configureBlocking(false);

            FileInputStream inStream = new FileInputStream(vpnInterface.getFileDescriptor());
            FileOutputStream outStream = new FileOutputStream(vpnInterface.getFileDescriptor());
            ByteBuffer packet = ByteBuffer.allocate(MTU);

            while (!Thread.interrupted()) {
                int length = inStream.read(packet.array());
                if (length > 0) {
                    packet.limit(length);
                    Log.i(TAG, "Received packet: " + length + " bytes");

                    if (!isBlocked(packet)) {
                        packet.flip();
                        while (packet.hasRemaining()) {
                            tunnel.write(packet);
                        }
                    }
                    packet.clear();
                } else if (length < 0) {
                    break; // End of stream
                }

                int bytesRead = tunnel.read(packet);
                if (bytesRead > 0) {
                    packet.limit(bytesRead);
                    Log.i(TAG, "Sending packet: " + bytesRead + " bytes");

                    outStream.write(packet.array(), 0, bytesRead);
                    packet.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error setting up VPN tunnel: " + e.getMessage());
        } finally {
            if (tunnel.isOpen()) {
                tunnel.close();
            }
        }
    }

    private boolean isBlocked(ByteBuffer packetBuffer) {
        DefaultBlockedUrlDao defaultBlockedUrlDao = db.defaultBlockedUrlDao();
        UserBlockedUrlDao userBlockedUrlDao = db.userBlockedUrlDao();

        // Retrieve the list of blocked URLs
        List<DefaultBlockedUrl> defaultBlockedUrls = defaultBlockedUrlDao.getAllUrls();
        List<UserBlockedUrl> userBlockedUrls = userBlockedUrlDao.getAllUrls();

        // Extract destination address from the packet (IPv4 example)
        packetBuffer.position(16); // Skip to destination address
        int destAddress = packetBuffer.getInt();

        String destAddressStr = String.format("%d.%d.%d.%d",
                (destAddress >> 24) & 0xFF,
                (destAddress >> 16) & 0xFF,
                (destAddress >> 8) & 0xFF,
                destAddress & 0xFF);
        Log.i(TAG, "VPNService - Destination Address: " + destAddressStr);

        for (DefaultBlockedUrl url : defaultBlockedUrls) {
            if (destAddressStr.equals(url.getUrl())) {
                Log.i(TAG, "Blocked by default URL list: " + destAddressStr);
                return true;
            }
        }

        for (UserBlockedUrl url : userBlockedUrls) {
            if (destAddressStr.equals(url.getUrl())) {
                Log.i(TAG, "Blocked by user URL list: " + destAddressStr);
                return true;
            }
        }

        Log.i(TAG, "Not blocked: " + destAddressStr);
        return false;
    }

    private void closeInterface() {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        vpnInterface = null;
    }
}

