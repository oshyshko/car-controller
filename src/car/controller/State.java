package car.controller;

public class State {
    public final int direction; // -100=left     | 0=center  | 100=right
    public final int speed;     // -100=backward | 0=neutral | 100=forward

    public State(int direction, int speed) {
        this.direction = direction;
        this.speed = speed;
    }

    public String toJson() {
        return "[" + direction + ", " + speed + "]";
    }

    public String toString() {
        return toJson();
    }

    // generated code
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (Float.compare(state.direction, direction) != 0) return false;
        if (Float.compare(state.speed, speed) != 0) return false;

        return true;
    }

    public int hashCode() {
        int result = (direction != +0.0f ? Float.floatToIntBits(direction) : 0);
        result = 31 * result + (speed != +0.0f ? Float.floatToIntBits(speed) : 0);
        return result;
    }
}
