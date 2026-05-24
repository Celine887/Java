package backend.academy.linktracker.ai.model;

public enum Priority {
    HIGH,
    MEDIUM,
    LOW;

    public static Priority max(Priority p1, Priority p2) {
        if (p1 == HIGH || p2 == HIGH) {
            return HIGH;
        }
        if (p1 == MEDIUM || p2 == MEDIUM) {
            return MEDIUM;
        }
        return LOW;
    }
}
