package com.bankmemory.util;

import javax.annotation.Nullable;

public class TickDebouncer {
    private final int tickDelay;

    @Nullable private Runnable action;
    private int tickCount;

    public TickDebouncer(int tickDelay) {
        this.tickDelay = tickDelay;
    }

    public void start(Runnable action) {
        this.action = action;
        this.tickCount = this.tickDelay;
    }

    public void tick() {
        if (this.action == null || this.tickCount < 1) {
            return;
        }
        this.tickCount--;
        if (this.tickCount == 0) {
            this.action.run();
            this.action = null;
        }
    }

    public void flush() {
        if (this.tickCount > 0 && this.action != null) {
            this.tickCount = 0;
            this.action.run();
            this.action = null;
        }
    }
}
