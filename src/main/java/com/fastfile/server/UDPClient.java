package com.fastfile.server;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class UDPClient {

    private static final int BUFFER_SIZE = 1460;
    private static final String FILES_DIRECTORY = "src/main/resources/com/fastfile/files/client";
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1997;
    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    public String clientType;

    public UDPClient() {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            System.out.println("Conectando ao servidor...");
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public String createConnection(String password) throws IOException {
        // estabelecendo a conexão
        threeWayHandshake();

        // envia a senha do cliente
        byte[] passwordBytes = password.getBytes();
        DatagramPacket passwordPacket = new DatagramPacket(passwordBytes, passwordBytes.length, serverAddress, SERVER_PORT);
        socket.send(passwordPacket);

        // aguarda a resposta do servidor
        byte[] clientTypeData = new byte[BUFFER_SIZE];
        DatagramPacket clientTypePacket = new DatagramPacket(clientTypeData, clientTypeData.length);
        socket.receive(clientTypePacket);

        // retorna a string contendo o tipo de cliente (upload ou download)
        clientType = new String(clientTypePacket.getData(), 0, clientTypePacket.getLength());
        return clientType;
    }

    public String tryPasswordAgain(String password) throws IOException {
        // envia a senha do cliente
        byte[] passwordBytes = password.getBytes();
        DatagramPacket passwordPacket = new DatagramPacket(passwordBytes, passwordBytes.length, serverAddress, SERVER_PORT);
        socket.send(passwordPacket);

        // aguarda a resposta do servidor
        byte[] clientTypeData = new byte[BUFFER_SIZE];
        DatagramPacket clientTypePacket = new DatagramPacket(clientTypeData, clientTypeData.length);
        socket.receive(clientTypePacket);

        // retorna a string contendo o tipo de cliente (upload ou download)
        clientType = new String(clientTypePacket.getData(), 0, clientTypePacket.getLength());
        return clientType;
    }

    private void threeWayHandshake() throws IOException {
        // envia um pacote SYN para estabelecer a conexão com o servidor
        byte[] synPacketData = "SYN".getBytes();
        DatagramPacket synPacket = new DatagramPacket(synPacketData, synPacketData.length, serverAddress, SERVER_PORT);
        socket.send(synPacket);

        // aguarda a resposta do servidor
        byte[] synAckPacketData = new byte[BUFFER_SIZE];
        DatagramPacket synAckPacket = new DatagramPacket(synAckPacketData, synAckPacketData.length);
        socket.receive(synAckPacket);

        // verifica se a resposta do servidor é um pacote SYN-ACK
        String synAckPacketPayload = new String(synAckPacket.getData(), 0, synAckPacket.getLength());
        if (!synAckPacketPayload.equals("SYN-ACK")) {
            throw new IOException("Resposta inválida do servidor: " + synAckPacketPayload);
        }

        // envia um pacote ACK para confirmar a conexão com o servidor
        byte[] ackPacketData = "ACK".getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackPacketData, ackPacketData.length, serverAddress, SERVER_PORT);
        socket.send(ackPacket);
    }

    public void sendFile(String fileName) throws IOException {
        byte[] fileNameBytes = fileName.getBytes();
        DatagramPacket fileNamePacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, serverAddress, SERVER_PORT);
        socket.send(fileNamePacket);

        byte[] ackNameFileData = new byte[Integer.BYTES];
        DatagramPacket ackNameFilePacket = new DatagramPacket(ackNameFileData, ackNameFileData.length);
        socket.setSoTimeout(1000); // tempo de espera para ACK
        socket.receive(ackNameFilePacket);
        System.out.println("ACK recebido para o pacote do nome do arquivo");

        ConnectionUtil.sendFile(fileName, FILES_DIRECTORY, serverAddress, SERVER_PORT, socket);
    }

    public List<String> getAvailableFiles() throws IOException {
        byte[] filesListBytes = new byte[BUFFER_SIZE];
        DatagramPacket filesListPacket = new DatagramPacket(filesListBytes, filesListBytes.length);
        socket.receive(filesListPacket);
        String filesList = new String(filesListPacket.getData(), 0, filesListPacket.getLength());
        System.out.println("Arquivos disponíveis no servidor:");
        System.out.println(filesList);

        return List.of(filesList.split("\n"));
    }

    public void receiveFile(String fileName) throws IOException {
        ConnectionUtil.receiveFile(fileName, FILES_DIRECTORY, serverAddress, SERVER_PORT, socket);
    }
}