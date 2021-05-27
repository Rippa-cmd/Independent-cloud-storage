package com.antonov.cloudStorage.server.service;

import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * Класс пользователя
 */
public class Users {
    private String nickname;
    private Path rootPath;
    private Path curPath;
    private FileWriter fileWriter;
    private boolean writeStatus = false;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public Path getCurPath() {
        return curPath;
    }

    public void setCurPath(Path curPath) {
        this.curPath = curPath;
    }

    public boolean getWriteStatus() {
        return writeStatus;
    }

    public void initFileWriter(Path finalPath, long fileSize) {
        fileWriter = new FileWriter(finalPath, fileSize);
        writeStatus = true;
    }

    public boolean write(SocketChannel client) {
        if (fileWriter.continueWrite(client)) {
            writeStatus = false;
            fileWriter = null;
            return true;
        }
        return false;
    }

    public Users(String nickname, Path rootPath) {
        this.nickname = nickname;
        this.rootPath = rootPath;
        curPath = rootPath;
    }
}
