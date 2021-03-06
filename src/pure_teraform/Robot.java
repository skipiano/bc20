package pure_teraform;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static pure_teraform.Cast.getMessage;
import static pure_teraform.Cast.infoQ;
import static pure_teraform.Util.directions;

public class Robot {
    static RobotController rc;

    // spawn variables
    static int turnCount = 0;
//    static int factoryHeight;
    static int sizeX;
    static int sizeY;
    static int jitter = 0;

    // navigation object
    static Nav nav = null;
    // communication object
    static Cast cast = null;

    // hole array
    static boolean[][] holeLocation;

    // important locations
    static MapLocation HQLocation = null;
    static MapLocation enemyHQLocation = null;
    static Vector[] vapInside;
    static MapLocation[] vapInsideLoc;
    static ArrayList<MapLocation> soupLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> waterLocation = new ArrayList<MapLocation>();
    static MapLocation[] teraformLoc = new MapLocation[3];
    // only miners use the following
    static MapLocation soupLoc = null;
    static MapLocation closestRefineryLocation = null;
    // only drones use following
    static RobotInfo closestEnemyUnit;
    static ArrayList<Pair> helpLoc = new ArrayList<>();

    static RobotPlayer.actionPhase phase= RobotPlayer.actionPhase.NON_ATTACKING;

    // states
    // for drones:   0: not helping   1: finding stranded   2: going to requested loc
    // for miners:   0: normal        1: requesting help
    static int helpMode = 0;
    static int helpIndex = -1;

    // for landscapers:    0: building teraform   1: building 5x5 turtle
    static int teraformMode = 0;

    static boolean isBuilder;
    static boolean isAttacker;

    // booleans
    // is drone holding a cow
    static boolean isCow = false;
    // explode the unit
    static boolean explode = false;
    // is HQ under attack
    static boolean isUnderAttack = false;
    // is drone carrying another attacker unit
    static boolean isAttackerBuilder = false;
    // have drones spawned yet
    static boolean areDrones = false;
    // have we completed the innermost teraform yet
    static boolean completeTeraform = false;

    // used for exploring enemy HQ locations
    static int idIncrease = 0;

    // suspected enemy HQ location
    static MapLocation enemyHQLocationSuspect;
    // possible navigation locations
    static ArrayList<MapLocation> suspects = null;
    static Map<MapLocation, Boolean> suspectsVisited = new HashMap<>();
    // currently navigating to
    static MapLocation exploreTo;

    // unit counter
    static int minerCount = 0;
    static int droneCount = 0;
    static int landscaperCount = 0;
    static int vaporatorCount = 0;


    public Robot(RobotController r) {
        rc = r;
        cast = new Cast(rc);
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
        if (turnCount == 1) {
            initialize();
        } else {
            closestEnemyUnit=null;
            cast.getInfo(rc.getRoundNum()-1);
            cast.collectInfo();
        }
    }

    // when a unit is first created it calls this function
    public void initialize() throws GameActionException {
        if (rc.getType() == RobotType.HQ) {
            cast.collectInfo();
            findHoleSize();
        } else {
            int round = 1;
            while (HQLocation == null) {
                cast.getInfo(round);
                round++;
            }
            nav = new Nav();
            cast.getAllInfo();
        }

        exploreLoc();

        if (rc.getType() == RobotType.MINER) {
            vapInside = new Vector[]{new Vector(1, 0), new Vector(0,1), new Vector(-1, 0), new Vector(0, -1), new Vector(-1, 2), new Vector(1, 2), new Vector(2, 1), new Vector(2, -1), new Vector(1, -2), new Vector(-1, -2), new Vector(-2, -1), new Vector(-2, 1)};
            vapInsideLoc = new MapLocation[12];
            for (int i = 0; i < 12; i++) {
                vapInsideLoc[i] = vapInside[i].addWith(HQLocation);
            }
            if (rc.getRoundNum() == 2) {
                isBuilder = true;
            }
            if (rc.getRoundNum() == 3) isAttacker = true;
            return;
        }
        if (rc.getType() == RobotType.LANDSCAPER) {
            teraformLoc[0] = null;
            teraformLoc[1] = null;
            teraformLoc[2] = null;
//            // find design school and record location
//            if (factoryLocation == null) {
//                for (Direction dir : directions) {
//                    MapLocation loc = rc.getLocation().add(dir);
//                    if (rc.canSenseLocation(loc)) {
//                        RobotInfo factory = rc.senseRobotAtLocation(loc);
//                        if (factory != null && factory.getType() == RobotType.DESIGN_SCHOOL && factory.getTeam() == rc.getTeam()) {
//                            factoryLocation = loc;
//                            factoryHeight = rc.senseElevation(factoryLocation);
//                            System.out.println("direction of factory is: " + dir.toString());
//                            break;
//                        }
//                    }
//                }
//            } else {
//                if (rc.canSenseLocation(factoryLocation)) {
//                    factoryHeight = rc.senseElevation(factoryLocation);
//                }
//            }
        }
        if (rc.getType() == RobotType.VAPORATOR) {
            // send out signal
            infoQ.add(getMessage(Cast.InformationCategory.VAPORATOR, rc.getLocation()));
        }
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

    // generates locations we can explore
    static void exploreLoc() throws GameActionException {
        suspects = new ArrayList<>(Arrays.asList(horRef(HQLocation), verRef(HQLocation), horVerRef(HQLocation), new MapLocation(0, 0), new MapLocation(rc.getMapWidth()-1, 0), new MapLocation(0, rc.getMapHeight()), new MapLocation(rc.getMapWidth()-1, rc.getMapHeight()-1), new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2)));
        for (MapLocation loc: suspects) {
            suspectsVisited.put(loc, false);
        }
        enemyHQLocationSuspect = suspects.get(rc.getID() % 3);
    }

    // find how many holes can fit in the map and initialize the array
    static void findHoleSize() {
        int minX = HQLocation.x % 2;
        int minY = HQLocation.y % 2;
        int maxX = HQLocation.x;
        int maxY = HQLocation.y;
        while (maxX < rc.getMapWidth()) {
            maxX += 2;
        }
        while (maxY < rc.getMapHeight()) {
            maxY += 2;
        }
        System.out.println("maxX: " + maxX);
        System.out.println("minX: " + minX);
        System.out.println("maxY: " + maxY);
        System.out.println("minY: " + minY);

        sizeX = (maxX-minX+2)/2;
        sizeY = (maxY-minY+2)/2;
        System.out.println("sizeX: " + sizeX);
        System.out.println("sizeY: " + sizeY);
        holeLocation = new boolean[sizeX][sizeY];
    }

    public boolean surroundedLand(MapLocation pos) throws GameActionException {
        // return true if surrounded by land
        for (int i = 0 ; i<8 ; i++){
            if ( rc.canSenseLocation(pos.add(directions[i])) && rc.senseFlooding(pos.add(directions[i])) ){
                return false;
            }
        }
        return true;
    } 

    public boolean isHole(Direction dir){
        MapLocation pos =rc.getLocation().add(dir);
        return isHole(pos);
    }

    public boolean isHole(MapLocation pos){
        return pos.x%2 != HQLocation.x % 2 && pos.y%2 != HQLocation.y % 2;
    }
}