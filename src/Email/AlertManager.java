/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Email;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.util.Duration;

/**
 *
 * @author AlperenDGRYL
 */
public class AlertManager {

    public static void showAlert(int duration, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.show();
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(duration),
                ae -> alert.close()
        ));
        timeline.play();
    }
}
