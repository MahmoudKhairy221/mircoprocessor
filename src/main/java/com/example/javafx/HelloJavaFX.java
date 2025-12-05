package com.example.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simple JavaFX test application to verify JavaFX setup is working correctly.
 */
public class HelloJavaFX extends Application {
    
    private Label messageLabel;
    private int clickCount = 0;
    
    @Override
    public void start(Stage primaryStage) {
        // Create UI components
        messageLabel = new Label("Hello, JavaFX!");
        messageLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Button clickButton = new Button("Click Me!");
        clickButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
        clickButton.setOnAction(e -> {
            clickCount++;
            messageLabel.setText("Button clicked " + clickCount + " time(s)!");
        });
        
        // Create layout
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getChildren().addAll(messageLabel, clickButton);
        
        // Create scene
        Scene scene = new Scene(root, 400, 300);
        
        // Configure stage
        primaryStage.setTitle("JavaFX Test");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
        
        System.out.println("JavaFX application started successfully!");
    }
    
    @Override
    public void stop() {
        System.out.println("JavaFX application stopped.");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}














