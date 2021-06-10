package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import com.antonov.cloudStorage.handlers.UploadHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Контроллер формы ввода отправляемого файла
 */
public class UploadMessageController {
    @FXML
    private TextField dirPathField;
    private Path curPath;

    public void setCurPath(Path curPath) {
        this.curPath = curPath;
    }

    /**
     * Проверяет валидность пути, после чего отправляет запрос серверу на отправку файла
     */
    public void prepareUploadingFile(ActionEvent actionEvent) {
        String name = dirPathField.getText();
        if (name.isEmpty()) {
            ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
            return;
        }
        Path path = Paths.get(name);
        if (Files.exists(path)) {
                try {
                    MainClient.uploadHandler = new UploadHandler(path);
                    long size = MainClient.uploadHandler.getFileSize();
                    MainClient.sendMessage("upload " + curPath.resolve(path.getFileName()) + " " + size);
                    MainClient.uploadStatus = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
