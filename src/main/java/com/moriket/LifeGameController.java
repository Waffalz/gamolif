package com.moriket;

import com.moriket.core.Coordinate2D;
import com.moriket.core.IGameOfLife;
import com.moriket.core.IGameOfLifeFactory;
import com.moriket.impl.GameOfLifeChangeList;
import com.moriket.impl.GameOfLifeHashLife;
import com.moriket.impl.GameOfLifeNaive;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.lang.Math;
import java.text.DecimalFormat;

public class LifeGameController {

    public static final Point2D WINDOW_SIZE = new Point2D(1200, 1400);

    public static final double[] VIEWPORT_SCALE_BOUNDS = new double[]{5,20};
    public static final double SCROLL_SCALE = .05f;

    //default simulation speed, in updates per second
    public static final double BASE_SIMULATION_FREQUENCY = 6;

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#");

    public static final boolean DEBUG_MODE = false;

    //I probably shouldve MVCd this

    @FXML
    private MenuItem newWorldButton;
    @FXML
    private MenuItem massSimulateButton;
    @FXML
    private ComboBox<IGameOfLifeFactory> implementationSelectionBox;
    @FXML
    private Button resetButton;
    @FXML
    private Button playButton;
    @FXML
    private Button stepButton;
    @FXML
    private Slider simSpeedSlider;
    @FXML
    private Label speedLabel;
    @FXML
    private Canvas canvas;

    private int gameWidth;
    private int gameHeight;
    private IGameOfLife game;

    private List<IGameOfLifeFactory> implFactories;

    //using two different representations for canvas cells
    //matrix is used for rendering and cell matrix implementaitons
    //List is a sorted list of live cells
    //feed this into the newly created game on sim startup
    //this will be used as a way to init gol sims
    //across implementations that may not use 2d arrays (e.g. quadtrees)
    private Set<Coordinate2D> liveCells;
    private boolean[][] cells;
    
    //graphics
    private double viewportScale = 15;//in pixels per square
    private Rectangle2D viewport;

    //game control attrbs
    //signal whether a run has already started
    private boolean started = false;
    private boolean playing = false;
    private Timer simTimer;
    private TimerTask simTimerTask;

    private ExecutorService calculator;

    //mouse control attrbs
    private boolean panning;
    private Point2D prevMousePos;
    private Point2D dragOrigin;

    Window window;

