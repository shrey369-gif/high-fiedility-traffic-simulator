package com.traffic.algorithm;

import com.traffic.model.Intersection;
import com.traffic.model.Road;
import com.traffic.model.TrafficSignal;
import java.util.List;

public class AdaptiveSignalController {

    public void update(List<Intersection> intersections) {
        for (Intersection intersection : intersections) {
            TrafficSignal signal = intersection.getSignal();
            if (!signal.isAdaptive()) continue;

            int queueLength = 0;
            for (Road road : intersection.getConnectedRoads()) {
                if (road.getDestination().getId() == intersection.getId()) {
                    queueLength += countWaitingVehicles(road);
                }
            }
            signal.setQueueLength(queueLength);
        }
    }

    private int countWaitingVehicles(Road road) {
        int count = 0;
        for (var vehicle : road.getVehicles()) {
            if (vehicle.getProgress() > 0.7 && vehicle.getSpeed() < 10) {
                count++;
            }
        }
        return count;
    }
}
