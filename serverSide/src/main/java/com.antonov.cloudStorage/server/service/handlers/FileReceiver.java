package com.antonov.cloudStorage.server.service.handlers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Класс загрузки файла
 */
public class FileReceiver {
    private FileChannel fileChannel;
    private final long fileSize;

    public FileReceiver(Path finalPath, long fileSize) {
        this.fileSize = fileSize;
        try {
            fileChannel = FileChannel.open(finalPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Продолжает запись получаемого файла
     */
    public boolean continueReading(SocketChannel client) {
        try {
            fileChannel.transferFrom(client, fileChannel.size(), fileSize);
            System.out.println(fileChannel.size());
            if (fileChannel.size() >= fileSize) {
                fileChannel.close();
                System.out.println("Stopped");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
