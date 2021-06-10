package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * Контроллер формы ввода нового имени файла
 */
public class RenamingMessageController {
    @FXML
    TextField fileNameField;
    private Path curPath;

    public void setCurPath(Path curPath) {
        this.curPath = curPath;
        fileNameField.setText(curPath.getFileName().toString());
    }

    /**
     * Проверяет валидность имени, после чего отправляет запрос серверу на смену имени файла
     */
    public void rename(ActionEvent actionEvent) {
        String newName = fileNameField.getText();
        if (!newName.isEmpty() && !newName.equals(curPath.getFileName().toString()))
            MainClient.sendMessage("rename " + curPath + " " + newName);
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
