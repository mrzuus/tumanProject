package com.tuman;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.tuman.dropBox.dropBoxMain;
import com.tuman.mainController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;
import java.util.ResourceBundle;

public class closeShiftController extends dropBoxMain implements Initializable {



    @FXML
    private AnchorPane stage;


    @FXML
    private Button cancel;



    @FXML
    private Button confirmCloseButton;

    @FXML
    private TextField fieldData ;


    DateFormat formatter = new SimpleDateFormat("dd.MM.yy");
    Calendar obj = Calendar.getInstance();
    String str = formatter.format(obj.getTime());
    DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/TUMAN").build();
    DbxClientV2 client = new DbxClientV2(config, getAPI());



    public closeShiftController() throws DbxException {
    }

    @FXML
    void close(ActionEvent event) throws IOException {

    }
    @FXML
    public void confirmClose(ActionEvent actionEvent) throws IOException, InterruptedException, DbxException {
        if (!fieldData.getText().isEmpty()) {
            File file = new File(str + ".txt");


            if (new File(str +".txt").exists()){
               file = new File(str + "(2).txt");
            }

            FileWriter writer = new FileWriter(file);
            writer.write("Итого за смену:\n" + "Всего:"+mainController.money.getTotalMoney() + "\n Наличными:" + mainController.money.getCashRevenue()
                    + "\n Безналом:" + mainController.money.getCardRevenue() + "\n Переводом:" + mainController.money.getTransferRevenue() );
            writer.close();
            if(fileIsExist()) {
                try (InputStream in = new FileInputStream(file)) {
                    FileMetadata metadata = client.files().uploadBuilder("/" + file.getName()).uploadAndFinish(in);


                    System.out.println("Файл успешно загружен в Dropbox: " + metadata.getPathLower());
                } catch (UploadErrorException e) {
                    System.err.println("Ошибка при загрузке файла в Dropbox: " + e.getMessage());
                } finally {
                    file.delete();
                    Stage stage = (Stage) cancel.getScene().getWindow();
                    stage.close();
                }
            }

        }
        else if (fieldData.getText().isEmpty()){
            StackPane rt = new StackPane();
            Scene sc = new Scene(rt, 350, 150);
            Stage st = new Stage();
            st.setScene(sc);
            st.initModality(Modality.WINDOW_MODAL);
            st.setTitle("Изменение позиций");
            Label errorLabel = new Label(" Вы не ввели дату");
            rt.getChildren().add(errorLabel);
            rt.setAlignment(errorLabel, Pos.CENTER);
            errorLabel.setFont(new Font("Ariel", 20));
            st.show();

        }
        else {
            File file = new File(str + ".txt");
            try (InputStream in = new FileInputStream(file)) {
                FileMetadata metadata = client.files().uploadBuilder("/" + file.getName()).uploadAndFinish(in);


                System.out.println("Файл успешно загружен в Dropbox: " + metadata.getPathLower());
            } catch (UploadErrorException e) {
                System.err.println("Ошибка при загрузке файла в Dropbox: " + e.getMessage());
            } finally {
                file.delete();
                Stage stage = (Stage) cancel.getScene().getWindow();
                stage.close();
            }
        }
    }
    boolean fileIsExist() throws DbxException {
        ListFolderResult result = client.files().listFolder("");
        while (true) {
            for (Metadata metadata : result.getEntries()) {
               if(Objects.equals(metadata.getName(),"/" + str + ".txt")){
                   return true;
               }
            }
            return true;
    }}


@FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
fieldData.setText(str);
    }


}
