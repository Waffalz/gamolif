package com.moriket.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import com.moriket.core.Coordinate2D;
import com.moriket.core.IGameOfLife;
import com.moriket.core.IGameOfLifeFactory;


public class GameOfLifeHashLife implements IGameOfLife{

    public static class GameOfLifeHashLifeFactory implements IGameOfLifeFactory {
        public GameOfLifeHashLifeFactory() {

        }

        public String toString() {
            return "Hashlife";
        }

        public IGameOfLife build(int width, int height) {
            return new GameOfLifeHashLife();
        }
    }

    private QuadTree rootNode;

    private HashMap<QuadTree, QuadTree> nodeCanon;
    private HashMap<QuadTree, QuadTree> stepCanon;

    //map of n -> T representing empty quadtrees of level n
    //ideally we'd want to rep these things with null, but I don't want to think about that right now
    //generally want this to be populated up to at least root level - 1 for expansion padding
    //expect these trees to also be contained in the nodeCanon
    private HashMap<Integer, QuadTree> emptyTrees;

    private QuadTree DEAD_CELL = new QuadTree(false);
    private QuadTree LIVE_CELL = new QuadTree(true);

    public GameOfLifeHashLife(){
        //rootNode = new QuadTree(null, null, null, null);
        nodeCanon = new HashMap<>();
        stepCanon = new HashMap<>();
        emptyTrees = new HashMap<>();

        emptyTrees.put(0, DEAD_CELL);

        initLevels();
    }

    public void initializeCells(boolean[][] initMatrix, Set<Coordinate2D> initList) {
        //initialize all level 0, 1, and 2 trees
        //init based on initMatrix
        rootNode = getQuadTreeFromMatrix(initMatrix);
    }

    public QuadTree getCanonNode(QuadTree nw, QuadTree ne, QuadTree sw, QuadTree se) {
        return getCanonNode(new QuadTree(nw, ne, sw, se));
    }

    public QuadTree getCanonNode(QuadTree quadassem) {
        QuadTree node = nodeCanon.get(quadassem);
        if (node != null) return node;
        node = new QuadTree(
            getCanonNode(quadassem.nw),
            getCanonNode(quadassem.ne), 
            getCanonNode(quadassem.sw), 
            getCanonNode(quadassem.se)
            );
        canonizeNode(node);
        return node;
    }

    public void canonizeNode(QuadTree node) {
        nodeCanon.put(node, node);
    }

    public QuadTree getCanonEvolution(QuadTree base) {
        QuadTree toReturn = stepCanon.get(base);
        //System.out.printf("\n%d %d", base.level, (toReturn!=null)?toReturn.level:-1);
        return toReturn;
    }

    public void canonizeEvolution(QuadTree base, QuadTree evolution) {
        canonizeNode(evolution);
        stepCanon.put(base, evolution);
    }

    public synchronized void evolve() {
        rootNode = getEvolution(getExpansion(rootNode));
    }

