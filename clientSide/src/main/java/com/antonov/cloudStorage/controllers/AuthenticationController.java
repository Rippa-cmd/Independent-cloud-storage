package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Контроллер формы ввода логина и пароля для авторизации
 */
public class AuthenticationController {
    @FXML
    PasswordField passwordField;
    @FXML
    TextField loginField;

    /**
     * Отправляет запрос серверу для входа в систему под введенными данными
     */
    public void tryLogin(ActionEvent actionEvent) {
        if (!loginField.getText().isEmpty() || !passwordField.getText().isEmpty()) {
            String logPass = loginField.getText() + " " + passwordField.getText();
            MainClient.sendMessage(logPass);
        }
        loginField.clear();
        passwordField.clear();
    }
}
