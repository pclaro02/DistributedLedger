package pt.tecnico.distledger.vectorclock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VectorClock {

    private final ArrayList<Integer> timeStamps;

    private final int numServers = 3;

    public VectorClock() {
        timeStamps = initTimeStamps();
    }

    public VectorClock(List<Integer> TSList) {
        timeStamps = initTimeStamps();
        for (int i = 0; i < numServers; i++) {
            setTimeStamp(i, TSList.get(i));
        }
    }

    private ArrayList<Integer> initTimeStamps() {
        final ArrayList<Integer> timeStamps;
        timeStamps = (ArrayList<Integer>) IntStream.of(new int[numServers])
                .boxed()
                .collect(Collectors.toList());
        return timeStamps;
    }

    public Integer getTimeStamp(Integer i) {
        return timeStamps.get(i);
    }

    public void setTimeStamp(Integer index, Integer value) {
        timeStamps.set(index, value);
    }

    public List<Integer> getTimeStamps(){
        return new ArrayList<>(timeStamps);
    }

    public void setTimeStamps(List<Integer> timeStamps) {
        for (int i = 0; i < numServers; i++) {
            this.setTimeStamp(i, timeStamps.get(i));
        }
    }

    public boolean greaterOrEqual(VectorClock v) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (this.getTimeStamp(i) < v.getTimeStamp(i))
                return false;
        }
        return true;
    }

    public boolean lessOrEqual(VectorClock v) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (this.getTimeStamp(i) > v.getTimeStamp(i))
                return false;
        }
        return true;
    }

    public boolean greaterThan(VectorClock v) {
        boolean isGreater = false;
        for (int i = 0; i < timeStamps.size(); i++) {
            if (this.getTimeStamp(i) < v.getTimeStamp(i)) {
                return false;
            }
            else if (this.getTimeStamp(i) > v.getTimeStamp(i)) {
                isGreater = true;
            }
        }
        return isGreater;
    }

    public void mergeWith(VectorClock v) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (this.getTimeStamp(i) < v.getTimeStamp(i))
                this.setTimeStamp(i, v.getTimeStamp(i));
        }
    }

    public void incrementTimeStamp(Integer position) {
        this.timeStamps.set(position, this.timeStamps.get(position) + 1);
    }
}
