package rngform;

import battlecode.common.*;

import static rngform.Cast.infoQ;

public class LandscaperFactory extends Building {
    public LandscaperFactory(RobotController r) {
        super(r);

    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (factoryLocation == null) {
            factoryLocation = rc.getLocation();
            infoQ.add(Cast.getMessage(Cast.InformationCategory.FACTORY, factoryLocation));
        }
        // spawning 5 turtle landscapers with a bit of leeway for refineries
        if (landscaperCount < 5 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost+150) {
            Direction optDir = rc.getLocation().directionTo(HQLocation);
            if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                rc.buildRobot(RobotType.LANDSCAPER, optDir);
                landscaperCount++;
            }
        }
        // spawning the other 3 to complete the turtle as soon as we have enough money
        if (landscaperCount < 8 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost+75) {
            Direction optDir = rc.getLocation().directionTo(HQLocation);
            if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                rc.buildRobot(RobotType.LANDSCAPER, optDir);
                landscaperCount++;
            }
        }
        // spawn the teraforming landscapers
        if (landscaperCount < 12 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 300) {
            Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (loc.x % 3 == HQLocation.x % 3 && loc.y % 3 == HQLocation.y % 3) {
                    optDir = optDir.rotateRight();
                    continue;
                }
                if (rc.getLocation().directionTo(HQLocation).equals(optDir)) {
                    optDir = optDir.rotateRight();
                    continue;
                }
                if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, optDir);
                    landscaperCount++;
                    break;
                }
                optDir = optDir.rotateRight();
            }
        }
        if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost+300) {
            Direction optDir = rc.getLocation().directionTo(HQLocation);
            if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                rc.buildRobot(RobotType.LANDSCAPER, optDir);
                landscaperCount++;
            }
        }
        if (landscaperCount < 25 && rc.getTeamSoup() >= 900) {
            Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
            for (int i = 0; i < 3; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (loc.x % 3 == HQLocation.x % 3 && loc.y % 3 == HQLocation.y % 3) {
                    optDir = optDir.rotateRight();
                    continue;
                }
                if (rc.getLocation().directionTo(HQLocation).equals(optDir)) {
                    optDir = optDir.rotateRight();
                    continue;
                }
                if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, optDir);
                    landscaperCount++;
                    break;
                }
                optDir = optDir.rotateRight();
            }
        }
    }
}
