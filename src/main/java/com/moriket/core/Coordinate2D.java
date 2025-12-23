package com.moriket.core;

public class Coordinate2D {
    
    private int x;
    private int y;

    public Coordinate2D() {
        this.x = 0;
        this.y = 0;
    }

    public Coordinate2D(int x, int y) { 
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Coordinate2D add (Coordinate2D other) { 
        return new Coordinate2D(x + other.x, y + other.y);
    }

    //Necessary for creating sets of coordinates recognized to be unique
    public int hashCode() {
        return (397 * x) ^ y;
    }
    public String toString() {
        return "(" + x + "," + y + ")";
    }
    public boolean equals(Object other) {
        //System.out.println(hashCode());
        if (other instanceof Coordinate2D) {
            return equals((Coordinate2D)other);
        }
        return this == other;
    }
    public boolean equals(Coordinate2D other) {
        return this.x == other.x && this.y == other.y;
    }
}
