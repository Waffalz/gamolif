package com.moriket.core;

import java.util.Set;


public interface IGameOfLife {
    /*
    Interface to abstractly define Game of Life implementation behaviors
    Some constraints:
        > square world sizes of powers of 2
        > top left of the world shall be (0,0)
        > cell interactions ocurring across the world boundary will wrap around
    */

    /* 
    utility const with a precalculated list of adjacent cell offsets
    it'd be a waste of computational time to dynamically generate this
    every time we needed it
    */
    public static final Coordinate2D[] adjacentCells = new Coordinate2D[]{
                                                            new Coordinate2D(-1, -1), new Coordinate2D(0, -1), new Coordinate2D(1, -1),
                                                            new Coordinate2D(-1, 0),                            new Coordinate2D(1, 0), 
                                                            new Coordinate2D(-1, 1),  new Coordinate2D(0, 1),  new Coordinate2D(1, 1)};

    /*
     * Making my own mod func because Java won't let me have nice things
    */
    public static int modulus(int i, int mod) {
        return (((i % mod) + mod) % mod);
    }

    public void initializeCells(boolean[][] liveMatrix, Set<Coordinate2D> liveList);

    public void evolve();

    public boolean getValue(int x, int y);

    public void clear();
    public void reset();
    public void cleanup();

    public Set<Coordinate2D> getDebuggingCoords();
}