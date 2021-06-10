package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * Контроллер формы ввода имени создаваемой папки
 */
public class NewDirNameController {
    @FXML
    TextField dirNameField;
    private Path curPath;

    public void setCurPath(Path curPath) {
        this.curPath = curPath;
    }

    /**
     * Проверяет валидность имени, после чего отправляет запрос серверу на создание папки
     */
    public void createDir(ActionEvent actionEvent) {
        String dirName = dirNameField.getText();
        if (!dirName.isEmpty())
            MainClient.sendMessage("mkdir " + curPath.resolve(dirName));
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
