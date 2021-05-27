package com.antonov.cloudStorage.client;

import com.antonov.cloudStorage.client.handlers.UploadHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;

/**
 * Swing client - Cloud Storage (temporary graphic telnet)
 * Client commands:
 * download "filename path" "filename server path"
 * cd path
 */

public class Client extends JFrame {
    private final ByteBuffer myBuffer = ByteBuffer.allocate(512);
    private static SocketChannel myClient;
    private JTextField msgInputField;
    private JTextArea chatArea;
    private UploadHandler uploadHandler;
    private boolean uploadStatus = false;

    public Client() throws IOException {
        // init
        InetAddress hostIP = InetAddress.getLocalHost();
        InetSocketAddress myAddress = new InetSocketAddress(hostIP, 5678);
        myClient = SocketChannel.open(myAddress);
        myClient.configureBlocking(false);

        // create form
        setBounds(500, 500, 700, 300);
        setTitle("Chat Frame");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        chatArea = new JTextArea(); // Поле отображения сообщений
        chatArea.setEditable(false); // Не изменяемо
        chatArea.setLineWrap(true); // Перенос строки

        // Делаем возможность скролла и добавляем его
        JScrollPane jsp = new JScrollPane(chatArea);
        add(jsp, BorderLayout.CENTER);

        // Создаем нижнюю панель с формой ввода сообщения и кнопкой отправки
        JPanel bottomPanel = new JPanel(new BorderLayout());
        add(bottomPanel, BorderLayout.SOUTH);

        msgInputField = new JTextField();
        bottomPanel.add(msgInputField, BorderLayout.CENTER);

        // Навешиваем слушателя на нажатие Enter'a
        msgInputField.addActionListener(e -> checkMessage());

        // Слушатель на нажатие кнопки
        JButton button = new JButton("Send");
        bottomPanel.add(button, BorderLayout.EAST);

        button.addActionListener(a -> {
            checkMessage();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                sendMessage("exit", myClient);
            }
        });

        new Thread(() -> {
            while (true) {
                try {
                    if (myClient.read(myBuffer) > 0) {
                        myBuffer.flip();
                        StringBuilder sb = new StringBuilder();
                        while (myBuffer.hasRemaining()) {
                            sb.append((char) myBuffer.get());
                        }
                        myBuffer.clear();
                        String cmd = sb.toString();
                        if (uploadStatus) {
                            if ("DReady".equals(cmd)) {
                                uploading();
                                continue;
                            } else
                                cancelUploading();
                            uploadStatus = false;
                        }
                        chatArea.append(cmd);
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
                }
            }
        }).start();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);

        //sendMessage("bob 12345", myClient); // autologin
    }

    private void cancelUploading() {
        uploadHandler.close();
    }

    private void checkMessage() {
        String cmd = msgInputField.getText();
        if (cmd.startsWith("download")) {
            prepareUploading(cmd);
        } else
            sendMessage(msgInputField.getText(), myClient);
    }

    private void prepareUploading(String cmd) {
        String[] paths = cmd.split(" ");
        if (paths.length == 3)
            try {
                uploadHandler = new UploadHandler(Paths.get(paths[1]));
                long size = uploadHandler.getFileSize();
                sendMessage(paths[0] + " " + paths[2] + " " + size, myClient);
                uploadStatus = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    private void uploading() {
        try {
            uploadHandler.initUploading(myClient);
        } catch (IOException e) {
            System.out.println("something went wrong when loading");
            e.printStackTrace();
        }

    }

    /**
     * message sending
     *
     * @param message String
     */
    private void sendMessage(String message, SocketChannel myClient) {
        try {
            myBuffer.put(message.getBytes());
            myBuffer.flip();
            myClient.write(myBuffer);
            myBuffer.clear();
            msgInputField.setText("");
            msgInputField.grabFocus();
        } catch (EOFException eofException) {
            try {
                System.err.println("Reading command error from " + myClient.getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new Client();
    }
}
