package com.fastfile.controller;

import com.fastfile.server.ClientHolder;
import com.fastfile.server.UDPClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class DownloadFilesController implements Initializable {

    @FXML
    public ListView<String> listView;

    private List<String> arquivosParaDownload = new ArrayList<>(); // Lista dinâmica de nomes de arquivos

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        UDPClient client = ClientHolder.getInstance().getClient();

        listView.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/fastfile/style/ListView.css")).toExternalForm());

        listView.setFixedCellSize(35);

        listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {

            @Override
            public ListCell<String> call(ListView<String> listView) {
                return new ListCell<String>() {

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            BorderPane borderPane = new BorderPane();

                            Label label = new Label(item);

                            File file = new File("src/main/resources/com/fastfile/font/Fredoka-Regular.ttf");
                            try {
                                Font font = Font.loadFont(new FileInputStream(file), 16);
                                label.setFont(font);
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            }

                            Image image;
                            try {
                                image = new Image(Objects.requireNonNull(getClass().getResource("/com/fastfile/images/download.png")).toURI().toString());
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                            ImageView imageView = new ImageView(image);
                            imageView.setImage(image);
                            imageView.setFitWidth(25);
                            imageView.setFitHeight(25);

                            borderPane.setLeft(label);
                            BorderPane.setMargin(label, new Insets(0, 0, 0, 10));
                            borderPane.setRight(imageView);
                            BorderPane.setMargin(imageView, new Insets(0, 10, 0, 0));

                            // Adiciona um evento de clique à ImageView
                            imageView.setOnMouseClicked(event -> {
                                String fileName= label.getText();
                                System.out.println("Clicou na imagem para o arquivo: " + fileName);
                                try {
                                    client.receiveFile(fileName);
                                    showDownloadSuccessNotification();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });


                            setGraphic(borderPane);

                        }
                    }
                };
            }
        });

        try {
            arquivosParaDownload.addAll(client.getAvailableFiles());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Adiciona a lista de arquivos ao ListView
        listView.getItems().addAll(arquivosParaDownload);
    }

    private void showDownloadSuccessNotification() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Download concluído");
        alert.setHeaderText(null);
        alert.setContentText("O arquivo foi baixado com sucesso.");
        alert.showAndWait();
    }
}
