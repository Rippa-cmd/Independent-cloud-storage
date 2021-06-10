package com.antonov.cloudStorage.handlers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * Класс для отправки файла
 */
public class UploadHandler {
    private FileChannel fileChannel;
    private long fileSize;

    public UploadHandler(Path path) throws IOException {
        fileChannel = FileChannel.open(path);
        fileSize = fileChannel.size();
    }

    /**
     * Начинает отправку файла на сервер
     */
    public void initUploading(SocketChannel client) throws IOException {
        long curBytes = 0;
        do {
            curBytes += fileChannel.transferTo(curBytes, fileSize, client);
        }
        while (curBytes != fileSize);

    }

    public long getFileSize() throws IOException {
        return fileSize;
    }

    public void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
