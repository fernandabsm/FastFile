package com.fastfile.controller;

import com.fastfile.model.Model;
import com.fastfile.server.ClientHolder;
import com.fastfile.server.UDPClient;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    public AnchorPane dashboard;
    public Button show_files_button;
    public Button enter_button;
    public TextField password;
    public Text warning_message;

    private static final String CAN_DO_UPLOAD = "upload";
    private static final String NO_PASSWORD = "NO_PASSWORD";
    private static final String CAN_DO_DOWNLOAD = "download";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        warning_message.setVisible(false);
        UDPClient client = new UDPClient();
        ClientHolder clientHolder = ClientHolder.getInstance();
        clientHolder.setClient(client);

        enter_button.setOnAction(event -> {
            String pass = password.getText();
            String canDoUpload;
            try {
                if (!warning_message.isVisible()) {
                    canDoUpload = client.createConnection(pass);
                } else {
                    canDoUpload = client.tryPasswordAgain(pass);
                }
                if (CAN_DO_UPLOAD.equals(canDoUpload)) {
                    warning_message.setVisible(false);
                    Model.getInstance().getViewFactory().showUploadFilesView();
                    dashboard.getScene().getWindow().hide();
                } else {
                    warning_message.setVisible(true);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        show_files_button.setOnAction(event -> {
            try {
                String canDoDownload = client.createConnection(NO_PASSWORD);
                if (CAN_DO_DOWNLOAD.equals(canDoDownload)) {
                    Model.getInstance().getViewFactory().showDownloadFilesView();
                    dashboard.getScene().getWindow().hide();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
