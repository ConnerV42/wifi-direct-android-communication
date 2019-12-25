package com.breeze.application;

public abstract class BreezeModule {
    BreezeAPI api;
    public BreezeModule(BreezeAPI api) {
        this.api = api;
    }
}