    public void initialize(int width, int height){
        this.gameWidth = width;
        this.gameHeight = height;
        initializeCells();

        Point2D portSize = new Point2D(canvas.getWidth()/viewportScale, canvas.getHeight()/viewportScale);
        viewport = new Rectangle2D(((double)gameWidth-portSize.getX())/2, ((double)gameHeight-portSize.getY())/2, portSize.getX(), portSize.getY());

        simTimer = new Timer(false);

        implFactories = new ArrayList<>();
        implFactories.add(new GameOfLifeChangeList.GameOfLifeChangeListFactory());
        implFactories.add(new GameOfLifeHashLife.GameOfLifeHashLifeFactory());
        implFactories.add(new GameOfLifeNaive.GameOfLifeNaiveFactory());

        initalizeImplementationDropdown();

        /*
        * I was hoping to naively make a scrollPane with a canvas inside of it
        * then just make the canvas size the entire size of the world*cellSize, but
        * that gets so big I get a rendering exception from running out of VRAM
        */
        //canvas.setWidth(CELL_SIZE*gameWidth);
        //canvas.setHeight(CELL_SIZE*gameHeight);

        VBox.setVgrow(canvas, Priority.ALWAYS);

        simSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            //from changing to not changing (i.e. on release)
            if (true) {
                sliderChanged();
            }
        });

        //resize the window
        window = App.getMainScene().getWindow();
        //window.setWidth(WINDOW_SIZE.getX());
        //window.setHeight(WINDOW_SIZE.getY());
        window.sizeToScene();
        drawCanvas();

        App.getMainScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::handleWindowClose);
    }

    public int getWidth() {
        return gameWidth;
    }

    public int getHeight() {
        return gameHeight;
    }

    private void initializeCells() {
        liveCells = new HashSet<>();
        cells = new boolean[gameHeight][gameWidth];
    }

    private void initalizeImplementationDropdown() {
        implementationSelectionBox.getItems().addAll(implFactories);
        implementationSelectionBox.getSelectionModel().selectFirst();
    }

    private IGameOfLifeFactory getSelectedGameOfLifeFactory() {
        return implementationSelectionBox.getSelectionModel().getSelectedItem();
    }

    private void handleWindowClose(WindowEvent event) {
        cleanup();
        App.getMainScene().getWindow().removeEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::handleWindowClose);
    }

    private void cleanup() {
        if (game != null) game.cleanup();
        simTimer.cancel();
        if (calculator != null && !calculator.isShutdown()) {
            calculator.shutdownNow();
        }
    }

    private void startGame() {
        started = true;
        calculator = Executors.newSingleThreadExecutor();
        resetButton.setDisable(false);
        implementationSelectionBox.setDisable(true);
        resetButton.setText("Reset");
        game = getSelectedGameOfLifeFactory().build(gameWidth, gameHeight);
        game.initializeCells(cells, liveCells);
    }

    private void playGame(double frequency) {
        if (!started) {
            startGame();
        }
        playButton.setText("Pause");
        simTimerTask = createSimTimerTask();
        long period = Math.round(1000/frequency);
        simTimer.scheduleAtFixedRate(simTimerTask, period, period);
        stepButton.setDisable(true);
        playing = true;
    }

    private void pauseGame() {
        simTimerTask.cancel();
        simTimer.purge();
        playButton.setText("Play");        
        stepButton.setDisable(false);
        playing = false;
    }

    //clear the board
    private void clearBoard() {
        initializeCells();
        drawCanvas();
    }

    //reset to initial state before the run started
    //discard game object
    private void resetGame() {
        calculator.shutdownNow();
        if (playing) {
            pauseGame();
        }
        game.cleanup();
        synchronized(game) {
            game = null;
        }
        started = false;
        implementationSelectionBox.setDisable(false);
        drawCanvas();
    }

    public void updateGame() {
        if (!started) {
            Platform.runLater(new Runnable () {
                public void run() {
                    synchronized(game) {
                        startGame();
                    }
                }
            });
        }
        game.evolve();
        Platform.runLater(new Runnable () {
            public void run() {
                //synchronized(game) {
                    drawCanvas();
                //}
            }
        });
    }

    private void toggleCell(int x, int y) {
        Coordinate2D coord = new Coordinate2D(x, y);
        if (cells[y][x]) {
            cells[y][x] = false;
            liveCells.remove(coord);
        } else {
            cells[y][x] = true;
            liveCells.add(coord);
        }
    }

    private boolean getCell(int x, int y) {
        return cells[y][x];
    }

    /*
     * -------------------------------------
     * Simulation Control Bar
     * -------------------------------------
     */

    @FXML
    private void handleStepButtonAction() {
        if (!started) {
            startGame();
        }
        executeUpdate();
    }

    @FXML
    private void handlePlayButtonAction() {
        if (playing == false) {
            //start playing
            playGame(BASE_SIMULATION_FREQUENCY * simSpeedSlider.getValue());
        } else {
            //pause play
            pauseGame();
        }
    }

    private TimerTask createSimTimerTask() {
        return new TimerTask() {
            public void run() {
                executeUpdate();
            }
        };
    }

    private void executeUpdate() {
        calculator.execute(new Runnable () {
            public void run() {
                updateGame();
            }
        });
    }

    @FXML
    private void handleResetButtonAction() {
        if (started) {
            resetGame();
            resetButton.setText("Clear");
        } else {
            clearBoard();
        }
        
    }

    /*
     * -------------------------------------
     * Sim speed slider
     * -------------------------------------
     */

    private void setSimSpeed(double modifier) {
        if (playing) {
            pauseGame();
            playGame(BASE_SIMULATION_FREQUENCY * modifier);
        }
        speedLabel.setText(DECIMAL_FORMAT.format(modifier) + "x");
    }
    
    private void sliderChanged() {
        setSimSpeed(simSpeedSlider.getValue());
    }

    /*
     * -------------------------------------
     * World Grid graphics
     * -------------------------------------
     */

    /*
     * draw to the canvas
     */
    private void drawCanvas(){
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        graphics.setLineWidth(1);

        //calculate the index bounds of the grid that will be visible to the screen
        int leftIndexX = Math.max((int)Math.floor(viewport.getMinX()), 0);
        int rightIndexX = Math.min((int)Math.ceil(viewport.getMaxX()), gameWidth-1);

        int topIndexY = Math.max((int)Math.floor(viewport.getMinY()), 0);
        int bottomIndexY = Math.min((int)Math.ceil(viewport.getMaxY()), gameHeight-1);

        graphics.setFill(Color.BLACK);
        graphics.setStroke(Color.GREY);
        //iterate lines and cells to draw
        for (int y = topIndexY; y <= bottomIndexY; y++) {
            //calculate position of grid positions relative to viewport and scale accordingly
            double gridLineY = (y - viewport.getMinY()) * viewportScale;
            for (int x = leftIndexX; x <= rightIndexX; x++) {
                //calculate position of grid positions relative to viewport and scale accordingly
                double gridLineX = (x - viewport.getMinX()) * viewportScale;
                graphics.strokeLine(gridLineX, 0, gridLineX, canvas.getHeight());
                //draw the cell
                if ((game == null)? getCell(x, y) : game.getValue(x, y)) {
                    graphics.fillRect(gridLineX, gridLineY, viewportScale, viewportScale);
                }
            }
            graphics.strokeLine(0, gridLineY, canvas.getWidth(), gridLineY);
        }

        //for debugging squares
        if (DEBUG_MODE) {
            if (game != null) {
                Set<Coordinate2D> debugPoints = game.getDebuggingCoords();
                if (debugPoints != null) {
                    for (Coordinate2D point: debugPoints) {
                        graphics.setFill(new Color(0, 1, 0, .5));
                        double gridLineX = (point.getX() - viewport.getMinX()) * viewportScale;
                        double gridLineY = (point.getY() - viewport.getMinY()) * viewportScale;
                        graphics.fillRect(gridLineX, gridLineY, viewportScale, viewportScale);
                    }
                }
            }
        }

        double gridLineY = (rightIndexX + 1 - viewport.getMinY()) * viewportScale;
        double gridLineX = (bottomIndexY + 1 - viewport.getMinX()) * viewportScale;
        //need to draw the last lines in the grid. I don't feel like culling these 
        graphics.strokeLine(gridLineX, 0, gridLineX, canvas.getHeight());
        graphics.strokeLine(0, gridLineY, canvas.getWidth(), gridLineY);
    }

    private void setViewportScale(double newScale){
        viewportScale = newScale;
        Point2D center = new Point2D((viewport.getMinX() + viewport.getMaxX())/2, (viewport.getMinY() + viewport.getMaxY())/2);

        Point2D portSize = new Point2D(canvas.getWidth()/viewportScale, canvas.getHeight()/viewportScale);
        viewport = new Rectangle2D(center.getX()-portSize.getX()/2, center.getY()-portSize.getY()/2, portSize.getX(), portSize.getY());
        drawCanvas();
    }

    private void translateViewPort(Point2D diff){
        viewport = new Rectangle2D(viewport.getMinX() - diff.getX(), viewport.getMinY() - diff.getY(), viewport.getWidth(), viewport.getHeight());
        drawCanvas();
    }


    /*
     * -------------------------------------
     * Mouse Control
     * -------------------------------------
     */

    private Point2D getMousePosition(MouseEvent mouseEvent) {
        return new Point2D(mouseEvent.getX(), mouseEvent.getY());
    }

    @FXML
    private void handleCanvasMouseClick(MouseEvent mEvent) {

        //disallow editing the world if we've already started a run
        if (started) return;

        //maximum distance from start of mouse press to deem it not a drag
        if (getMousePosition(mEvent).distance(dragOrigin) > 2) return;
        int indexX = (int)Math.floor(viewport.getMinX() + mEvent.getX()/viewportScale);
        int indexY = (int)Math.floor(viewport.getMinY() + mEvent.getY()/viewportScale);

        if (0 <= indexX && indexX < gameWidth &&
            0 <= indexY && indexY < gameHeight) {
            toggleCell(indexX, indexY);
        }
        drawCanvas();
    }

    @FXML
    private void handleScroll(ScrollEvent mEvent) {
        double disp = mEvent.getDeltaY() * SCROLL_SCALE;
        setViewportScale(Math.clamp(viewportScale + (disp), VIEWPORT_SCALE_BOUNDS[0], VIEWPORT_SCALE_BOUNDS[1]));
    }

    @FXML
    private void handleCanvasMouseDragged(MouseEvent mEvent) {
        if (panning) {
            Point2D currentPos = getMousePosition(mEvent);
            Point2D dif = currentPos.subtract(prevMousePos).multiply(1/viewportScale);
            translateViewPort(dif);
            prevMousePos = currentPos;
        }
    }

    @FXML
    private void handleMousePressed(MouseEvent mEvent) {
        panning = true;
        dragOrigin = prevMousePos = getMousePosition(mEvent);
    }

    @FXML
    private void handleMouseReleased(MouseEvent mEvent) {
        panning = false;
    }

    /*
     * -------------------------------------
     * Mouse Control
     * -------------------------------------
     */

    @FXML
    private void handleNewWorldButtonAction() throws IOException {
        cleanup();
        FXMLLoader loader = App.getFxmlLoader("worldSize");
        Parent newScene = loader.load();
        App.setRoot(newScene);
    }

    @FXML
    private void handleMassSimulationButtonAction() throws IOException {
        Stage massModalStage = new Stage();
        FXMLLoader massSimLoader = App.getFxmlLoader("massSim");
        Parent massModalRoot = massSimLoader.load();
        massModalStage.setTitle("Mass Simulation");
        massModalStage.initModality(Modality.WINDOW_MODAL);
        massModalStage.initOwner(window);
        massModalStage.setScene(new Scene(massModalRoot));
        ((MassSimulationModal)massSimLoader.getController()).initialize(this, implFactories, liveCells, cells);
        massModalStage.show();
    }

}
