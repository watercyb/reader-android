package com.blueberryjoy.reader.main.utils;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.main.message.MessageBluetooth;
import com.blueberryjoy.reader.main.message.MessageSender;
import com.blueberryjoy.reader.settings.ReaderSettings;

public class BluetoothConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!ReaderSettings.getIsStopWhenBlueToothDisconnected()) return;
        String action = intent.getAction();
        BluetoothDevice device;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        } else {
            device = getDevice(intent);
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            if (device != null)
                MessageSender.sendMessage(new MessageBluetooth(MessageBluetooth.ACTION_DISCONNECT, device.getAddress()));
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            if (device != null)
                MessageSender.sendMessage(new MessageBluetooth(MessageBluetooth.ACTION_CONNECT, device.getAddress()));
        }
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private BluetoothDevice getDevice(@NonNull Intent intent) {
        return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    }
}