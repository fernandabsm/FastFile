package com.fastfile.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ConnectionUtil {

    private static final int PACKET_SIZE = 1460; // tamanho máximo de carga útil de um pacote UDP
    private static final int WINDOW_SIZE = 10; // tamanho da janela

    public static void sendFile(String fileName, String filesDirectory, InetAddress address, int port, DatagramSocket socket) throws IOException {
        Path filePath = Paths.get(filesDirectory, fileName);
        if (Files.exists(filePath)) {
            byte[] fileBytes = Files.readAllBytes(filePath);
            int fileSize = fileBytes.length;
            int sizeOfPacketWithoutSeqNum = PACKET_SIZE - 4;
            int numPackets = (int) Math.ceil((double) fileSize / sizeOfPacketWithoutSeqNum);
            int lastPacketSize = fileSize % sizeOfPacketWithoutSeqNum;
            if (lastPacketSize == 0) {
                lastPacketSize = sizeOfPacketWithoutSeqNum;
            }
            int base = 0;
            int nextSeqNum = 0;
            int unAckedSeqNum = 0;
            int packetsSent = 0;

            ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
            sizeBuffer.putInt(fileSize);
            DatagramPacket sizePacket = new DatagramPacket(sizeBuffer.array(), sizeBuffer.array().length, address, port);
            socket.send(sizePacket);
            System.out.println("Tamanho do arquivo enviado: " + fileSize + " bytes");

            byte[] ackLengthFileData = new byte[Integer.BYTES];
            DatagramPacket ackLengthFilePacket = new DatagramPacket(ackLengthFileData, ackLengthFileData.length);
            socket.setSoTimeout(1000);
            socket.receive(ackLengthFilePacket);
            System.out.println("ACK recebido para o pacote do tamanho do arquivo");

            while (unAckedSeqNum < numPackets) {
                // enviar pacotes ainda não confirmados e que estão dentro da janela
                while (nextSeqNum < (base + WINDOW_SIZE) && nextSeqNum < numPackets) {
                    int packetSize = nextSeqNum == numPackets - 1 ? lastPacketSize : sizeOfPacketWithoutSeqNum;
                    byte[] packetData = new byte[packetSize];
                    System.arraycopy(fileBytes, nextSeqNum * sizeOfPacketWithoutSeqNum, packetData, 0, packetSize);
                    if (nextSeqNum == 2) {
                        System.out.println(Arrays.toString(packetData));
                    }
                    ByteBuffer packetBuffer = ByteBuffer.allocate(packetSize + Integer.BYTES);
                    packetBuffer.putInt(nextSeqNum);
                    packetBuffer.put(packetData);
                    System.out.println(packetBuffer.array().length);
                    DatagramPacket packet = new DatagramPacket(packetBuffer.array(), packetBuffer.array().length, address, port);
                    socket.send(packet);
                    System.out.println("Enviando pacote #" + nextSeqNum);
                    nextSeqNum++;
                    packetsSent++;
                }

                // aguardar ACK dos pacotes enviados
                while (packetsSent > unAckedSeqNum) {
                    byte[] ackData = new byte[Integer.BYTES];
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                    try {
                        socket.setSoTimeout(10000); // tempo de espera para ACK
                        socket.receive(ackPacket);
                        int ackSeqNum;
                        try {
                            ackSeqNum = ByteBuffer.wrap(ackPacket.getData(), 0, ackPacket.getLength()).getInt();
                        } catch (BufferUnderflowException e) {
                            ackSeqNum = 0;
                        }
                        System.out.println("ACK recebido para o pacote #" + ackSeqNum);
                        if (ackSeqNum >= base) {
                            base = ackSeqNum + 1; // atualizar a base da janela
                        }
                    } catch (SocketTimeoutException e) {
                        // timeout expirado, reenviar pacotes ainda não confirmados
                        System.out.println("Timeout expirado. Reenviando pacotes...");
                        nextSeqNum = base;
                        packetsSent = 0;
                    }
                    unAckedSeqNum++;
                }
            }
            System.out.println("Arquivo \"" + fileName + "\" enviado para o cliente.");
        } else {
            System.out.println("Arquivo não encontrado: " + fileName);
        }
    }

    public static void receiveFile(String fileName, String filesDirectory, InetAddress address, int port, DatagramSocket socket) throws IOException {
        byte[] fileNameBytes = fileName.getBytes();
        DatagramPacket fileNamePacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, port);
        socket.send(fileNamePacket);

        byte[] fileSizeBytes = new byte[4];
        DatagramPacket fileSizePacket = new DatagramPacket(fileSizeBytes, fileSizeBytes.length);
        socket.receive(fileSizePacket);
        int fileSize = ByteBuffer.wrap(fileSizeBytes).getInt();

        DatagramPacket ackPacket = new DatagramPacket(new byte[] { 1 }, 1, address, port);
        socket.send(ackPacket);

        byte[] fileBytes = new byte[fileSize];

        int seqNum = 0;
        int windowBase = 0;
        int lastAckReceived = 0;
        boolean endOfFile = false;
        int sizeOfPacketWithoutSeqNum = PACKET_SIZE - 4;
        int numOfPacketsReceived = 0;

        int numPackets = (int) Math.ceil((double) fileSize / sizeOfPacketWithoutSeqNum);
        int lastPacketSize = fileSize % sizeOfPacketWithoutSeqNum;
        if (lastPacketSize == 0) {
            lastPacketSize = sizeOfPacketWithoutSeqNum;
        }

        // recebe os pacotes do servidor enquanto a janela deslizante não chega ao final do arquivo
        while (!endOfFile) {
            // recebe pacote do servidor
            byte[] packetBytes = new byte[PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(packetBytes, packetBytes.length);
            socket.receive(packet);

            numOfPacketsReceived++;

            // extrai informações do pacote (número de sequência, dados)
            int packetSeqNum = ByteBuffer.wrap(packetBytes, 0, 4).getInt();
            byte[] packetData = Arrays.copyOfRange(packetBytes, 4, packet.getLength());

            if (numOfPacketsReceived == 3) {
                System.out.println(Arrays.toString(packetData));
            }
            // atualiza janela deslizante
            if (packetSeqNum >= windowBase && packetSeqNum < (windowBase + WINDOW_SIZE)) {
                int dataOffset = packetSeqNum * sizeOfPacketWithoutSeqNum;
                System.arraycopy(packetData, 0, fileBytes, dataOffset, packetData.length);
                if (packetSeqNum == windowBase) {
                    int numPacketsProcessed = 1;
                    while (windowBase < fileSize / sizeOfPacketWithoutSeqNum && numPacketsProcessed < WINDOW_SIZE) {
                        if (windowBase + numPacketsProcessed == seqNum) {
                            break;
                        }
                        int nextPacketOffset = (windowBase + numPacketsProcessed) * sizeOfPacketWithoutSeqNum;
                        if (fileBytes[nextPacketOffset] != 0) {
                            numPacketsProcessed++;
                        } else {
                            break;
                        }
                    }
                    lastAckReceived = windowBase + numPacketsProcessed - 1;
                    windowBase += numPacketsProcessed;
                }
                if (numOfPacketsReceived == numPackets) {
                    endOfFile = true;
                }
            }

            // envia ack para o último pacote processado pela janela deslizante
            ackPacket = new DatagramPacket(intToBytes(lastAckReceived), 4, address, port);
            socket.send(ackPacket);
        }

        Path filePath = Paths.get(filesDirectory, fileName);
        Files.write(filePath, fileBytes);
        System.out.println(fileBytes.length);

        System.out.println("Arquivo \"" + fileName + "\" baixado do servidor e salvo no cliente.");
    }

    public static void receiveFile(String filesDirectory, InetAddress address, int port, DatagramSocket socket) throws IOException {
        byte[] fileNameBuffer = new byte[PACKET_SIZE];
        DatagramPacket fileNamePacket = new DatagramPacket(fileNameBuffer, fileNameBuffer.length);
        socket.receive(fileNamePacket);
        String fileName = new String(fileNamePacket.getData(), 0, fileNamePacket.getLength());

        DatagramPacket ackPacket = new DatagramPacket(new byte[] { 1 }, 1,  address, port);
        socket.send(ackPacket);

        receiveFile(fileName, filesDirectory, address, port, socket);
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }
}