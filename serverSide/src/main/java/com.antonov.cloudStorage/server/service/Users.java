package com.antonov.cloudStorage.server.service;

import com.antonov.cloudStorage.server.service.handlers.FileReceiver;
import com.antonov.cloudStorage.server.service.handlers.FileSender;

import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * Класс хранящий информацию о подключенном пользователе
 */
public class Users {
    private String nickname;
    private final Path rootPath;
    private FileReceiver fileReceiver;
    private FileSender fileSender;
    private boolean downloadingStatus = false;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public boolean getDownloadingStatus() {
        return downloadingStatus;
    }


    /**
     * Инифиация получения файла от клиента
     */
    public void initFileReceiver(Path finalPath, long fileSize) {
        fileReceiver = new FileReceiver(finalPath, fileSize);
        downloadingStatus = true;
    }

    /**
     * Инициация отправки файла клиенту
     */
    public long initFileSender(Path finalPath) {
        fileSender = new FileSender(finalPath);
        return fileSender.getFileSize();
    }

    /**
     * Запускает отправку файла
     */
    public void startSending(SocketChannel channel) {
        fileSender.startUploading(channel);
    }

    /**
     * Записывает получаемый файл
     */
    public boolean write(SocketChannel client) {
        if (fileReceiver.continueReading(client)) {
            downloadingStatus = false;
            return true;
        }
        return false;
    }

    public Path getReceivedFilePath () {
        return fileReceiver.getFilePath();
    }

    public void closeFileReceiver() {
        fileReceiver = null;
    }

    public Users(String nickname, Path rootPath) {
        this.nickname = nickname;
        this.rootPath = rootPath;
    }
}
