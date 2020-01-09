package bot01;

import battlecode.common.*;

import java.util.ArrayList;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;

    // game map, not implemented yet
    static int[][] map;

    // information queue waiting to be sent to blockchain
    static ArrayList<Integer> infoQ = new ArrayList<>();

    // constants
    // max vision of all units
    static int maxV = 6;
    // how many turns we wait before blockChain
    static int waitBlock = 10;

    // navigation object
    static Nav nav = new Nav();

    // important locations
    static MapLocation HQLocation = null;
    static MapLocation enemyHQLocation = null;
    static ArrayList<MapLocation> soupLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> waterLocation = new ArrayList<MapLocation>();


    // suspected enemy HQ location
    static MapLocation enemyHQLocationSuspect;
    // possible navigation locations
    static MapLocation[] suspects;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;
        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

                if (turnCount == 1) {
                    initialize();
                } else {
                    getInfo(rc.getRoundNum()-1);
                    collectInfo();
                }
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        // find drones and shoot them
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots) {
            if (r.getTeam() != rc.getTeam() && r.getType() == RobotType.DELIVERY_DRONE) {
                System.out.println("Shot a drone at " + r.getLocation());
                if (rc.canShootUnit(r.getID())) {
                    rc.shootUnit(r.getID());
                    break;
                }
            }
        }
        // build all the miners we can get in the first few turns
        if (rc.getRobotCount() < 5) {
            for (Direction d : Direction.allDirections()) {
                tryBuild(RobotType.MINER, d);
            }
        }
    }

    static void runMiner() throws GameActionException {
        // build drone factory if there isn't one
        if (rc.getRobotCount() == 5 && rc.getLocation().distanceSquaredTo(HQLocation) < 15 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
            for (Direction d : Direction.allDirections()) {
                tryBuild(RobotType.FULFILLMENT_CENTER, d);
            }
        }
        if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 200 && rc.getLocation().distanceSquaredTo(HQLocation) < 15) {
            for (Direction d : Direction.allDirections()) {
                tryBuild(RobotType.VAPORATOR, d);
            }
        }
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit || (findSoup() == null && rc.getSoupCarrying() > 0)) {
            // if the robot is full or has stuff and no more soup nearby, move back to HQ
            if (HQLocation != null) {
                // if HQ is next to miner deposit
                if (HQLocation.isAdjacentTo(rc.getLocation())) {
                    Direction soupDepositDir = rc.getLocation().directionTo(HQLocation);
                    tryRefine(soupDepositDir);
                    nav.navReset();
                } else {
                    nav.bugNav(rc, HQLocation);
                }
            }
        } else {
            MapLocation soupLoc = findSoup();
            if (soupLoc != null) {
                System.out.println("Soup is at: " + soupLoc.toString());
                Direction locDir = rc.getLocation().directionTo(soupLoc);
                if (rc.canMineSoup(locDir)) {
                    rc.mineSoup(locDir);
                    nav.navReset();
                }
                // if we can't mine soup, go to other soups
                else nav.bugNav(rc, soupLoc);
            } else {
                // TODO: think of strategy for scouting for soup
                // move to suspected enemy HQ
                nav.bugNav(rc, enemyHQLocationSuspect);
            }
        }
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {
        // no drones -> 6 units
        // produce 4 drones
        if (rc.getRobotCount() < 25) {
            for (Direction dir : directions)
                tryBuild(RobotType.DELIVERY_DRONE, dir);
        }
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {
        // find opponent units and pick up
        if (!rc.isCurrentlyHoldingUnit()) {
            System.out.println("I'm not holding any units!");
            // find opponent units
            RobotInfo pickup = null;
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.getTeam() != rc.getTeam() && (r.getType() == RobotType.MINER || r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.COW)) {
                    pickup = r;
                }
            }
            if (pickup != null) {
                // if can pickup do pickup
                if (pickup.getLocation().isAdjacentTo(rc.getLocation())) {
                    System.out.println("Just picked up a " + pickup.getType());
                    if (rc.canPickUpUnit(pickup.getID())) rc.pickUpUnit(pickup.getID());
                    nav.navReset();
                } else {
                    // if not navigate to that unit
                    nav.bugNav(rc, pickup.getLocation());
                    System.out.println("Navigating to unit at " + pickup.getLocation().toString());
                }
            } else {
                // if there are no robots nearby
                nav.bugNav(rc, enemyHQLocationSuspect);
                System.out.println("Searching for robots, navigating to suspected enemy HQ");
            }
        } else {
            // find water if not cow
            System.out.println("I'm holding a unit!");
            MapLocation water = null;
            MapLocation robotLoc = rc.getLocation();
            int maxV = 5;
            for (int x = -maxV; x <= maxV; x++) {
                for (int y = -maxV; y <= maxV; y++) {
                    MapLocation check = robotLoc.translate(x, y);
                    if (rc.canSenseLocation(check)) {
                        if (rc.senseFlooding(check)) {
                            // find the closest maxmimal soup deposit
                            if (water == null || check.distanceSquaredTo(rc.getLocation()) < water.distanceSquaredTo(rc.getLocation()))
                                water = check;
                        }
                    }
                }
            }
            if (water != null) {
                if (water.isAdjacentTo(robotLoc)) {
                    System.out.println("Dropping off unit!");
                    // drop off unit
                    Direction dropDir = robotLoc.directionTo(water);
                    if (rc.canDropUnit(dropDir)) rc.dropUnit(dropDir);
                    nav.navReset();
                } else {
                    System.out.println("Navigating to water at " + water.toString());
                    nav.bugNav(rc, water);
                }
            } else {
                // TODO: find water
                // for now, move randomly to try find water
                System.out.println("Moving randomly to find water!");
                nav.bugNav(rc, suspects[rc.getID() % 4]);
            }
        }
        nav.bugNav(rc, enemyHQLocationSuspect);
        System.out.println("I'm at " + rc.getLocation().toString());
    }

    static void runNetGun() throws GameActionException {
        // find drones and shoot them
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots) {
            if (r.getTeam() != rc.getTeam() && r.getType() == RobotType.DELIVERY_DRONE) {
                System.out.println("Shot a drone at " + r.getLocation());
                if (rc.canShootUnit(r.getID())) {
                    rc.shootUnit(r.getID());
                    break;
                }
            }
        }
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    // returns the current water level given turn count
    static double waterLevel() {
        double x = (double) turnCount;
        return Math.exp(0.0028 * x - 1.38 * Math.sin(0.00157 * x - 1.73) + 1.38 * Math.sin(-1.73)) - 1;
    }

    static int distSqr(int dx, int dy) {
        return dx * dx + dy * dy;
    }

    // returns the closest MapLocation of soup in the robot's vision radius
    static MapLocation findSoup() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        int maxV = 6;
        MapLocation soupLoc = null;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                MapLocation check = robotLoc.translate(x, y);
                if (rc.canSenseLocation(check)) {
                    if (rc.senseSoup(check) > 0) {
                        // find the closest maxmimal soup deposit
                        if (soupLoc == null || check.distanceSquaredTo(rc.getLocation()) < soupLoc.distanceSquaredTo(rc.getLocation())
                                || (check.distanceSquaredTo(rc.getLocation()) == soupLoc.distanceSquaredTo(rc.getLocation()) && rc.senseSoup(check) > rc.senseSoup(soupLoc)))
                            soupLoc = check;
                    }
                }
            }
        }
        return soupLoc;
    }

    // guesses enemy HQ
    static void findHQ() throws GameActionException {
        suspects = new MapLocation[]{horRef(HQLocation), verRef(HQLocation), horVerRef(HQLocation), HQLocation};
        enemyHQLocationSuspect = suspects[rc.getID() % 3];
//        rc.setIndicatorDot(enemyHQLocationSuspect, 255, 0, 0);
    }

    // when a unit is first created it calls this function
    static void initialize() throws GameActionException {
        if (rc.getType() == RobotType.HQ) {
            Cast.blockChain(Cast.InformationCategory.HQ, rc.getLocation(), rc);
            HQLocation = rc.getLocation();
        } else {
            getAllInfo();
        }
        findHQ();
    }

    // reflect horizontally
    static MapLocation horRef(MapLocation loc) {
        return new MapLocation(rc.getMapWidth() - 1 - loc.x, loc.y);
    }

    // reflect vertically
    static MapLocation verRef(MapLocation loc) {
        return new MapLocation(loc.x, rc.getMapHeight() - 1 - loc.y);
    }

    // reflect vertically and horizontally
    static MapLocation horVerRef(MapLocation loc) {
        return new MapLocation(rc.getMapWidth() - 1 - loc.x, rc.getMapHeight() - 1 - loc.y);
    }

    // get information from the blocks
    static void getAllInfo() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++) {
            if (i % 10 == 1 || i % 10 == 2 || i % 10 == 3) getInfo(i);
        }
    }

    // get information from blockchain on that turn
    static void getInfo(int roundNum) throws GameActionException {
        System.out.println("Getting info of round "+roundNum);
        Transaction[] info = rc.getBlock(roundNum);
        for (Transaction stuff: info) {
            for (int message: stuff.getMessage()) {
                System.out.println("message is: " + message);
                System.out.println("message validness is " + Cast.isValid(message, rc));
                System.out.println("message cat is" + Cast.getCat(message));
                System.out.println("message coord is" + Cast.getCoord(message));
                if (Cast.isValid(message, rc)) {
                    // if valid message
                    MapLocation loc = Cast.getCoord(message);
                    boolean doAdd;
                    System.out.println(Cast.getCat(message).toString());
                    switch (Cast.getCat(message)) {
                        case HQ:
                            HQLocation = loc;
                            break;
                        case ENEMY_HQ:
                            enemyHQLocation = loc;
                            break;
                        case NEW_SOUP:
                            // add if it's far away enough from all the other soup coords
                            doAdd = true;
                            for (MapLocation soup: soupLocation) {
                                if (soup.distanceSquaredTo(loc) <= 24) {
                                    doAdd = false;
                                    break;
                                }
                            }
                            if (doAdd) {
                                soupLocation.add(loc);
                            }
                            break;
                        case WATER:
                            // add if it's far away enough from all the other water coords
                            doAdd = true;
                            for (MapLocation water: waterLocation) {
                                if (water.distanceSquaredTo(loc) <= 24) {
                                    doAdd = false;
                                    break;
                                }
                            }
                            if (doAdd) {
                                waterLocation.add(loc);
                            }
                            break;
                        case REMOVE:
                            soupLocation.remove(loc);
                            waterLocation.remove(loc);
                            break;
                        // TODO: other cases we need to figure out
                    }
                }
            }
        }
    }

    // send info when the turn number is 1 mod waitBlock (10), otherwise keep saving data
    static void collectInfo() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r: robots) {
            if (enemyHQLocation == null && r.getType() == RobotType.HQ && r.getTeam() != rc.getTeam()) {
                enemyHQLocation = r.getLocation();
                infoQ.add(Cast.getMessage(Cast.InformationCategory.ENEMY_HQ, enemyHQLocation));
            }
        }
        boolean doAdd;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                MapLocation check = robotLoc.translate(x, y);
                if (rc.canSenseLocation(check)) {
                    // TODO: do stuff
                    // recording in order of importance
                    if (rc.senseSoup(check) > 0) {
                        doAdd = true;
                        for (MapLocation soup: soupLocation) {
                            if (soup.distanceSquaredTo(check) <= 24) {
                                doAdd = false;
                                break;
                            }
                        }
                        if (doAdd) {
                            soupLocation.add(check);
                            infoQ.add(Cast.getMessage(Cast.InformationCategory.NEW_SOUP, check));
                        }
                    }
                    if (rc.senseFlooding(check)) {
                        doAdd = true;
                        for (MapLocation water: waterLocation) {
                            if (water.distanceSquaredTo(check) <= 24) {
                                doAdd = false;
                                break;
                            }
                        }
                        if (doAdd) {
                            waterLocation.add(check);
                            infoQ.add(Cast.getMessage(Cast.InformationCategory.WATER, check));
                        }
                    }
                }
            }
        }
        if (rc.getRoundNum() % waitBlock == 1) sendInfo();
    }

    // send information collected to the blockchain
    static void sendInfo() throws GameActionException {
        if (!infoQ.isEmpty())  {
            int blockSize = Math.min(7, infoQ.size());
            int[] info = new int[blockSize];
            for (int i = 0; i < blockSize; i++) {
                info[i] = infoQ.get(0);
                infoQ.remove(0);
            }
            //
            if (rc.canSubmitTransaction(info, 5)) {
                System.out.println("Submitted transaction! Message is : " + info.toString());
                rc.submitTransaction(info, 5);
            }
        }
    }
}

