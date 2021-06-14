package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * Контроллер подтверждения удаления файла\вложенных файлов
 */
public class MessagesController {
    @FXML
    private Label messageText;
    private Path filePath;

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public void setMessageText(String message) {
        messageText.setText("Are you sure want to delete " + filePath + " and " + message + " nested files?");
    }

    public void setMessageText() {
        messageText.setText("Are you sure want to delete " + filePath + "?");
    }

    /**
     * Инициализирует удаление выбранного файла
     */
    public void delete(ActionEvent actionEvent) {
        MainClient.sendMessage("rm " + filePath);
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
        //MainClient.sendMessage("ls " + filePath.getParent());
    }

    /**
     * Закрывает окно
     */
    public void close(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
