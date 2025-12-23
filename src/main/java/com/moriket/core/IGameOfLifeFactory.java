package com.moriket.core;

public interface IGameOfLifeFactory {
    public IGameOfLife build(int width, int height);
}
