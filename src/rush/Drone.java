package rush;

import battlecode.common.*;

import java.util.ArrayList;

import static rush.Util.*;
import static rush.Util.directions;

public class Drone extends Unit {

    public Drone(RobotController r) { super(r); }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        System.out.println("My wander value is " + nav.getWander());
        System.out.println("My stuck value is " + nav.getStuck());
        System.out.println("My threats are: " + nav.getThreats().toString());
        // check if it needs to explode
        if (nav.getStuck() >= explodeThresh) {
            explode = true;
            return;
        }
        // TODO: this part is bugged
//        // check if it is going to be carrying a landscaper for attack mode
//        if (phase == actionPhase.ATTACK && Vector.vectorSubtract(rc.getLocation(), HQLocation).equals(new Vector(0, 1))) {
//            RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(Direction.WEST));
//            if (r != null && r.getType() == RobotType.LANDSCAPER && r.getTeam() != rc.getTeam()) {
//                isAttackerBuilder = true;
//                if (rc.canPickUpUnit(r.getID())) {
//                    rc.pickUpUnit(r.getID());
//                }
//            }
//        }
        // check if outer layer is completed then never go inside again
//        if (isOuterLayer) {
//            System.out.println("inner layer is completed!");
//            System.out.println("My distance is: " + rc.getLocation().distanceSquaredTo(shiftedHQLocation));
//            // if within distance 13 of HQ first things first move away
//            if (rc.getLocation().distanceSquaredTo(shiftedHQLocation) <= 13) {
//                Direction optDir = rc.getLocation().directionTo(shiftedHQLocation).opposite();
//                for (int i = 0; i < 8; i++) {
//                    if (rc.canMove(optDir)) {
//                        rc.move(optDir);
//                        break;
//                    } else {
//                        optDir = optDir.rotateRight();
//                    }
//                }
//            }
//        }
        // check for help mode
        if (helpMode == 0 && !rc.isCurrentlyHoldingUnit()) {
            // check for unit to help
            // check for all of helpLoc if there's anything, if so, change helpMode and order of helpLoc
            helpIndex = -1;
            int closestDist = helpRadius;
            // first prune the helpLoc list
            ArrayList<Pair> removeHelp = new ArrayList<>();
            for (Pair value : helpLoc) {
                MapLocation helpAt = value.getKey();
                if (rc.canSenseLocation(helpAt)) {
                    RobotInfo r = rc.senseRobotAtLocation(helpAt);
                    if (r == null || (r.getType() != RobotType.MINER && r.getType() != RobotType.LANDSCAPER) || r.getTeam() != rc.getTeam()) {
                        removeHelp.add(value);
                    }
                }
            }
            for (Pair pair : removeHelp) {
                helpLoc.remove(pair);
            }
            for (int i = 0; i < helpLoc.size(); i++) {
                int helpDist = helpLoc.get(i).getKey().distanceSquaredTo(rc.getLocation());
                if (helpDist <= closestDist) {
                    closestDist = helpDist;
                    helpIndex = i;
                }
            }
            if (helpIndex != -1) {
                // only help people if you don't know enemy hq location or you're not bugging around enemyHQ
                if (enemyHQLocation == null || rc.getLocation().distanceSquaredTo(enemyHQLocation) > enemyPatrolRadiusMax) helpMode = 1;
            }
        }
        // if helping
        if (helpMode != 0) {
            System.out.println("Currently on help mode " + helpMode);
            // helping mode on!
            if (helpMode == 1) {
                //
                MapLocation strandLoc = helpLoc.get(helpIndex).getKey();
                if (rc.canSenseLocation(strandLoc)) {
                    RobotInfo r = rc.senseRobotAtLocation(strandLoc);
                    if (r == null) {
                        helpLoc.remove(helpIndex);
                        helpMode = 0;
                    } else {
                        if (rc.getLocation().isAdjacentTo(strandLoc)) {
                            if (rc.canPickUpUnit(r.getID())) {
                                rc.pickUpUnit(r.getID());
                                helpMode = 2;
                            }
                        }
                    }
                }
                System.out.println("Navigating to " + strandLoc.toString());
                nav.bugNav(rc, strandLoc);
            } else if (helpMode == 2) {
                MapLocation requestLoc = helpLoc.get(helpIndex).getValue();
                if (requestLoc.equals(HQLocation) && isTurtle || rc.getLocation().isAdjacentTo(requestLoc)) {
                    Direction optDir = rc.getLocation().directionTo(requestLoc);
                    for (int i = 0; i < 8; i++) {
                        if (rc.canDropUnit(optDir) && !rc.senseFlooding(rc.getLocation().add(optDir))) {
                            rc.dropUnit(optDir);
                            helpLoc.remove(helpIndex);
                            helpMode = 0;
                            break;
                        } else {
                            optDir = optDir.rotateRight();
                        }
                    }
                } else {
                    System.out.println("Navigating to " + requestLoc);
                    nav.bugNav(rc, requestLoc);
                }
            }
        } else {
            // if not helping and no one to help
            switch(phase) {
                case NON_ATTACKING:
                    System.out.println("Threats are: " + nav.getThreats().toString());
                    // find opponent units and pick up
                    if (!rc.isCurrentlyHoldingUnit()) {
                        System.out.println("I'm not holding any units!");
                        // find opponent units
                        RobotInfo pickup = null;
                        for (RobotInfo r : rc.senseNearbyRobots()) {
                            if (r.getTeam() != rc.getTeam() && (r.getType() == RobotType.MINER || r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.COW)) {
                                if (pickup == null || r.getLocation().distanceSquaredTo(rc.getLocation()) < pickup.getLocation().distanceSquaredTo(rc.getLocation())) {
                                    if (r.getType() == RobotType.COW) {
                                        if (enemyHQLocation == null || r.getLocation().distanceSquaredTo(enemyHQLocation) > 48)
                                            pickup = r;
                                    } else {
                                        if (enemyHQLocation == null || r.getLocation().distanceSquaredTo(enemyHQLocation) >= 8)
                                            pickup = r;
                                    }
                                }
                            }
                        }
                        if (pickup != null) {
                            // if can pickup do pickup
                            if (pickup.getLocation().isAdjacentTo(rc.getLocation())) {
                                System.out.println("Just picked up a " + pickup.getType());
                                if (rc.canPickUpUnit(pickup.getID())) {
                                    isCow = pickup.getType() == RobotType.COW;
                                    rc.pickUpUnit(pickup.getID());
                                }
                            } else {
                                // if not navigate to that unit
                                nav.bugNav(rc, pickup.getLocation());
                                System.out.println("Navigating to unit at " + pickup.getLocation().toString());
                            }
                        } else {
                            // if there are no robots nearby
                            if (enemyHQLocation != null) {
                                if (rc.getID() % 6 == 0) {
                                    // let some drones patrol
                                    nav.bugNav(rc, enemyHQLocationSuspect);
                                } else if (rc.getID() % 2 == 1) {
                                    patrolHQ();
                                } else nav.bugNav(rc, enemyHQLocation);
                            } else {
                                // too many rushes that we can't deal with without drones
                                if (rc.getID() % 2 == 1) {
                                    System.out.println("I'm patrolling!");
                                    patrolHQ();
                                } else {
                                    nav.searchEnemyHQ(rc);
                                }
                            }
                        }
                    } else {
                        // find water if not cow
                        System.out.println("I'm holding a unit!");
                        if (isCow) {
                            // go to enemyHQ
                            boolean canPlace = false;
                            if (enemyHQLocation != null) {
                                if (rc.getLocation().distanceSquaredTo(enemyHQLocation) < 24) {
                                    Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                                    for (int i = 0; i < 8; i++) {
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            canPlace = true;
                                            break;
                                        } else optDir = optDir.rotateRight();
                                    }
                                }
                            }
                            if (enemyHQLocation != null && !canPlace) {
                                nav.bugNav(rc, enemyHQLocation);
                            }
                            nav.searchEnemyHQ(rc);
                        } else {
                            MapLocation water = findWater();
                            MapLocation robotLoc = rc.getLocation();
                            if (water != null) {
                                for (Direction dir: directions) {
                                    MapLocation loc = rc.getLocation().add(dir);
                                    if (rc.canSenseLocation(loc) && rc.senseFlooding(loc)) {
                                        if (rc.canDropUnit(dir)) rc.dropUnit(dir);
                                    }
                                }
                                // if no water
                                if (rc.isReady()) nav.bugNav(rc, water);
                            } else {
                                // explore
                                if (exploreTo == null || suspectsVisited.get(exploreTo)) {
                                    nav.nextExplore();
                                }
                                System.out.println("I'm exploring to " + exploreTo.toString());
                                nav.bugNav(rc, exploreTo);
                            }
                        }
                    }
                    break;
                case PREPARE:
                    if (enemyHQLocation.distanceSquaredTo(rc.getLocation()) >= enemyPatrolRadiusMax) {
                        // if too far, move in
                        nav.bugNav(rc, enemyHQLocation);
                        break;
                    } else if (enemyHQLocation.distanceSquaredTo(rc.getLocation()) < enemyPatrolRadiusMin) {
                        // if too close, move out
                        nav.bugNav(rc, HQLocation);
                    }
                    // with in the area, move to closest possible position around enemy hq
                    MapLocation minDistancedSafe = rc.getLocation();
                    int min_dist = enemyHQLocation.distanceSquaredTo(minDistancedSafe);
                    MapLocation nextPrepareLocation;
                    for (Direction dir : directions) {
                        nextPrepareLocation = rc.getLocation().add(dir);
                        if (min_dist > enemyHQLocation.distanceSquaredTo(nextPrepareLocation) &&
                                enemyHQLocation.distanceSquaredTo(nextPrepareLocation) >= 25 &&
                                nav.canGoDrone(rc, dir, true)) {
                            rc.move(dir);
                            break;
                        }
                    }
                    break;
                case ATTACK:
                    if (rc.isCurrentlyHoldingUnit() && !isAttackerBuilder) {
                        // throw unit out if it's not our unit
                        // check the 8 adjacent tiles and see if there's any water
                        boolean unitDropped = false;
                        for (Direction d : directions) {
                            if (rc.senseFlooding(rc.getLocation().add(d))) {
                                if (rc.canDropUnit(d)) {
                                    rc.dropUnit(d);
                                    unitDropped = true;
                                    break;
                                }
                            }
                        }
                        if (!unitDropped) {
                            // else, move out using even manhattan distance
                            MapLocation nextEscapeLocation;
                            int currentDistToEnemyHQ = rc.getLocation().distanceSquaredTo(enemyHQLocation);
                            for (Direction dir : directions) {
                                nextEscapeLocation = rc.getLocation().add(dir);
                                // manhattan distaance is odd makes a lattice
                                // even or closer make sure no dense positions
                                // check if empty
                                if (manhattanDistance(enemyHQLocation, nextEscapeLocation) % 2 == 0 &&
                                        (manhattanDistance(enemyHQLocation, rc.getLocation()) % 2 == 1 || nextEscapeLocation.distanceSquaredTo(enemyHQLocation) <= currentDistToEnemyHQ) &&
                                        rc.canMove(dir)) {
                                    rc.move(dir);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (isAttackerBuilder) {
                            // first, check if they can see enemyHQ
                            if (rc.canSenseLocation(enemyHQLocation)) {
                                // empty spots
                                ArrayList<MapLocation> emptySpots = new ArrayList<>();
                                // non buildings and drones spot
                                ArrayList<MapLocation> nonBuildingSpots = new ArrayList<>();
                                // check HQ surroundings and see if there's any openings
                                for (Direction dir: directions) {
                                    if (rc.canSenseLocation(enemyHQLocation.add(dir))) {
                                        RobotInfo r = rc.senseRobotAtLocation(enemyHQLocation.add(dir));
                                        if (r == null) emptySpots.add(enemyHQLocation.add(dir));
                                        else if (r.getTeam() != rc.getTeam() && !r.getType().isBuilding() && r.getType() != RobotType.DELIVERY_DRONE) {
                                            nonBuildingSpots.add(enemyHQLocation.add(dir));
                                        }
                                    }
                                }
                                // find the closest empty spot next to HQ
                                MapLocation closestEmptySpot = null;
                                for (MapLocation spots: emptySpots) {
                                    if (closestEmptySpot == null || rc.getLocation().distanceSquaredTo(spots) < rc.getLocation().distanceSquaredTo(closestEmptySpot)) {
                                        closestEmptySpot = spots;
                                    }
                                }
                                MapLocation closestNonBuildingSpot = null;
                                for (MapLocation spots: nonBuildingSpots) {
                                    if (closestNonBuildingSpot == null || rc.getLocation().distanceSquaredTo(spots) < rc.getLocation().distanceSquaredTo(closestNonBuildingSpot)) {
                                        closestNonBuildingSpot = spots;
                                    }
                                }
                                if (closestEmptySpot != null) {
                                    // if there's an empty spot navigate there and place your unit
                                    if (rc.getLocation().isAdjacentTo(closestEmptySpot)) {
                                        Direction optDir = rc.getLocation().directionTo(closestEmptySpot);
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            isAttackerBuilder = false;
                                        }
                                    } else {
                                        // if you're placing a unit move in even manhattan distance
                                        moveManhattan(closestEmptySpot, 0);
                                    }
                                }
                                else if (closestNonBuildingSpot != null) {
                                    // if there's an non building spot then hope that some other drone will come pick that unit up and then it'll convert to closestEmptySpot
                                    if (rc.getLocation().isAdjacentTo(closestNonBuildingSpot)) {
                                        Direction optDir = rc.getLocation().directionTo(closestNonBuildingSpot);
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            isAttackerBuilder = false;
                                        }
                                    } else {
                                        // if you're placing a unit move in even manhattan distance
                                        moveManhattan(closestNonBuildingSpot, 0);
                                    }
                                }
                                else {
                                    // if all of HQ is enclosed, then place them somewhere on land and make them dig the buildings (just deploying will do)
                                    if (rc.getLocation().distanceSquaredTo(enemyHQLocation) > 8) {
                                        // if they're not close enough, move in more
                                        moveManhattan(enemyHQLocation, 0);
                                    } else {
                                        // check all adjacent locations and see if they're land and placable
                                        Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                                        for (int i = 0; i < 8; i++) {
                                            if (!rc.senseFlooding(rc.getLocation().add(optDir))) {
                                                if (rc.canDropUnit(optDir)) {
                                                    rc.dropUnit(optDir);
                                                    break;
                                                }
                                            }
                                            optDir = optDir.rotateRight();
                                        }
                                        // if there's no available locations, still move manhattan
                                        moveManhattan(enemyHQLocation, 0);
                                    }
                                }
                            } else {
                                // otherwise move in
                                // they move with even parity because they have priority
                                moveManhattan(enemyHQLocation, 0);
                            }
                        } else {
                            // if next to any units pick them up
                            RobotInfo[] robotsmall = rc.senseNearbyRobots(2);
                            for (RobotInfo r : robotsmall) {
                                if (r.getTeam() != rc.getTeam() && (r.getType() == RobotType.MINER || r.getType() == RobotType.LANDSCAPER)) {
                                    if (rc.canPickUpUnit(r.getID())) {
                                        rc.pickUpUnit(r.getID());
                                        break;
                                    }
                                }
                            }
                            // get into attack radius of enemy netgun
                            moveManhattan(enemyHQLocation, 1);
                        }
                    }
                    break;
                case SURRENDER:
                    if (enemyHQLocation != null && rc.getLocation().distanceSquaredTo(enemyHQLocation) < enemyPatrolRadiusMin) {
                        // if close, move away
                        nav.bugNav(rc, HQLocation);
                    } else {
                        // if far, change back to normal state
                        phase = RobotPlayer.actionPhase.NON_ATTACKING;
                    }
                    break;
                case DEFENSE:
                    if (rc.isCurrentlyHoldingUnit()) {
                        // find water if not cow
                        System.out.println("I'm holding a unit!");
                        if (isCow) {
                            // go to enemyHQ
                            boolean canPlace = false;
                            if (enemyHQLocation != null) {
                                if (rc.getLocation().distanceSquaredTo(enemyHQLocation) < 24) {
                                    Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                                    for (int i = 0; i < 8; i++) {
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            canPlace = true;
                                            break;
                                        } else optDir = optDir.rotateRight();
                                    }
                                }
                            }
                            if (enemyHQLocation != null && !canPlace) {
                                nav.bugNav(rc, enemyHQLocation);
                            }
                            nav.searchEnemyHQ(rc);
                        } else {
                            MapLocation water = findWater();
                            MapLocation robotLoc = rc.getLocation();
                            if (water != null) {
                                if (water.isAdjacentTo(robotLoc)) {
                                    System.out.println("Dropping off unit!");
                                    // drop off unit
                                    Direction dropDir = robotLoc.directionTo(water);
                                    if (rc.canDropUnit(dropDir)) rc.dropUnit(dropDir);
                                } else {
                                    System.out.println("Navigating to water at " + water.toString());
                                    nav.bugNav(rc, water);
                                }
                            } else {
                                // explore
                                if (exploreTo == null || suspectsVisited.get(exploreTo)) {
                                    nav.nextExplore();
                                }
                                System.out.println("I'm exploring to " + exploreTo.toString());
                                nav.bugNav(rc, exploreTo);
                            }
                        }
                    } else {
                        // find opponent units
                        RobotInfo pickup = null;
                        for (RobotInfo r : rc.senseNearbyRobots()) {
                            if (r.getTeam() != rc.getTeam() && (r.getType() == RobotType.MINER || r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.COW)) {
                                if (pickup == null || r.getLocation().distanceSquaredTo(rc.getLocation()) < pickup.getLocation().distanceSquaredTo(rc.getLocation())) {
                                    if (r.getType() == RobotType.COW) {
                                        if (enemyHQLocation == null || r.getLocation().distanceSquaredTo(enemyHQLocation) > 48)
                                            pickup = r;
                                    } else {
                                        if (enemyHQLocation == null || r.getLocation().distanceSquaredTo(enemyHQLocation) >= 8)
                                            pickup = r;
                                    }
                                }
                            }
                        }
                        if (pickup != null) {
                            // if can pickup do pickup
                            if (pickup.getLocation().isAdjacentTo(rc.getLocation())) {
                                System.out.println("Just picked up a " + pickup.getType());
                                if (rc.canPickUpUnit(pickup.getID())) {
                                    isCow = pickup.getType() == RobotType.COW;
                                    rc.pickUpUnit(pickup.getID());
                                }
                            } else {
                                // if not navigate to that unit
                                nav.bugNav(rc, pickup.getLocation());
                                System.out.println("Navigating to unit at " + pickup.getLocation().toString());
                            }
                        } else {
                            // if there are no robots nearby
                            patrolHQ();
                        }
                    }

            }
        }
        System.out.println("I'm at " + rc.getLocation().toString());
    }

    static int manhattanDistance(MapLocation loc1, MapLocation loc2){
        return  Math.abs(loc1.x-loc2.x) + Math.abs(loc1.y-loc2.y);
    }

    // move to location with the desired parity of manhattan distance

    // NOTE: PARITY WITH ENEMYHQ
    static void moveManhattan(MapLocation loc, int parity) throws GameActionException {
        int nonparity;
        if (parity == 0) nonparity = 1;
        else nonparity = 0;
        int currentDist = rc.getLocation().distanceSquaredTo(loc);
        MapLocation nextAttackLocation;
        for (Direction dir : directions) {
            nextAttackLocation = rc.getLocation().add(dir);
            // manhattan distance is odd makes a lattice
            // even or closer make sure no dense positions
            // check if empty
            if (manhattanDistance(enemyHQLocation, nextAttackLocation) % 2 == parity &&
                    (manhattanDistance(enemyHQLocation, rc.getLocation()) % 2 == nonparity || nextAttackLocation.distanceSquaredTo(loc) <= currentDist) &&
                    rc.canMove(dir)) {
                rc.move(dir);
                break;
            }
        }
    }

    public void patrolHQ() throws GameActionException {
        Direction rotateDir = rc.getLocation().directionTo(HQLocation);
        int distHQ = rc.getLocation().distanceSquaredTo(HQLocation);
        if (distHQ < friendlyPatrolRadiusMin) {
            rotateDir = rotateDir.opposite();
        } else if (distHQ <= friendlyPatrolRadiusMax) {
            rotateDir = rotateDir.rotateLeft();
            rotateDir = rotateDir.rotateLeft();
        }
        for (int i = 0; i < 8; i++) {
            if (nav.canGo(rc, rotateDir, true)) rc.move(rotateDir);
            else rotateDir = rotateDir.rotateRight();
        }
        if (rc.canMove(rotateDir)) rc.move(rotateDir);
    }

    static MapLocation findWater() throws GameActionException {
        MapLocation water = null;
        int maxV = 2;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                if (x == 0 && y == 0) continue;
                MapLocation check = rc.getLocation().translate(x, y);
                if (rc.canSenseLocation(check)) {
                    if (rc.senseFlooding(check)) {
                        // find the closest maxmimal soup deposit
                        int checkDist = check.distanceSquaredTo(rc.getLocation());
                        if (water == null || checkDist < water.distanceSquaredTo(rc.getLocation()))
                            water = check;
                    }
                }
            }
        }
        if (water != null) return water;
        int closestDist = 0;
        for (MapLocation w : waterLocation) {
            // find the closest water
            int waterDist = w.distanceSquaredTo(rc.getLocation());
            if ((water == null || waterDist < closestDist) && !rc.getLocation().equals(w)) {
                closestDist = waterDist;
                water = w;
            }
        }
        return water;
    }
}
