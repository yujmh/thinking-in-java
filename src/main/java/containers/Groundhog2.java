package containers;

public class Groundhog2 extends Groundhog {
    public Groundhog2(int n) {
        super(n);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Groundhog && (number == ((Groundhog) o).number);
    }

    @Override
    public int hashCode() {
        return number;
    }
}
