package com.antonov.cloudStorage.controllers;

import com.antonov.cloudStorage.MainClient;
import com.antonov.cloudStorage.handlers.DownloadHandler;
import com.antonov.cloudStorage.handlers.UploadHandler;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
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
public class ServerExplorerController implements Initializable {


    @FXML
    protected TableView<ArrayList<String>> filesTable;

    @FXML
    TextField pathFiled;

    @FXML
    TextField searchField;

    protected Path curPath = Paths.get("");
    private boolean isFoundedFiles = false;

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
                    Path serverPath;
                    if (isFoundedFiles) {
                        serverPath = Paths.get(file.get(5));
                        isFoundedFiles = false;
                        curPath = serverPath;
                    }
                    else {
                        serverPath = curPath.resolve(file.get(0));
                        curPath = curPath.resolve(file.get(0));
                    }
                    MainClient.sendMessage("ls " + serverPath);

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
        isFoundedFiles = false;
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
        isFoundedFiles = false;
    }

    /**
     * Отображение никнейма пользователя
     */
//    public void setNick(String nickname) {
//        nicknameField.setText(nickname);
//    }

    /**
     * Метод рекурсивного удаления файла\файлов
     */
    public void remove(ActionEvent actionEvent) throws IOException {
        ArrayList<String> file = filesTable.getSelectionModel().getSelectedItem();
        if (file == null) {
            return;
        }

        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/confirmationMessage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("Confirmation");
        stage.setScene(new Scene(root, 300, 150));
        stage.setResizable(false);

        MessagesController controller = fxmlLoader.getController();
        if (isFoundedFiles)
            controller.setFilePath(Paths.get(file.get(5)));
        else
            controller.setFilePath(curPath.resolve(file.get(0)));

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
        if (isFoundedFiles)
            return;
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
        if (isFoundedFiles) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Send File");
        Stage ownerStage = ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow());
        File sendingFile = fileChooser.showOpenDialog(ownerStage);

        if (sendingFile != null) {
                Path filePath = sendingFile.toPath();
                MainClient.uploadHandler = new UploadHandler(filePath);
                long size = MainClient.uploadHandler.getFileSize();
                MainClient.sendMessage("upload " + curPath.resolve(filePath.getFileName()) + " " + size);
                MainClient.uploadStatus = true;
        }

//        Stage stage = new Stage();
//        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/uploadMessage.fxml"));
//        Parent root = fxmlLoader.load();
//        stage.setTitle("Downloading directory");
//        stage.setScene(new Scene(root, 300, 150));
//        stage.setResizable(false);
//        UploadMessageController controller = fxmlLoader.getController();
//        controller.setCurPath(curPath);
//        stage.show();
    }

    /**
     * Метод загрузки выбранного файла с сервера
     */
    public void downloadFromServer(ActionEvent actionEvent) throws IOException {
        ArrayList<String> selectedFile = filesTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            return;
        } else if ("D".equals(selectedFile.get(3))) {
            return;
        }

        Path serverPath;
        if (isFoundedFiles)
            serverPath = Paths.get(selectedFile.get(5));
        else
            serverPath = curPath.resolve(selectedFile.get(0));

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(selectedFile.get(0));
        Stage ownerStage = ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow());
        File file = fileChooser.showSaveDialog(ownerStage);
        if (file != null) {
                MainClient.downloadHandler = new DownloadHandler(file.toPath());
                MainClient.sendMessage("download " + serverPath);
                MainClient.downloadStatus = true;
        }

//        ArrayList<String> selectedFile = filesTable.getSelectionModel().getSelectedItem();
//        if (selectedFile == null) {
//            return;
//        }
//
//        Stage stage = new Stage();
//        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/downloadMessage.fxml"));
//        Parent root = fxmlLoader.load();
//        stage.setTitle("Uploading directory");
//        stage.setScene(new Scene(root, 300, 150));
//        stage.setResizable(false);
//        DownloadMessageController controller = fxmlLoader.getController();
//
//        if (isFoundedFiles)
//            controller.setCurPath(Paths.get(selectedFile.get(5)));
//        else
//            controller.setCurPath(curPath.resolve(selectedFile.get(0)));
////        Stage mainStage = ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow());
////        stage.initModality(Modality.WINDOW_MODAL);
////        stage.initOwner(mainStage);
//        stage.show();
    }

    /**
     * Метод переименовывания выбранного файла или папки
     */
    public void rename(ActionEvent actionEvent) throws IOException {
        ArrayList<String> file = filesTable.getSelectionModel().getSelectedItem();
        if (file == null) {
            return;
        }

        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/renamingMessage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("Rename file");
        stage.setScene(new Scene(root, 300, 150));
        stage.setResizable(false);
        RenamingMessageController controller = fxmlLoader.getController();
        if (isFoundedFiles)
            controller.setCurPath(Paths.get(file.get(5)));
        else
            controller.setCurPath(curPath.resolve(file.get(0)));
        stage.show();
    }

    //todo search engine, new stage or something
    public void search(ActionEvent actionEvent) {
        String filename = searchField.getText();
        if (filename.isEmpty())
            return;
        searchField.clear();
        pathFiled.setText("Result of searching for \"" + filename + "\"");
        MainClient.sendMessage("find " + filename);
    }

    public void showFoundedFiles(ArrayList<ArrayList<String>> filesList) {
        filesTable.getItems().clear();
        filesTable.getItems().addAll(filesList);
        isFoundedFiles = true;
    }

    //todo
//    public void moveFile(ActionEvent actionEvent) {
//        ArrayList<String> file = filesTable.getSelectionModel().getSelectedItem();
//        if (file == null) {
//            return;
//        }
//        MainClient.sendMessage("move " + filename);
//    }
}
