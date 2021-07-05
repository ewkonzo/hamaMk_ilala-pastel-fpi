package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.Calendar;
import java.util.Optional;
import java.util.logging.Logger;

public class Main extends Application {
    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    //default paths
    public static final String INPUT_PATH = "";
    public static final String OUTPUT_PATH = "";
    public static final String EFP_INPUT_PATH = "";
    public static final String EFP_OUTPUT_PATH = "";

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        primaryStage.setTitle("iLala Pastel FPI");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();

        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);

        if(isExpired())
            showTrialExpiryAlert();
    }

    private <T extends Event> void closeWindowEvent(T t) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setHeaderText("Are you sure you want to close the application");
//        alert.setContentText("textContent");
        Optional<ButtonType> result = alert.showAndWait();
        if(!result.isPresent()){

        }
        // alert is exited, no button has been pressed.
        else if(result.get() == ButtonType.OK){
            try {
                Controller.executorService.shutdown();
            } catch (NullPointerException e) {
                LOGGER.warning("service already stopped, skipping sequence");
            }
            Platform.exit();
        }
        //oke button is pressed
        else if(result.get() == ButtonType.CANCEL){
            alert.close();
            t.consume();
        }
    }

    private static boolean isExpired(){
        Calendar expireDate = Calendar.getInstance();
        // January is 0 (y, m, d)
        expireDate.set(2021, Calendar.JULY, 12);
        // Get current date and compare
        return Calendar.getInstance().after(expireDate);
    }

    private void showTrialExpiryAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Trial version expired");
        alert.setContentText("Please contact the developers for activation");
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
