package com.antonov.cloudStorage.server.service;

/**
 * Класс хранящий информацию и размере и количестве вложенных файлов файла
 */
public class FileSizeAndNestedFilesCount {
    protected String size;
    protected int count;

    public FileSizeAndNestedFilesCount(String size, int count) {
        this.size = size;
        this.count = count;
    }
}
