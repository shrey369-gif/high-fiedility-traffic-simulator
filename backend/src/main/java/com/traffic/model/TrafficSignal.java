package com.traffic.model;

public class TrafficSignal {
    public enum SignalState { RED, YELLOW, GREEN }

    private static int nextId = 1;
    private final int id;
    private SignalState state;
    private int greenDuration;
    private int yellowDuration;
    private int redDuration;
    private int timer;
    private boolean adaptive;
    private int queueLength;

    public TrafficSignal(int greenDuration, int redDuration) {
        this.id = nextId++;
        this.state = SignalState.RED;
        this.greenDuration = greenDuration;
        this.yellowDuration = 3;
        this.redDuration = redDuration;
        this.timer = 0;
        this.adaptive = false;
        this.queueLength = 0;
    }

    public void tick() {
        timer++;
        switch (state) {
            case GREEN:
                if (timer >= greenDuration) {
                    state = SignalState.YELLOW;
                    timer = 0;
                }
                break;
            case YELLOW:
                if (timer >= yellowDuration) {
                    state = SignalState.RED;
                    timer = 0;
                }
                break;
            case RED:
                if (timer >= redDuration) {
                    state = SignalState.GREEN;
                    timer = 0;
                }
                break;
        }
    }

    public int getTimeRemaining() {
        switch (state) {
            case GREEN: return greenDuration - timer;
            case YELLOW: return yellowDuration - timer;
            case RED: return redDuration - timer;
            default: return 0;
        }
    }

    public void setAdaptive(boolean adaptive) { this.adaptive = adaptive; }
    public boolean isAdaptive() { return adaptive; }
    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
        if (adaptive) {
            this.greenDuration = Math.max(10, Math.min(60, 10 + queueLength * 3));
        }
    }
    public int getQueueLength() { return queueLength; }
    public int getId() { return id; }
    public SignalState getState() { return state; }
    public void setState(SignalState state) { this.state = state; this.timer = 0; }
    public int getGreenDuration() { return greenDuration; }
    public int getRedDuration() { return redDuration; }
}
