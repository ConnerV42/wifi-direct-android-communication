package com.breeze.application;

public abstract class BreezeModule {
    private BreezeAPI api;
    public BreezeModule(BreezeAPI api) {
        this.api = api;
    }
}
