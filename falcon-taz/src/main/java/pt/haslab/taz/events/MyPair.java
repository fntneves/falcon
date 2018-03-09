package pt.haslab.taz.events;

/**
 * Created by nunomachado on 31/03/17.
 */
public class MyPair<First,Second> {

    private First first;
    private Second second;

    public MyPair(First first, Second second) {
        this.first = first;
        this.second = second;
    }

    public void setFirst(First first) {
        this.first = first;
    }

    public void setSecond(Second second) {
        this.second = second;
    }

    public First getFirst() {
        return first;
    }

    public Second getSecond() {
        return second;
    }

    public void set(First first, Second second) {
        setFirst(first);
        setSecond(second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyPair pair = (MyPair) o;

        //if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
        //if (second != null ? !second.equals(pair.second) : pair.second != null) return false;

        //consider that a pair (A,B) is equal to (B,A)
        if (first != null && first.equals(pair.getFirst()) && second != null && second.equals(pair.getSecond()))
            return true;
        if (first != null && first.equals(pair.getSecond()) && second != null && second.equals(pair.getFirst()))
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * (result + (second != null ? second.hashCode() : 0));
        return result;
    }

    @Override
    public String toString() {
        String fst, snd;
        fst = (first != null)? first.toString() : " ";
        snd = (second != null)? second.toString() : " ";
        return "(" + fst.toString() + ", " + snd.toString() + ")";
    }
}
