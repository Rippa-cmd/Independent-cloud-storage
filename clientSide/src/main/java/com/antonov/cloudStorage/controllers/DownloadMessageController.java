package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import com.antonov.cloudStorage.handlers.DownloadHandler;
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
 * Контроллер формы ввода желаемого пути для сохранения скачиваемого файла
 */
public class DownloadMessageController {
    @FXML
    TextField dirPathField;
    private Path curPath;

    public void setCurPath(Path curPath) {
        this.curPath = curPath;
    }

    /**
     * Проверяет валидность имени, после чего отправляет запрос серверу на получение файла
     */
    public void prepareDownloadingFile(ActionEvent actionEvent) {
        String folder = dirPathField.getText();
        //if (Paths.get(folder).resolve(curPath.getFileName()))

        if (folder.isEmpty()) {
            ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
            return;
        }

        Path folderPath = Paths.get(folder).resolve(curPath.getFileName());

        if (!Files.exists(folderPath)) {
            try {
                MainClient.downloadHandler = new DownloadHandler(folderPath);
                MainClient.sendMessage("download " + curPath);
                MainClient.downloadStatus = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
