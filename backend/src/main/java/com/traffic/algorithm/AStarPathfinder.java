package com.traffic.algorithm;

import com.traffic.model.Intersection;
import com.traffic.model.Road;
import java.util.*;

public class AStarPathfinder {

    public List<Road> findPath(Intersection start, Intersection goal, List<Road> allRoads) {
        if (start == null || goal == null || start.getId() == goal.getId()) {
            return Collections.emptyList();
        }

        Map<Integer, Double> gScore = new HashMap<>();
        Map<Integer, Double> fScore = new HashMap<>();
        Map<Integer, Road> cameFromRoad = new HashMap<>();
        Map<Integer, Intersection> cameFromNode = new HashMap<>();
        Set<Integer> closedSet = new HashSet<>();

        PriorityQueue<Intersection> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(a -> fScore.getOrDefault(a.getId(), Double.MAX_VALUE))
        );

        gScore.put(start.getId(), 0.0);
        fScore.put(start.getId(), heuristic(start, goal));
        openSet.add(start);

        while (!openSet.isEmpty()) {
            Intersection current = openSet.poll();

            if (current.getId() == goal.getId()) {
                return reconstructPath(cameFromRoad, cameFromNode, current, start);
            }

            closedSet.add(current.getId());

            for (Road road : current.getConnectedRoads()) {
                Intersection neighbor = null;
                if (road.getSource().getId() == current.getId()) {
                    neighbor = road.getDestination();
                } else if (road.getDestination().getId() == current.getId()) {
                    neighbor = road.getSource();
                }

                if (neighbor == null || closedSet.contains(neighbor.getId())) continue;

                double tentativeG = gScore.getOrDefault(current.getId(), Double.MAX_VALUE)
                    + road.getLength() * (1 + road.getDensity() * 5);

                if (tentativeG < gScore.getOrDefault(neighbor.getId(), Double.MAX_VALUE)) {
                    cameFromNode.put(neighbor.getId(), current);
                    cameFromRoad.put(neighbor.getId(), road);
                    gScore.put(neighbor.getId(), tentativeG);
                    fScore.put(neighbor.getId(), tentativeG + heuristic(neighbor, goal));
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return Collections.emptyList();
    }

    private double heuristic(Intersection a, Intersection b) {
        return a.distanceTo(b);
    }

    private List<Road> reconstructPath(Map<Integer, Road> cameFromRoad,
                                       Map<Integer, Intersection> cameFromNode,
                                       Intersection current, Intersection start) {
        List<Road> path = new ArrayList<>();
        while (current.getId() != start.getId() && cameFromRoad.containsKey(current.getId())) {
            path.add(0, cameFromRoad.get(current.getId()));
            current = cameFromNode.get(current.getId());
        }
        return path;
    }
}
