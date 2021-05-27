package com.antonov.cloudStorage.server.service;

import com.antonov.cloudStorage.server.interfaces.AuthService;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BaseNioServer {

    private final ByteBuffer buffer = ByteBuffer.allocate(512);
    private final Map<SocketAddress, Users> users = new ConcurrentHashMap<>();
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public BaseNioServer(int port) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        Selector selector = Selector.open();
        server.bind(new InetSocketAddress(port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        authService = new BaseAuthService();
        authService.start();

        System.out.println("Server started");

        while (server.isOpen()) {
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {

        SocketChannel channel = ((SocketChannel) key.channel());
        SocketAddress client = channel.getRemoteAddress();

        // если от клиента ожидается получение файла
        if (users.containsKey(client))
            if (users.get(client).getWriteStatus()) {
                if (users.get(client).write(channel))
                    sendMessage("The download was successful", selector, client);
                return;
            }

        int readBytes = 0;
        try {
            readBytes = channel.read(buffer);
        } catch (IOException ignored) {
            System.out.println("Something gone wrong");
            System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
            channel.close();
        }

        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();

        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }

        buffer.clear();

        if (key.isValid()) {
            String[] commands = sb
                    .toString()
                    .replace("\n", "")
                    .replace("\r", "").split(" ");

            if (!users.containsKey(client)) {
                if (!authentication(commands, selector, client))
                    return;
            }

            if ("cd".equals(commands[0])) {
                if (commands.length == 2)
                changeDirectory(commands[1], selector, client);
            } else if ("download".equals(commands[0])) {
                if (commands.length == 3)
                downloading(commands[1], Long.parseLong(commands[2]), selector, client);
            } else if ("exit".equals(commands[0])) {
                System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
                channel.close();
                return;
            }

            newLine(selector, client);
        }
    }

    private void downloading(String pathToFile, Long fileSize, Selector selector, SocketAddress client) throws IOException {
        Users user = users.get(client);
        Path finalPath = user.getCurPath().resolve(Path.of(pathToFile));
        if (Files.exists(finalPath)) {
            sendMessage("This file is already exists", selector, client);
        } else {
            sendMessage("DReady", selector, client);
            user.initFileWriter(finalPath, fileSize);
        }
    }

    /**
     * For telnet console
     *
     */
    private void newLine(Selector selector, SocketAddress client) throws IOException {
        Path curPath = users.get(client).getCurPath();
        Path rootPath = users.get(client).getRootPath();
        if (curPath.equals(rootPath))
            sendMessage(users.get(client).getNickname() + ":~ ", selector, client);
        else {
            curPath = rootPath.relativize(curPath);
            sendMessage(users.get(client).getNickname() + ":" + curPath + File.separator + "$ ", selector, client);
        }
    }

    private void changeDirectory(String path, Selector selector, SocketAddress client) throws IOException {
        Path curPath = users.get(client).getCurPath();
        Path rootPath = users.get(client).getRootPath();
        if ("..".equals(path)) {
            if (!curPath.equals(rootPath)) {
                users.get(client).setCurPath(curPath.getParent());
            } else
                sendMessage("You are already in root folder\n\r", selector, client);
            return;
        } else if ("~".equals(path)) {
            users.get(client).setCurPath(rootPath);
            return;
        }


        Path newPath = curPath.resolve(path);
        if (Files.exists(newPath))
            users.get(client).setCurPath(newPath);
        else
            sendMessage("No such directory\n\r", selector, client);
    }

    private boolean authentication(String[] msg, Selector selector, SocketAddress client) throws IOException {
        if (msg.length == 2) {
            List<String> idAndNick = authService.getIdAndNicknameByLoginAndPassword(msg[0], msg[1]);
            if (idAndNick != null) {
                String id = idAndNick.get(0);
                String nick = idAndNick.get(1);
                Path serverPath = Path.of("server", "root" + id);
                users.put(client, new Users(nick, serverPath));
                if (!Files.exists(serverPath))
                    Files.createDirectories(serverPath);
                sendMessage("Welcome, " + nick + "!\n\r", selector, client);
                sendMessage("Enter --help for support info\n\r", selector, client);
                return true;
            }
        }
        sendMessage("Wrong credentials\n\r", selector, client);
        return false;
    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel())
                            .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());

        channel.register(selector, SelectionKey.OP_READ, "some attach");
        channel.write(ByteBuffer.wrap(("Hello, user!\n\r").getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(("Enter your login and password to log in\n\r").getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(("in format \"login password\"\n\r").getBytes(StandardCharsets.UTF_8)));
    }
}
