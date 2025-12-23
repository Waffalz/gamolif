package com.moriket.impl;

import java.util.Set;

import com.moriket.core.Coordinate2D;
import com.moriket.core.IGameOfLife;
import com.moriket.core.IGameOfLifeFactory;

public class GameOfLifeNaive implements IGameOfLife {

    public static class GameOfLifeNaiveFactory implements IGameOfLifeFactory {
        public GameOfLifeNaiveFactory() {

        }

        public String toString() {
            return "Naive";
        }

        public IGameOfLife build(int width, int height) {
            return new GameOfLifeNaive(width, height);
        }
    }
    
    private int width, height;
    
    /*
     * this 2d array nesting order syntax always confuses me
     * for future reference, it's cells[y][x]
     */
    private boolean[][] cells;

    private boolean[][] initialState;

    public GameOfLifeNaive(int width, int height) {
        cells = new boolean[height][width];
        this.width = width;
        this.height = height;
    }

    public void initializeCells(boolean[][] initMatrix, Set<Coordinate2D> initList) {
        cells = initMatrix;
    }

    public synchronized void evolve() {
        if (initialState == null) initialState = cells;
        boolean[][] newCells = new boolean[height][width];
        
        for (int i = 0; i < cells.length; i++) { //iterating over rows (y)
            for (int j = 0; j < cells[i].length; j++) { //iterating over columns (x)
                boolean cellVal = cells[i][j];
                boolean newCellVal = cellVal;
                int liveAdj = liveAdjacentCells(j, i);
                if (cellVal) {
                    newCellVal = liveAdj > 1 && liveAdj < 4;
                } else {
                    newCellVal = liveAdj==3;
                }
                newCells[i][j] = newCellVal;
            }
        }
        cells = newCells;
    }

    //Returns number of adjacent cells
    private int liveAdjacentCells(int x, int y) {
        int liveAdj = 0;
        for (Coordinate2D dir : IGameOfLife.adjacentCells) {
            liveAdj += getValueMod(x + dir.getX(), y + dir.getY())?1:0;
        }
        return liveAdj;
    }

    private boolean getValueMod(int x, int y) {
        return cells[IGameOfLife.modulus(y, height)][IGameOfLife.modulus(x, width)];
    }

    public boolean getValue(int x, int y) {
        return cells[y][x];
    }
    public void clear() {
        cells = new boolean[height][width];
        reset();
    }
    public void reset () {
        //no point in resetting what's already reset
        if (initialState == null) return;
        cells = initialState;
        initialState = null;
    }

    public void cleanup() {

    }

    public Set<Coordinate2D> getDebuggingCoords() {return null;}
}
