package nonrush;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import static nonrush.Cast.*;
import static nonrush.Util.attackLandscaperCount;
import static nonrush.Util.maxDroneCount;

public class DroneFactory extends Building {
    public DroneFactory(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        System.out.println("helpLoc length is: " + helpLoc.size());
        // produce 5 drones
        Direction optDir = Direction.NORTHEAST;
        if (droneCount < 5 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 150) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir) && rc.getTeamSoup() > 400){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    if (droneCount == 1) {
                        infoQ.add(getMessage(InformationCategory.DRONE_SPAWN, HQLocation));
                        areDrones = true;
                    }
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }
        optDir = Direction.NORTHEAST;
        if (droneCount < 8 && rc.getTeamSoup() >= RobotType.REFINERY.cost+RobotType.DELIVERY_DRONE.cost && helpLoc.size() >= 5) {
            for (int i = 0; i < 2; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir) && rc.getTeamSoup() > 350){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }

        // spam drones
        if (rc.getTeamSoup() >= 500) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)) {
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    if (droneCount == maxDroneCount-attackLandscaperCount) {
                        infoQ.add(Cast.getMessage(InformationCategory.PREPARE, HQLocation));
                    }
                    if (droneCount == maxDroneCount) {
                        infoQ.add(Cast.getMessage(InformationCategory.ATTACK, HQLocation));
                    }
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }


//        boolean isVaporator = false;
//        RobotInfo[] robots = rc.senseNearbyRobots();
//        for (RobotInfo r: robots) {
//            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
//                isVaporator = true;
//            }
//        }
//        // spam drones if we have a ton of soup
//        if (rc.getTeamSoup() >= 1000 && isVaporator) {
//            optDir = Direction.NORTH;
//            if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)) {
//                rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
//                droneCount++;
//            }
//            // if drones are almost max then prepare attack
//            if (droneCount == maxDroneCount-attackLandscaperCount) {
//                infoQ.add(Cast.getMessage(Cast.InformationCategory.PREPARE, HQLocation));
//            }
//            if (droneCount == maxDroneCount) {
//                infoQ.add(Cast.getMessage(Cast.InformationCategory.ATTACK, HQLocation));
//            }
//        }
    }
}
