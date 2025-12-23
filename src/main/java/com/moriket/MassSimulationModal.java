package com.moriket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.moriket.core.Coordinate2D;
import com.moriket.core.IGameOfLife;
import com.moriket.core.IGameOfLifeFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class MassSimulationModal {

    public static final String STAT_FILEPATH = "./ConwayResults.csv";
    public static final int IMP_PADDING_CELLS = 2;

    @FXML
    TextField iterationField;
    @FXML
    TextField replicationField;
    @FXML
    Button startButton;

    LifeGameController parentGameController;
    List<IGameOfLifeFactory> implFactories;
    private Set<Coordinate2D> liveCells;
    private boolean[][] cellMatrix;

    public void initialize(LifeGameController pGameController, List<IGameOfLifeFactory> implFactories, Set<Coordinate2D> liveCells, boolean[][] cellMatrix) {
        this.parentGameController = pGameController;
        this.implFactories = implFactories;
        this.liveCells = liveCells;
        this.cellMatrix = cellMatrix;
    }

    public void handleStartButtonAction() {
        try {
            int iterCount = Integer.parseInt(iterationField.getText());
            int repCount = Integer.parseInt(replicationField.getText());
            startButton.setDisable(true);
            massSimulate(iterCount, repCount);
            startButton.setDisable(false);
        } catch (NumberFormatException e) {
            System.out.println("Bad input");
        }
    }

    public void massSimulate(int iterations, int replications) {
        System.out.println(iterations + " " + replications);
        //exclude Naive approach
        List<IGameOfLifeFactory> factories = new ArrayList<>();
        for (IGameOfLifeFactory factory : implFactories) {
            if (factory.toString() != "Naive") {
                factories.add(factory);
            }
        }
        //array of iteration times x replications, keyed by factory
        //first row will be for game creation
        //second row for universe initialization
        //subsequent rows for simulation iterations
        HashMap<IGameOfLifeFactory, double[][]> results = new HashMap<>();

        for (IGameOfLifeFactory factory : factories) {
            double[][] implResults = new double[replications][iterations+2];

            for (int i = 0; i < replications; i++) {
                //initialization time
                long startTime = System.currentTimeMillis();
                IGameOfLife game = factory.build(parentGameController.getWidth(), parentGameController.getHeight());
                implResults[i][0] = ((double)System.currentTimeMillis() - startTime)/1000;
                //loading time
                implResults[i][1] = timeAction(() -> game.initializeCells(cellMatrix, liveCells) );
                //simulation times
                for (int j = 2; j < iterations + 2; j++) {
                    implResults[i][j] = timeAction(() -> game.evolve() );
                }
            }
            results.put(factory, implResults);
        }

        writeResultsToFile(factories, results, iterations+2);
    }

    public static double timeAction(Runnable r) {
        long startTime = System.currentTimeMillis();
        r.run();
        return ((double)System.currentTimeMillis() - startTime)/1000;
    } 

    public void writeResultsToFile(List<IGameOfLifeFactory> factories, Map<IGameOfLifeFactory, double[][]> results, int length) {
        List<List<String>> writeCells = new ArrayList<>();

        //initialize column headers
        List<String> colLabels = new LinkedList<String>();
        colLabels.add("Stage");
        for (IGameOfLifeFactory factory : factories) {
            for (int i = 0; i < results.get(factory).length; i++) {
                colLabels.add(factory.toString() + " " + (i+1));    
            }
            //empty space to separate the implementations
            for (int h = 0; h < IMP_PADDING_CELLS; h++) colLabels.add(" ");
        }
        writeCells.add(colLabels);

        //write actual data
        for (int i = 0; i < length; i++) {
            List<String> recordRow = new LinkedList<>();
            //stage
            recordRow.add(Integer.toString(i));
            for (IGameOfLifeFactory factory : factories) {
                for (int j = 0; j < results.get(factory).length; j++) {
                    recordRow.add(Double.toString(results.get(factory)[j][i]));    
                }
                //empty space to separate the implementations
                for (int h = 0; h < IMP_PADDING_CELLS; h++) recordRow.add(" ");
            }
            writeCells.add(recordRow);
        }

        File resultFile = new File(STAT_FILEPATH);
        try (PrintWriter writer = new PrintWriter(resultFile)) {
            writeCells.stream().map(rowList ->
                rowList.stream().collect(Collectors.joining(","))
            ).forEach(writer::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("written to " + resultFile.getAbsolutePath());
    }
}
