package com.antonov.cloudStorage.server.service.handlers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * Класс отправления файла клиенту
 */
public class FileSender {
    private FileChannel fileChannel;
    private long fileSize;

    public long getFileSize() {
        return fileSize;
    }

    public FileSender(Path serverPath) {
        try {
            fileChannel = FileChannel.open(serverPath);
            fileSize = fileChannel.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Начинает отправку файла клиенту
     */
    public void startUploading(SocketChannel channel) {
        new Thread(() -> {
            try {
                long curBytes = 0;
                do {
                    curBytes += fileChannel.transferTo(curBytes, fileSize, channel);
                }
                while (curBytes != fileSize);
                fileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
