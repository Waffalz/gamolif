package com.moriket.impl;

import java.util.HashSet;
import java.util.Set;

import com.moriket.core.Coordinate2D;
import com.moriket.core.IGameOfLife;
import com.moriket.core.IGameOfLifeFactory;

//Implementation in which we only iterate over cells adjacent to cells that have changed previously
//Just a Naive implementation with extra stuff
//Hardest part for this one is apparently making a good hashing function on Coordinate2D to get unique hashes
public class GameOfLifeChangeList implements IGameOfLife {
        public static class GameOfLifeChangeListFactory implements IGameOfLifeFactory {
        public GameOfLifeChangeListFactory() {

        }

        public String toString() {
            return "ChangeList";
        }

        public IGameOfLife build(int width, int height) {
            return new GameOfLifeChangeList(width, height);
        }
    }
    
    private int width, height;
    
    /*
     * this 2d array nesting order syntax always confuses me
     * for future reference, it's cells[y][x]
     */
    private boolean[][] cells;

    private boolean[][] initialState;

    //list of cells that have changed or were adjacent to changed cells in the last evolution
    private Set<Coordinate2D> changedCells;

    public GameOfLifeChangeList(int width, int height) {
        cells = new boolean[height][width];
        this.width = width;
        this.height = height;
        changedCells = new HashSet<>();
    }

    public void initializeCells(boolean[][] initMatrix, Set<Coordinate2D> initList) {
        changedCells = new HashSet<>();
        
        for (Coordinate2D liveCell : initList) {
            addAdjacentCellsToChangedSet(liveCell.getX(), liveCell.getY());
            cells[liveCell.getY()][liveCell.getX()] = true;
        }
    }

    public synchronized void evolve() {
        if (initialState == null) initialState = cells;
        boolean[][] newCells = new boolean[height][width];
        
        Set<Coordinate2D> oldChangedSet = changedCells;
        changedCells = new HashSet<>();

        for (Coordinate2D point : oldChangedSet) {
            int x = point.getX(), y = point.getY();
            boolean newVal = getCellEvolution(x, y);
            newCells[y][x] = newVal;
            if (cells[y][x] != newVal || newVal) addAdjacentCellsToChangedSet(x, y);
        }
        cells = newCells;
    }

    public boolean getCellEvolution(int x, int y) {
        boolean cellVal = cells[y][x];
        int liveAdj = liveAdjacentCells(x, y);
        if (cellVal) {
            return liveAdj > 1 && liveAdj < 4;
        }
        return liveAdj==3;
    }

    //adds a cell and its adjacent ones to the set of changed cells
    private void addAdjacentCellsToChangedSet(int x, int y) {
        for (Coordinate2D coord : IGameOfLife.adjacentCells) {
            changedCells.add(new Coordinate2D(IGameOfLife.modulus(x + coord.getX(), width), IGameOfLife.modulus(y + coord.getY(), height)));
        }
        changedCells.add(new Coordinate2D(x, y));
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

    public Set<Coordinate2D> getDebuggingCoords() {
        return changedCells;
    }

    public void cleanup() {

    }

    
}
