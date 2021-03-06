package com.siziksu.bluetooth.domain.bluetooth;

import com.siziksu.bluetooth.domain.BaseDomainContract;

public interface BluetoothDomainContract<D> extends BaseDomainContract<D> {

    void start();

    void refresh();

    void onDeviceClick(int position, String device);

    void onConnectButtonClick();

    void sendCommand(byte[] command);
}
