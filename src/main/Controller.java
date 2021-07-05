package main;

import utils.FileManager;
import workers.DirWatch;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static persistence.Settings.INPUT;
import static persistence.Settings.STARTED;
import static persistence.Settings.*;

public class Controller implements Initializable {

    private final static Logger LOGGER = Logger.getLogger(Controller.class.getName());
    private Preferences prefs = getInstance().prefs;
    public static ExecutorService executorService;
    private FileManager fileManager = new FileManager();
    public static Boolean SERVICE_STARTED = false;
    private final String homePath = System.getProperty("user.home");

    @FXML
    private Button btnStart;

    @FXML
    private Button btnStop;

    @FXML
    private Button btnZReport;

    @FXML
    private TextField txtSource;

    @FXML
    private Button btnSetSource;

    @FXML
    private TextField txtDest;

    @FXML
    private Button btnSetDest;

    @FXML
    private TextField txtEFPSource;

    @FXML
    private Button btnSetEFPSource;

    @FXML
    private TextField txtEFPDest;

    @FXML
    private Button btnSetEFPDest;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblActivation;

    @FXML
    private MenuItem actionClose;

    @FXML
    private MenuItem actionAbout;

    @FXML
    private ProgressIndicator progressStatus;

    @FXML
    void onAbout(ActionEvent event) {
    }

    @FXML
    void onClose(ActionEvent event) {
        showOnCloseAlert();
    }

    @FXML
    void onSetDest(ActionEvent event) {
        String dirPath = chooseDir(txtDest, prefs.get(OUTPUT, Main.OUTPUT_PATH));
        try {
            prefs.put(OUTPUT, dirPath);
            txtDest.setText(dirPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onSetEFPDest(ActionEvent event) {
        String dirPath = chooseDir(txtEFPDest, prefs.get(EFP_OUTPUT, Main.EFP_OUTPUT_PATH));
        try {
            prefs.put(EFP_OUTPUT, dirPath);
            txtEFPDest.setText(dirPath);
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    @FXML
    void onSetEFPSource(ActionEvent event) {
        String dirPath = chooseDir(txtEFPSource, prefs.get(EFP_INPUT, Main.EFP_INPUT_PATH));
        try {
            prefs.put(EFP_INPUT, dirPath);
            txtEFPSource.setText(dirPath);
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    @FXML
    void onSetSource(ActionEvent event) {
        String dirPath = chooseDir(txtSource, prefs.get(INPUT, Main.INPUT_PATH));
        try {
            prefs.put(INPUT, dirPath);
            txtSource.setText(dirPath);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onStart(ActionEvent event) {
        if(!SERVICE_STARTED) {
            executorService = Executors.newCachedThreadPool();
            executorService.submit(new DirWatch(prefs.get(INPUT, Main.INPUT_PATH), this));
            lblStatus.setText("Started...");
            btnStart.setDisable(true);
            btnStop.setDisable(false);
            LOGGER.info("Watch service started");

            //save service state to prefs for use when app recovers from a crash
            prefs.putBoolean(STARTED, true);
            SERVICE_STARTED = true;
        }
    }

    @FXML
    void onStop(ActionEvent event) {
        if(SERVICE_STARTED) {
            try {
                executorService.shutdownNow();
            } catch (Exception e) {
                e.printStackTrace();
            }
            lblStatus.setText("Stopped.");
            btnStart.setDisable(false);
            btnStop.setDisable(true);
            LOGGER.info("Watch service stopped");

            prefs.putBoolean(STARTED, false);
            SERVICE_STARTED = false;
        }
    }

    @FXML
    void onSettings(ActionEvent event) {
    }

    @FXML
    void onZReport(ActionEvent event) {
        try {
            fileManager.writeToTextFile(
                    "C_DYZ // Produce daily Z. Report. ",
                    prefs.get(EFP_INPUT, Main.EFP_INPUT_PATH) + File.separator + "Z_REPORT.txt"
            );
            LOGGER.info("Successfully wrote Z_REPORT to the file.");
//            lblStatus.setText("Generating Z. Report.");
        } catch (IOException e) {
            LOGGER.info("An error occurred.");
            e.printStackTrace();
            lblStatus.setText("Failed to generate Z. Report.");
            showAlert(e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtSource.setText(prefs.get(INPUT, Main.INPUT_PATH));
        txtDest.setText(prefs.get(OUTPUT, Main.OUTPUT_PATH));
        txtEFPSource.setText(prefs.get(EFP_INPUT, Main.EFP_INPUT_PATH));
        txtEFPDest.setText(prefs.get(EFP_OUTPUT, Main.EFP_OUTPUT_PATH));
        btnStop.setDisable(true);

        LOGGER.setLevel(Level.INFO);

        //if the service was not stopped on last app shutdown, automatically start the service
        if(prefs.getBoolean(STARTED, false))
            btnStart.fire();
    }

    private <T extends Event> void closeWindowEvent(T t) {
        showOnCloseAlert();
    }

    private boolean isDirValid(String path){
        Path pathToDir = Paths.get(path);
        if(Files.exists(pathToDir))
            if(Files.isDirectory(pathToDir))
                return true;
        showInvalidDirAlert();
        return false;
    }

    private void showInvalidDirAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Could set folder.");
        alert.setContentText("Invalid folder selection");
        alert.showAndWait();
    }

    // directory chooser for the necessary input and output files
    private String chooseDir(TextField textField, String prevOpenedPath) throws NullPointerException{
        System.out.println("homePath");

        LOGGER.info("prev. dir " + prevOpenedPath);
        File dir;
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select folder");

        File validator = new File(prevOpenedPath);
        if (validator.exists() && validator.isDirectory())
            dirChooser.setInitialDirectory(validator);
        else
            dirChooser.setInitialDirectory(new File(homePath));

        dir = dirChooser.showDialog(textField.getScene().getWindow());
        if (dir == null)
            return null;
        String dirPath = dir.getAbsolutePath();
        LOGGER.info(dirPath);

        return dirPath;
    }

    private void showAlert(String textContent){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Could not generate Z. Report");
        alert.setContentText(textContent);
        alert.showAndWait();
    }

    private void showOnCloseAlert(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setHeaderText("Are you sure you want to close the application");
//        alert.setContentText("textContent");
        Optional<ButtonType> result = alert.showAndWait();
        if(!result.isPresent()){

        }
        // alert is exited, no button has been pressed.
        else if(result.get() == ButtonType.OK){
            Platform.exit();
        }
        //oke button is pressed
        else if(result.get() == ButtonType.CANCEL){
            alert.close();
        }
        // cancel button is pressed
    }

    public void toggleIndicator(){
        Platform.runLater(() -> {
            if (progressStatus.isVisible())
                progressStatus.setVisible(false);
            else
                progressStatus.setVisible(true);
        });
    }
}