package com.antonov.cloudStorage.server;

import com.antonov.cloudStorage.server.service.BaseNioServer;

import java.io.IOException;

public class MainServerApp {
    public static void main(String[] args) throws IOException {
        new BaseNioServer(5678);
    }
}
