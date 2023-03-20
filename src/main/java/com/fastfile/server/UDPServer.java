package com.fastfile.server;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.fastfile.server.ConnectionUtil.receiveFile;
import static com.fastfile.server.ConnectionUtil.sendFile;

public class UDPServer {

    private static final int BUFFER_SIZE = 1460;

    private static final String FILES_DIRECTORY = "src/main/resources/com/fastfile/files/server";
    private static final String PASSWORD = "123456";
    private static final String NO_PASSWORD = "NO_PASSWORD";
    private final DatagramSocket socket;

    public UDPServer() {
        try {
            socket = new DatagramSocket(1997);
            System.out.println("Servidor iniciado. Aguardando conexões...");
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void acceptConnections() {
        Thread thread = new Thread(() -> {
            try {
                // aguarda a conexão do cliente
                byte[] synPacketData = new byte[3];
                DatagramPacket synPacket = new DatagramPacket(synPacketData, synPacketData.length);
                socket.receive(synPacket);

                // verifica se é um pacote SYN
                String synPacketPayload = new String(synPacket.getData(), 0, synPacket.getLength());
                if (!synPacketPayload.equals("SYN")) {
                    throw new IOException("Solicitação Inválida.");
                }

                // Cria o pacote SYN-ACK
                String synAckPayload = "SYN-ACK";
                byte[] synAckPacketData = synAckPayload.getBytes();
                DatagramPacket synAckPacket = new DatagramPacket(synAckPacketData, synAckPacketData.length, synPacket.getAddress(), synPacket.getPort());

                // Envia o pacote SYN-ACK para o cliente
                socket.send(synAckPacket);

                // aguarda o ack do cliente
                byte[] ackPacketData = new byte[3];
                DatagramPacket ackPacket = new DatagramPacket(ackPacketData, ackPacketData.length);
                socket.receive(ackPacket);

                // verifica se é um pacote ACK
                String ackPacketPayload = new String(ackPacket.getData(), 0, ackPacket.getLength());
                if (!ackPacketPayload.equals("ACK")) {
                    throw new IOException("Solicitação Inválida.");
                }

                // obtém a senha do cliente
                byte[] passwordBuffer = new byte[BUFFER_SIZE];
                DatagramPacket passwordPacket = new DatagramPacket(passwordBuffer, passwordBuffer.length);
                socket.receive(passwordPacket);
                String password = new String(passwordPacket.getData(), 0, passwordPacket.getLength());

                while (true) {
                    while (!password.equals(PASSWORD) && !password.equals(NO_PASSWORD)) {
                        // Cria o pacote de confirmação de permissão para upload
                        String canNotDoUploadPayload = "error";
                        byte[] canNotDoUploadPacketData = canNotDoUploadPayload.getBytes();
                        DatagramPacket canNotDoUploadPacket = new DatagramPacket(canNotDoUploadPacketData, canNotDoUploadPacketData.length, synPacket.getAddress(), synPacket.getPort());
                        socket.send(canNotDoUploadPacket);

                        socket.receive(passwordPacket);
                        password = new String(passwordPacket.getData(), 0, passwordPacket.getLength());
                    }
                    if (password.equals(PASSWORD)) {
                        // Cria o pacote de confirmação de permissão para upload
                        String canDoUploadPayload = "upload";
                        byte[] canDoUploadPacketData = canDoUploadPayload.getBytes();
                        DatagramPacket canDoUploadPacket = new DatagramPacket(canDoUploadPacketData, canDoUploadPacketData.length, synPacket.getAddress(), synPacket.getPort());
                        socket.send(canDoUploadPacket);
                        System.out.println("Cliente autorizado a fazer uploads.");

                        String fileList = getAvailableFiles();
                        socket.setSoTimeout(0);
                        byte[] fileListBytes = fileList.getBytes();
                        DatagramPacket fileListPacket = new DatagramPacket(fileListBytes, fileListBytes.length,
                                synPacket.getAddress(), synPacket.getPort());
                        socket.send(fileListPacket);

                        while (true) {
                            receiveFile(FILES_DIRECTORY, synPacket.getAddress(), synPacket.getPort(), socket);
                        }
                    } else {
                        String canDoDownloadPayload = "download";
                        byte[] canDoDownloadPacketData = canDoDownloadPayload.getBytes();
                        DatagramPacket canDoDownloadPacket = new DatagramPacket(canDoDownloadPacketData, canDoDownloadPacketData.length, synPacket.getAddress(), synPacket.getPort());
                        socket.send(canDoDownloadPacket);
                        System.out.println("Cliente autorizado a fazer downloads.");

                        String fileList = getAvailableFiles();
                        byte[] fileListBytes = fileList.getBytes();
                        DatagramPacket fileListPacket = new DatagramPacket(fileListBytes, fileListBytes.length,
                                synPacket.getAddress(), synPacket.getPort());
                        socket.send(fileListPacket);

                        while (true) {
                            socket.setSoTimeout(0);
                            // aguarda a seleção do arquivo pelo cliente
                            byte[] fileSelectionBuffer = new byte[BUFFER_SIZE];
                            DatagramPacket fileSelectionPacket = new DatagramPacket(fileSelectionBuffer, fileSelectionBuffer.length);
                            socket.receive(fileSelectionPacket);
                            String fileName = new String(fileSelectionPacket.getData(), 0, fileSelectionPacket.getLength());

                            // envia o arquivo selecionado pelo cliente
                            sendFile(fileName, FILES_DIRECTORY, synPacket.getAddress(), synPacket.getPort(), socket);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }

    public String getAvailableFiles() {
        StringBuilder fileList = new StringBuilder();
        try {
            Files.list(Paths.get(FILES_DIRECTORY))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .forEach(fileName -> fileList.append(fileName).append("\n"));
        } catch (IOException e) {
            System.err.println("Erro ao listar arquivos: " + e.getMessage());
        }
        return fileList.toString();
    }

    public static void main(String[] args) {
        UDPServer udpServer = new UDPServer();
        udpServer.acceptConnections();
    }
}
