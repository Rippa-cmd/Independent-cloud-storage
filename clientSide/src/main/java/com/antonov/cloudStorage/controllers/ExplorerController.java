package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Контроллер главного окна проваодника, отображающий хранящиеся файлы на сервере и позволяющий с ним взаимодействовать
 */
public class ExplorerController implements Initializable {


    @FXML
    protected TableView<ArrayList<String>> filesTable;

    @FXML
    TextField pathFiled;

    @FXML
    TextField nicknameField;

    protected Path curPath = Paths.get("");

    /**
     * Выход по кнопке
     */
    public void menuItemFileExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }


    /**
     * Задает начальные параметры проводника
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        TableColumn<ArrayList<String>, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(3)));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<ArrayList<String>, String> filenameColumn = new TableColumn<>("Name");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0)));
        filenameColumn.setPrefWidth(300);

        TableColumn<ArrayList<String>, String> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(1)));
        fileSizeColumn.setPrefWidth(200);
//        fileSizeColumn.setComparator((o1, o2) -> {
//            String[] first = o1.split(" ");
//            String[] second = o2.split(" ");
//            return new Long(Long.parseLong(first[0]) - Long.parseLong(second[0])).intValue();
//        });
//        fileSizeColumn.setCellFactory(column -> new TableCell<ArrayList<String>, String>() {
//            @Override
//            protected void updateItem(String item, boolean empty) {
//                super.updateItem(item, empty);
//                if (item == null || empty) {
//                    setText(null);
//                    setStyle("");
//                } else {
//                    setText(item);
//                }
//            }
//        });

        TableColumn<ArrayList<String>, String> fileCreationDateColumn = new TableColumn<>("Date");
        fileCreationDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(2)));
        fileCreationDateColumn.setPrefWidth(150);


//        filesTable.setCellFactory(new Callback<ListView<ArrayList<String>>, ListCell<ArrayList<String>>>() {
//            @Override
//            public ListCell<ArrayList<String>> call(ListView<ArrayList<String>> param) {
//                return new ListCell<ArrayList<String>>() {
//                    @Override
//                    protected void updateItem(ArrayList<String> item, boolean empty) {
//                        super.updateItem(item, empty);
//                        if (item == null || empty) {
//                            setText(null);
//                            setStyle("");
//                        } else {
//                            String formattedFilename = String.format("%-30s", item.get(0));
//                            String formattedFileLength = String.format("%s", item.get(1));
//                            if ("dir".equals(item.get(3))) {
//                                formattedFileLength = String.format("%s", "[ DIR ]");
//                            }
//
//                            String text = String.format("%s %-20s", formattedFilename, formattedFileLength);
//
//                            setText(text);
//                        }
//                    }
//                };
//            }
//        });
        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileCreationDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);
    }

    // todo warning message stage

    /**
     * Отображает полученный список файлов с сервера
     */
    public void showFiles(ArrayList<ArrayList<String>> list) {
        pathFiled.setText("~" + File.separator + curPath.toString());
        filesTable.getItems().clear();
        filesTable.getItems().addAll(list);
    }

    /**
     * Переходит в выбранную папку
     */
    public void filesListClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            ArrayList<String> file = filesTable.getSelectionModel().getSelectedItem();
            if (file != null) {
                if ("D".equals(file.get(3))) {
                    MainClient.sendMessage("ls " + curPath.resolve(file.get(0)));
                    curPath = curPath.resolve(file.get(0));
                }
            }
        }
    }

    /**
     * Возвращает к корневой каталог
     */
    public void goHome(ActionEvent actionEvent) {
        curPath = Paths.get("");
        MainClient.sendMessage("ls " + curPath);
    }

    /**
     * Переходит в родительскую директорию (по пути выше)
     */
    public void goUp(ActionEvent actionEvent) {
        if ("".equals(curPath.toString()))
            return;
        curPath = curPath.getParent();
        if (curPath == null)
            curPath = Paths.get("");
        MainClient.sendMessage("ls " + curPath);
    }

    /**
     * Отображение никнейма пользователя
     */
    public void setNick(String nickname) {
        nicknameField.setText(nickname);
    }

    /**
     * Метод рекурсивного удаления файла\файлов
     */
    public void remove(ActionEvent actionEvent) throws IOException {
        ArrayList<String> file = filesTable.getSelectionModel().getSelectedItem();
        if (file == null) {
            return;
        }
        Path fileName = curPath.resolve(file.get(0));
        System.out.println(fileName);

        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/confirmationMessage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("Confirmation");
        stage.setScene(new Scene(root, 300, 150));
        stage.setResizable(false);

        MessagesController controller = fxmlLoader.getController();
        controller.setFilePath(fileName);
        if ("dir".equals(file.get(3)))
            controller.setMessageText(file.get(4));
        else
            controller.setMessageText();
        filesTable.getSelectionModel().clearSelection();
        stage.show();
    }

    /**
     * Метод создания папки
     */
    public void createDir(ActionEvent actionEvent) throws IOException {
        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/newDirNameMessage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("Creation directory");
        stage.setScene(new Scene(root, 300, 150));
        stage.setResizable(false);
        NewDirNameController controller = fxmlLoader.getController();
        controller.setCurPath(curPath);
        stage.show();
    }

    /**
     * Обновление списка файлов
     */
    public void reload(ActionEvent actionEvent) {
        MainClient.sendMessage("ls " + curPath);
    }

    /**
     * Метод загрузки файла на сервер
     */
    public void uploadToServer(ActionEvent actionEvent) throws IOException {
        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/uploadMessage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("Downloading directory");
        stage.setScene(new Scene(root, 300, 150));
        stage.setResizable(false);
        UploadMessageController controller = fxmlLoader.getController();
        controller.setCurPath(curPath);
        stage.show();
    }

    /**
     * Метод загрузки выбранного файла с сервера
     */
    public void downloadFromServer(ActionEvent actionEvent) throws IOException {
        ArrayList<String> file = filesTable.getSelectionModel().getSelectedItem();
        if (file == null) {
            return;
        }

        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/downloadMessage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("Uploading directory");
        stage.setScene(new Scene(root, 300, 150));
        stage.setResizable(false);

        String selectedFileName = file.get(0);
        DownloadMessageController controller = fxmlLoader.getController();
        controller.setCurPath(curPath.resolve(selectedFileName));
//        Stage mainStage = ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow());
//        stage.initModality(Modality.WINDOW_MODAL);
//        stage.initOwner(mainStage);
        stage.show();
    }

    /**
     * Метод переименовывания выбранного файла или папки
     */
    public void rename(ActionEvent actionEvent) throws IOException {
        ArrayList<String> file = filesTable.getSelectionModel().getSelectedItem();
        if (file == null) {
            return;
        }
        String fileName = file.get(0);

        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/renamingMessage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("Rename file");
        stage.setScene(new Scene(root, 300, 150));
        stage.setResizable(false);
        RenamingMessageController controller = fxmlLoader.getController();
        controller.setCurPath(curPath.resolve(fileName));
        stage.show();
    }
}
