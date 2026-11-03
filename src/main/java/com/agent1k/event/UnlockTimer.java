package com.agent1k.event;

public record UnlockTimer(long unlockAt) {
    public boolean locked(long now) {
        return now < unlockAt;
    }

    public long remainingSeconds(long now) {
        return Math.max(0, (unlockAt - now + 999) / 1000);
    }
}
