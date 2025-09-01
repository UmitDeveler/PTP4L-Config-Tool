package com.ptpapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PTPConfig.fxml"));
        Parent root = loader.load();
        
        PTPConfigController controller = loader.getController();
        controller.setStage(primaryStage);
        
        Scene scene = new Scene(root, 1200, 900);
        scene.getStylesheets().add(getClass().getResource("/css/modern-styles.css").toExternalForm());
        
        primaryStage.setTitle("PTP4L Configuration Tool");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 