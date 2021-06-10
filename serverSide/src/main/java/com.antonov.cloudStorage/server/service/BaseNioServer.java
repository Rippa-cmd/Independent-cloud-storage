package com.antonov.cloudStorage.server.service;

import com.antonov.cloudStorage.server.interfaces.AuthService;
import com.antonov.cloudStorage.server.service.authentification.BaseAuthService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Основной класс серверной части облачного хранилища
 */
public class BaseNioServer {

    private final ByteBuffer buffer = ByteBuffer.allocate(512);
    private final Map<SocketAddress, Users> users = new ConcurrentHashMap<>();
    private AuthService authService;

    public BaseNioServer(int port) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        Selector selector = Selector.open();
        server.bind(new InetSocketAddress(port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        authService = new BaseAuthService();
        authService.start();

        System.out.println("Server started");

        // Получение информации от пользователей
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

    /**
     * Обработчик полученного сообщения от пользователя
     */
    private void handleRead(SelectionKey key, Selector selector) throws IOException {

        SocketChannel channel = ((SocketChannel) key.channel());
        SocketAddress client = channel.getRemoteAddress();

        if (checkDownloading(selector, channel, client)) return;

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

            // Авторизация пользователя, если не авторизован
            if (!users.containsKey(client)) {
                authentication(commands, selector, client);
                return;
            }

            if ("mkdir".equals(commands[0])) {
                    makeDirectory(commands[1], client);
            } else if ("rename".equals(commands[0])) {
                renameFile(commands[1], commands[2], channel);
            } else if ("ls".equals(commands[0])) {
                if (commands.length == 2)
                    listFiles(commands[1], channel);
                else
                    listFiles("", channel);
            } else if ("rm".equals(commands[0])) {
                if (commands.length == 2)
                    deleteFileOrDirectory(commands[1], channel);
            } else if ("upload".equals(commands[0])) {
                if (commands.length == 3)
                    uploadingToServer(commands[1], Long.parseLong(commands[2]), selector, client);
            } else if ("download".equals(commands[0])) {
                if (commands.length == 2) {
                    downloadingFromServer(commands[1], selector, channel);
                    return;
                }
            } else if ("exit".equals(commands[0])) {
                System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
                channel.close();
                return;
            }
        }
    }

    /**
     * Переименовывание выбранного файла или папки
     */
    private void renameFile(String path, String newName, SocketChannel channel) throws IOException {
        Users user = users.get(channel.getRemoteAddress());
        Path serverPath = user.getRootPath().resolve(path);
        Files.move(serverPath, serverPath.getParent().resolve(newName));
    }

    /**
     * Подготавливает и оправляет список файлов с информацией по запрашиваемому пути
     */
    private void listFiles(String path, SocketChannel channel) throws IOException {
        Users user = users.get(channel.getRemoteAddress());
        String serverPath;

        //если это корневой каталог пользователя, приводим к аблосютному пути сервера
        if (path.isEmpty())
            serverPath = user.getRootPath().toString();
        else
            serverPath = user.getRootPath().resolve(path).toString();

        String[] files = new File(serverPath).list();
        if (files != null) {
            // Подготовка списка файлов к отправке
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {

                ArrayList<ArrayList<String>> fullList = new ArrayList<>();

                // Собирание необходимой информации о каждом файле
                for (String fileName : files) {

                    ArrayList<String> list = new ArrayList<>();
                    String nestedFile = serverPath + File.separator + fileName;
                    FileSizeAndNestedFilesCount info = showFileSize(nestedFile);

                    //имя файла
                    list.add(fileName);
                    //размер файла
                    list.add(info.size);
                    //дата создания файла
                    list.add(showCreationTime(nestedFile));
                    //информация о типе файла (файл или папка)
                    if (Files.isDirectory(Path.of(nestedFile))) {
                        list.add("D");
                        list.set(1, "");
                    }
                    else
                        list.add("F");
                    //кол-во вложенных файлов
                    list.add("" + info.count);
                    fullList.add(list);
                }
                //перевод списка файлов с информации в байты
                oos.writeObject(fullList);
                oos.flush();

                //отправка списка файлов клиенту
                channel.write(ByteBuffer.wrap(("ls " + baos.size() + " ").getBytes(StandardCharsets.UTF_8)));
                channel.write(ByteBuffer.wrap(baos.toByteArray()));
            }
        }
    }

    /**
     * Возвращает время создания файла
     */
    private String showCreationTime(String path) throws IOException {
        Path serverPath = Path.of(path);

        BasicFileAttributes attr = Files.readAttributes(serverPath, BasicFileAttributes.class);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        String date = df.format(attr.creationTime().toMillis());
        return date;
    }

    /**
     * Возвращает объект с размером файла (вложенных файлов) и количеством вложенных файлов
     */
    private FileSizeAndNestedFilesCount showFileSize(String path) {
        Path serverPath = Path.of(path);
        final AtomicLong byteSize = new AtomicLong();
        final AtomicInteger count = new AtomicInteger();
        try {
            Files.walkFileTree(serverPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    byteSize.addAndGet(attrs.size());
                    count.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.out.println("skipped: " + file + " (" + exc + ")");
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    return FileVisitResult.CONTINUE;
                }
            });

            // перевод байтов в "читабельный вид"

            int unit = 1024;
            long finalSize = byteSize.get();
            StringBuilder size = new StringBuilder();

            if (finalSize < unit) {
                size.append(finalSize).append(" B");
                //sendMessage(size.toString(), selector, client);
                return new FileSizeAndNestedFilesCount(size.toString(), count.get());
            }
            int exp = (int) (Math.log(finalSize) / Math.log(unit));
            char pre = ("kMGTPE").charAt(exp - 1);
            size.append(String.format("%d (%.2f %sB)",finalSize, finalSize / Math.pow(unit, exp), pre));
            return new FileSizeAndNestedFilesCount(size.toString(), count.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Удаление выбранного файла (вложенных файлов)
     */
    private void deleteFileOrDirectory(String path, SocketChannel client) {
        try {
            Path serverPath = users.get(client.getRemoteAddress()).getRootPath().resolve(Path.of(path));

            Files.walkFileTree(serverPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Проверка ожидания получения файла от клиента
     */
    private boolean checkDownloading(Selector selector, SocketChannel channel, SocketAddress client) throws IOException {
        if (users.containsKey(client))
            if (users.get(client).getDownloadingStatus()) {
                if (users.get(client).write(channel))
                    sendMessage("The download was successful", selector, client);
                return true;
            }
        return false;
    }

    /**
     * Создание директорий
     */
    private void makeDirectory(String path, SocketAddress client) throws IOException {
        Path serverPath = users.get(client).getRootPath();
        Files.createDirectories(serverPath.resolve(path));
    }

    /**
     * Загрузка файла на сервер
     *
     * @param pathToFile
     * @param fileSize
     * @param selector
     * @param client
     * @throws IOException
     */
    private void uploadingToServer(String pathToFile, Long fileSize, Selector selector, SocketAddress client) throws IOException {
        Users user = users.get(client);
        Path serverPath = user.getRootPath().resolve(Path.of(pathToFile));
        if (Files.exists(serverPath)) {
            sendMessage("already exists", selector, client);
        } else {
            sendMessage("UReady", selector, client);
            user.initFileReceiver(serverPath, fileSize);
        }
    }

    private void downloadingFromServer(String pathToFile, Selector selector, SocketChannel channel) throws IOException {
        SocketAddress client = channel.getRemoteAddress();
        Users user = users.get(client);
        Path serverPath = user.getRootPath().resolve(Path.of(pathToFile));
        if (Files.exists(serverPath)) {
            long fileSize = user.initFileSender(serverPath);
            sendMessage("DReady " + fileSize, selector, client);
            user.startSending(channel);
        } else {
            sendMessage("This file doesn't exist", selector, client);
        }
    }

    /**
     * Класс авторизации пользователя todo обработать мультиподключение
     */
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
                sendMessage("good " + nick, selector, client);
                System.out.println("Client logged in by " + nick);
                return true;
            }
        }
        sendMessage("Wrong credentials\n\r", selector, client);
        return false;
    }

    /**
     * Отправка сообщения клиенту
     */
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

    /**
     * Обработчик подключения клиента
     */
    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());

        channel.register(selector, SelectionKey.OP_READ, "some attach");
    }
}
