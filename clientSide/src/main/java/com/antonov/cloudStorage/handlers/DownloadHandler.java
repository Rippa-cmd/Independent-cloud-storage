package com.antonov.cloudStorage.handlers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Класс для получения файла
 */
public class DownloadHandler {
    private FileChannel fileChannel;
    private long fileSize;

    public DownloadHandler(Path path) throws IOException {
        fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * Запускает процесс получения файла от сервера
     */
    public void startDownloading(String size, SocketChannel channel) {
        fileSize = Long.parseLong(size);
        try {
            do {
                fileChannel.transferFrom(channel, fileChannel.size(), fileSize);
                System.out.println(fileChannel.size());
            } while (fileChannel.size() != fileSize);
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
