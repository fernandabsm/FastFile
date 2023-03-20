package com.fastfile.server;

public class ClientHolder {
    private UDPClient client;

    private final static ClientHolder INSTANCE = new ClientHolder();

    private ClientHolder() {
    }

    public static ClientHolder getInstance() {
        return INSTANCE;
    }

    public void setClient(UDPClient udpClient) {
        this.client = udpClient;
    }

    public UDPClient getClient() {
        return this.client;
    }
}
