package com.moriket;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("worldSize"), 640, 480);
        stage.setTitle("Ethan's Conway's Game of Life");
        stage.setScene(scene);
        stage.show();
        stage.setResizable(false);
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static void setRoot(Parent newScene) {
        scene.setRoot(newScene);
    }

    public static FXMLLoader getFxmlLoader(String fxml) throws IOException {
        return new FXMLLoader(App.class.getResource(fxml + ".fxml"));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        return getFxmlLoader(fxml).load();
    }

    public static Scene getMainScene() {
        return scene;
    }

    public static void main(String[] args) {
        launch();
    }

}