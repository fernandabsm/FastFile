package com.fastfile.controller;

import com.fastfile.server.ClientHolder;
import com.fastfile.server.UDPClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class UploadFilesController implements Initializable {

    @FXML
    public ListView<String> listView;
    public Button upload_button;

    private List<String> arquivosDisponiveis = new ArrayList<>();

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

                            borderPane.setLeft(label);
                            BorderPane.setMargin(label, new Insets(0, 0, 0, 10));

                            setGraphic(borderPane);

                        }
                    }
                };
            }
        });

        upload_button.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File("src/main/resources/com/fastfile/files/client"));
            File selectedFile = fileChooser.showOpenDialog(null);

            if (selectedFile != null) {
                String fileName = selectedFile.getName();
                try {
                    client.sendFile(fileName);
                    arquivosDisponiveis.add(fileName);
                    listView.getItems().add(fileName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });


        try {
            arquivosDisponiveis.addAll(client.getAvailableFiles());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Adiciona a lista de arquivos ao ListView
        listView.getItems().addAll(arquivosDisponiveis);
    }
}