    public QuadTree getEvolution(QuadTree node) {
        //return if this has been done before
        QuadTree cachedEvolution = getCanonEvolution(node);
        if (cachedEvolution != null) return cachedEvolution;

        if (node.level == 1) throw new RuntimeException("Can't evolve a noncached level 1");

        //construct the nine
        QuadTree[] innerNodes = new QuadTree[9];
        innerNodes[0] = getCanonNode(node.nw); //topleft
        innerNodes[1] = getCanonNode(node.nw.ne, node.ne.nw, node.nw.se, node.ne.sw); //centerup
        innerNodes[2] = getCanonNode(node.ne); //topright
        innerNodes[3] = getCanonNode(node.nw.sw, node.nw.se, node.sw.nw, node.sw.ne); //leftCenter
        innerNodes[4] = getCenter(node); //center
        innerNodes[5] = getCanonNode(node.ne.sw, node.ne.se, node.se.nw, node.se.ne); //rightCenter
        innerNodes[6] = getCanonNode(node.sw); //bottomleft
        innerNodes[7] = getCanonNode(node.sw.ne, node.se.nw, node.sw.se, node.se.sw); //centerdown
        innerNodes[8] = getCanonNode(node.se); //bottomright

        QuadTree[] innerNodeEvolutions = Arrays.stream(innerNodes).map(inNode -> getEvolution(inNode)).toArray(QuadTree[]::new);

        QuadTree[] intermediateNode = new QuadTree[] {
            getCanonNode(innerNodeEvolutions[0], innerNodeEvolutions[1], innerNodeEvolutions[3], innerNodeEvolutions[4]),
            getCanonNode(innerNodeEvolutions[1], innerNodeEvolutions[2], innerNodeEvolutions[4], innerNodeEvolutions[5]),
            getCanonNode(innerNodeEvolutions[3], innerNodeEvolutions[4], innerNodeEvolutions[6], innerNodeEvolutions[7]),
            getCanonNode(innerNodeEvolutions[4], innerNodeEvolutions[5], innerNodeEvolutions[7], innerNodeEvolutions[8])
        };

        QuadTree evolution = getCanonNode(
            getCenter(intermediateNode[0]), 
            getCenter(intermediateNode[1]), 
            getCenter(intermediateNode[2]), 
            getCenter(intermediateNode[3]));
        canonizeEvolution(node, evolution);
        return evolution;
    }

    public QuadTree getCenter(QuadTree base) {
        //if (base.level < 2) throw new RuntimeException("Level too low to get center");
        return getCanonNode(base.nw.se, base.ne.sw, base.sw.ne, base.se.nw);
    }

    public QuadTree getExpansion(QuadTree base) {
        QuadTree emptyTree = getEmptyQuadTree(base.level-1); 
        return getCanonNode(
            getCanonNode(emptyTree, emptyTree, emptyTree, base.nw),
            getCanonNode(emptyTree, emptyTree, base.ne, emptyTree),
            getCanonNode(emptyTree, base.sw, emptyTree, emptyTree),
            getCanonNode(base.se, emptyTree, emptyTree, emptyTree)
        );
    }

    public QuadTree getEmptyQuadTree(int level) {
        QuadTree cachedTree = emptyTrees.get(level);
        if (cachedTree != null) return cachedTree;
        cachedTree = getCanonNode(getEmptyQuadTree(level-1), getEmptyQuadTree(level-1), getEmptyQuadTree(level-1), getEmptyQuadTree(level-1));
        emptyTrees.put(level, cachedTree);
        return cachedTree;
    }

    //Precondition: array is 2^n x 2^n
    public QuadTree getQuadTreeFromMatrix(boolean[][] array) {
        return getQuadTreeFromMatrix(array, 0, 0, array[0].length-1, array.length-1);
    }

    public QuadTree getQuadTreeFromMatrix(boolean[][] array, int x1, int y1, int x2, int y2) {
        if (x1 == x2 && y1 == y2) {
            return getLevel0Node(array[y1][x1]);
        }
        
        QuadTree nw = getQuadTreeFromMatrix(array, x1, y1, (x1 + x2)/2, (y1 + y2)/2);
        QuadTree ne = getQuadTreeFromMatrix(array, (x1 + x2 + 1)/2, y1, x2, (y1 + y2)/2);
        QuadTree sw = getQuadTreeFromMatrix(array, x1, (y1 + y2 + 1)/2, (x1 + x2)/2, y2);
        QuadTree se = getQuadTreeFromMatrix(array, (x1 + x2 + 1)/2, (y1 + y2 + 1)/2, x2, y2);

        QuadTree toReturn = getCanonNode(nw, ne, sw, se);
        return toReturn;
    }

    public QuadTree getLevel0Node(boolean value) {
        return (value)?LIVE_CELL:DEAD_CELL;
    }

    
    public void initLevels() {
        canonizeNode(LIVE_CELL);
        canonizeNode(DEAD_CELL);
        //level 2
        for (int i = 0; i < 65536; i++) {
            boolean[][] nodeMatrix = new boolean[4][4];
            for (int j = 0; j < 16; j++) {
                nodeMatrix[j/4][j%4] = (((i >> j) & 1) == 1);
            }
            //do this to canonize the node
            QuadTree fourTree = getQuadTreeFromMatrix(nodeMatrix);

            //generate evolution array from nodematrix
            QuadTree twoTree = getQuadTreeFromMatrix(applyGOF(nodeMatrix));
            canonizeEvolution(fourTree, twoTree);
        }
    }

