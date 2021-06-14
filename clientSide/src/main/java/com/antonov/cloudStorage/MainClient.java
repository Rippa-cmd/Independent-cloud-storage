package com.antonov.cloudStorage;

import com.antonov.cloudStorage.controllers.ServerExplorerController;
import com.antonov.cloudStorage.handlers.DownloadHandler;
import com.antonov.cloudStorage.handlers.UploadHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainClient extends Application {


    private static SocketChannel myClient;
    private final ByteBuffer myBuffer = ByteBuffer.allocate(5120);
    private ServerExplorerController controller;
    private int lsBytes = 0;
    public static UploadHandler uploadHandler;
    public static DownloadHandler downloadHandler;
    private boolean isAuthorized = false;
    public static boolean uploadStatus = false;
    public static boolean downloadStatus = false;


    @Override
    public void start(Stage primaryStage) throws Exception {
        InetSocketAddress myAddress = new InetSocketAddress("localhost", 5678);
        myClient = SocketChannel.open(myAddress);
        myClient.configureBlocking(false);

        createAuthenticationWindow(primaryStage);

        /*
         * Поток получения данных от сервера
         */

        new Thread(() -> {
            while (true) {
                try {
                    if (myClient.read(myBuffer) > 0) {
                        myBuffer.flip();

                        StringBuilder sb = new StringBuilder();

                        // Дополнительная проверка для отсечения первых байтов команд
                        int spaceCommandCount = 0;
                        while (myBuffer.hasRemaining()) {
                            char symbol = (char) myBuffer.get();
                            if (symbol == ' ')
                                spaceCommandCount++;
                            if (spaceCommandCount > 1)
                                break;
                            sb.append(symbol);
                        }

                        String cmd = sb.toString();
                        if (!isAuthorized) {
                            authorization(primaryStage, cmd);
                            myBuffer.clear();
                            continue;
                        }

                        // Если есть запросы на загрузку, или скачивание файлов
                        if (uploadStatus) {
                            uploading(cmd);
                            continue;
                        } else if (downloadStatus) {
                            downloading(cmd);
                            continue;
                        }

                        // Если пришел список файлов
                        if (cmd.startsWith("ls")) {
                            String[] count = cmd.split(" ");
                            lsBytes = Integer.parseInt(count[1]);
                            if (lsBytes != 0) {
                                getFilesList(false);
                            }
                        } else if (cmd.startsWith("srchls")) {
                            String[] count = cmd.split(" ");
                            lsBytes = Integer.parseInt(count[1]);
                            if (lsBytes != 0) {
                                getFilesList(true);
                            }
                        }

                        myBuffer.clear();
                    }
                } catch (IOException e) {
                    System.out.println("Connection lost");
                    e.printStackTrace();
                    try {
                        myClient.close();
                        break;
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Завершение процесса авторизации и допуск к файловой системе
     */
    private void authorization(Stage primaryStage, String cmd) {
        if (cmd.startsWith("good")) {
            String[] nickname = cmd.split(" ");
            Platform.runLater(() -> {
                showExplorer(primaryStage);
                //controller.setNick(nickname[1]); todo
                sendMessage("ls ");
            });
            isAuthorized = true;
            myBuffer.clear();
        }
    }

    /**
     * Получение ответа от сервера на запрос загрузки файла
     */
    private void downloading(String cmd) {
        if (cmd.startsWith("DReady")) {
            String[] size = cmd.split(" ");
            downloadHandler.startDownloading(size[1], myClient);
            System.out.println("Successful");


        } else {
            downloadHandler.close();
        }
        downloadStatus = false;
        myBuffer.clear();
    }

    /**
     * Получение ответа от сервера на запрос отправки файла
     */
    private void uploading(String cmd) {
        if ("UReady".equals(cmd)) {
            try {
                uploadHandler.initUploading(myClient);
            } catch (IOException e) {
                System.out.println("Something went wrong when loading");
                e.printStackTrace();
            }
        } else {
            uploadHandler.close();
        }
        uploadStatus = false;
        myBuffer.clear();
    }

    /**
     * Получение от сервера списка файлов
     */
    private void getFilesList(boolean isFoundedFiles) throws IOException, ClassNotFoundException {
        ByteBuffer FilesListBuf = ByteBuffer.allocate(lsBytes);
        while (FilesListBuf.position() != lsBytes) {
            if (!myBuffer.hasRemaining()) {
                myBuffer.clear();
                myClient.read(myBuffer);
                myBuffer.flip();
            } else {
                FilesListBuf.put(myBuffer);
                myBuffer.clear();
            }
        }
        FilesListBuf.flip();
        ByteArrayInputStream bais = new ByteArrayInputStream(FilesListBuf.array());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ArrayList<ArrayList<String>> FilesList = (ArrayList<ArrayList<String>>) ois.readObject();
        if (isFoundedFiles)
            Platform.runLater(() -> controller.showFoundedFiles(FilesList));
        else
            Platform.runLater(() -> controller.showFiles(FilesList));
        lsBytes = 0;
        ois.close();
        bais.close();
    }

    /**
     * Создание окна облачного проводника
     */
    private void showExplorer(Stage primaryStage) {
        primaryStage.close();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/serverExplorerManager.fxml"));
        Parent root = null;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        primaryStage.setTitle("Cloud storage");
        primaryStage.setScene(new Scene(root, 676, 400));
        controller = fxmlLoader.getController();
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    /**
     * Создание формы для авторизациии
     */
    private void createAuthenticationWindow(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/authentication.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Authentication");
        primaryStage.setScene(new Scene(root, 300, 200));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Отправляет выбранное сообщение на сервер
     */
    public static void sendMessage(String message) {
        try {
            myClient.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Запуск программы
     */
    public static void main(String[] args) {
        launch(args);
    }
}
