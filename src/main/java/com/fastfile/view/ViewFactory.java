package com.fastfile.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ViewFactory {

    public void showLoginView() throws FileNotFoundException {
        loadFont();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fastfile/view/Login.fxml"));
        createStage(loader);
    }

    public void showDownloadFilesView() throws FileNotFoundException {
        loadFont();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fastfile/view/DownloadFiles.fxml"));
        createStage(loader);
    }

    public void showUploadFilesView() throws FileNotFoundException {
        loadFont();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fastfile/view/UploadFiles.fxml"));
        createStage(loader);
    }

    private void loadFont() throws FileNotFoundException {
        File file = new File("src/main/resources/com/fastfile/font/Fredoka-Regular.ttf");
        Font.loadFont(new FileInputStream(file), 40);
    }

    private void createStage(FXMLLoader loader) {
        Scene scene = null;
        try {
            scene = new Scene(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.getIcons().add(new Image(String.valueOf(getClass().getResource("/com/fastfile/images/fastFile.png"))));
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setTitle("FastFile");
        stage.show();
    }
}
