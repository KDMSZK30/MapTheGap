
/**
 * Class with Coord type
 *
 * @param <X>
 * @param <Y>
 * @param <Inf>
 */
public class Coord<X, Y, Inf> implements java.io.Serializable {

    private X x;
    private Y y;
    private Inf inf;

    public Coord(X x, Y y, Inf inf) {
        this.x = x;
        this.y = y;
        this.inf = inf;
    }

    public X x() {
        return x;
    }

    public Y y() {
        return y;
    }

    public Inf inf() {
        return inf;
    }

    public void Set(X x, Y y, Inf inf) {
        if (x != null) {
            this.x = x;
        }
        if (y != null) {
            this.y = y;
        }
        if (inf != null) {
            this.inf = inf;
        }
    }

    @Override
    public int hashCode() {
        return x.hashCode() ^ y.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Coord)) {
            return false;
        }
        Coord pairo = (Coord) o;
        return this.x.equals(pairo.x())
                && this.y.equals(pairo.y());
    }

}