    /*
     * for game initialization, apply GOF rules to a 4x4 grid to produce
     * evolution for the 2x2 inner square
     */
    private static boolean[][] applyGOF(boolean[][] base) {
        boolean[][] evolution = new boolean[2][2];
        for (int i = 1; i < 3; i++) {
            for (int j = 1; j < 3; j++) {
                boolean cellVal = base[i][j];
                int liveAdj = liveAdjacentCells(base, j, i);
                if (cellVal) {
                    evolution[i-1][j-1] = liveAdj > 1 && liveAdj < 4;
                } else {
                    evolution[i-1][j-1] = liveAdj==3;
                }
            }
        }
        return evolution;
    }

    //Returns number of adjacent cells
    private static int liveAdjacentCells(boolean[][] base, int x, int y) {
        int liveAdj = 0;
        for (Coordinate2D dir : IGameOfLife.adjacentCells) {
            liveAdj += base[y + dir.getY()][x + dir.getX()]?1:0;
        }
        return liveAdj;
    }

    public synchronized boolean getValue(int x, int y) {
        return getValue(rootNode, x, y);
    }

    public boolean getValue(QuadTree node, int x, int y) {
        int bounds = (int)Math.pow(2, node.level);
        int midPoint = bounds/2;
        //System.out.printf("%d(lim%d, mid%d) %d %d\n", node.level, bounds-1, bounds/2, x, y);
        if (node.level == 0) { 
            return node.value;
        }
        if (x < 0 || x >= bounds || y < 0 || y >= bounds) throw new RuntimeException("Recursive get out of bounds");
        
        if (y < midPoint) {
            if (x < midPoint) {
                return getValue(node.nw, x, y);
            } else {
                return getValue(node.ne, x-midPoint, y);
            }
        } else {
            if (x < midPoint) {
                return getValue(node.sw, x, y - midPoint);
            } else {
                return getValue(node.se, x - midPoint, y - midPoint);
            }
        }
    }

    public void clear() {

    }

    public void reset() {
        rootNode = new QuadTree(null, null, null, null);
    }

    public void cleanup() {

    }

    public Set<Coordinate2D> getDebuggingCoords() { return null; }

    public static class QuadTree {

        //cardinal directions
        //undef if level =>0
        //null quads indicate empty nodes
        public final QuadTree nw;
        public final QuadTree ne;
        public final QuadTree sw;
        public final QuadTree se;

        public final int level;

        //undef if level > 0
        public final boolean value;

        private int hash;
        private boolean hashCached;

        //Precondition: all arg nodes must be of the same level. Undef behavior otherwise
        public QuadTree(QuadTree nw, QuadTree ne, QuadTree sw, QuadTree se) {
            this.nw = nw;
            this.ne = ne;
            this.sw = sw;
            this.se = se;
            
            value = false;
            level = nw.level+1;

            hashCached = false;
        }

        public QuadTree(boolean value) {
            this.nw = null;
            this.ne = null;
            this.sw = null;
            this.se = null;

            this.value = value;
            level = 0;
        }

        public int hashCode() {
            if (hashCached) {
                return hash;
            }
            if (level > 0) { 
                hash = Objects.hash(nw.hashCode(), ne.hashCode(), sw.hashCode(), se.hashCode());
            } else {
                hash = value?1:0;
            }
            hashCached = true;
            return hash;
        }

        public boolean equals(Object other){
            if (this == other) return true;
            if (other instanceof QuadTree) {
                QuadTree otherNode = (QuadTree)other;
                return this.nw == otherNode.nw && this.ne == otherNode.ne && this.sw == otherNode.sw && this.se == otherNode.se;
            }
            return false;
        }
        
    }
}
