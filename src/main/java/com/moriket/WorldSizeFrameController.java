package com.moriket;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.Parent;

public class WorldSizeFrameController {

    //range of supported world sizes, in powers of 2 
    private static int[] SUPPORTED_SIZE_EXPO_RANGE = new int[]{6, 17};

    private class WorldSizeSelection {
        //as in 2^x
        int powDim;

        public WorldSizeSelection(int powDim) {
            this.powDim = powDim;
        }

        public int getDim() {
            return (int)Math.pow(2, powDim);
        }

        @Override
        public String toString() {
            String dimString = Integer.toString(getDim());
            return dimString + "x" + dimString;
        }
    }

    @FXML
    private ComboBox<WorldSizeSelection> sizeSelectionBox;

    private void populateWorldSizeComboBox() {
        //Populate dropdown with a range of powers of 2
        for (int i = SUPPORTED_SIZE_EXPO_RANGE[0]; i <= SUPPORTED_SIZE_EXPO_RANGE[1]; i++){
            sizeSelectionBox.getItems().add(new WorldSizeSelection(i));
        }
        sizeSelectionBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void startGame() throws IOException {
        //Getting the selected world size
        WorldSizeSelection selection = sizeSelectionBox.getSelectionModel().getSelectedItem();

        FXMLLoader loader = App.getFxmlLoader("lifeGame");
        Parent newScene = loader.load();
        //Initialize the new game scene with the selected world size
        ((LifeGameController)loader.getController()).initialize(selection.getDim(), selection.getDim());
        App.setRoot(newScene);
    }

    @FXML
    public void initialize() {
        populateWorldSizeComboBox();
    }
}
