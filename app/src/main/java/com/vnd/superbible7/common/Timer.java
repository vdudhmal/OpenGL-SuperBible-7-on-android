package com.vnd.superbible7.common;

public class Timer {
    private long startTime;

    // Constructor that initializes startTime
    public Timer() {
        startTime = System.currentTimeMillis();
    }

    // Method to get the elapsed time in seconds (with fractions)
    public double getTotalTime() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - startTime) / 1000.0; // Convert to seconds as a double
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }
}