package teraform;

import battlecode.common.*;

import static teraform.Cast.getMessage;
import static teraform.Cast.infoQ;
import static teraform.Util.directions;

public class Landscaper extends Unit {

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
    }

    public void initialize() throws GameActionException {
        super.initialize();
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (teraformMode == 0) {
            System.out.println("Initially I have: " + Clock.getBytecodesLeft());
            if (teraformLoc[0] == null) {
                System.out.println("My teraformLoc is: null");
            } else {
                for (MapLocation loc : teraformLoc) {
                    System.out.println("My teraformLoc is: " + loc.toString());
                }
            }
            // if in position to build turtle, build it instead
//            if (rc.getLocation().distanceSquaredTo(HQLocation) <= 8) teraformMode = 1;
                // build the teraform
                // assume landscaper factory is distance 10 away from HQ
            if (rc.getLocation().distanceSquaredTo(HQLocation) > 2 && rc.getLocation().distanceSquaredTo(HQLocation) < 300 && (enemyHQLocation == null || !(rc.getLocation().distanceSquaredTo(enemyHQLocation) < 36 && rc.getLocation().distanceSquaredTo(HQLocation) > 36))) {
                System.out.println("Case 1");
                Direction fill = fillTo();
                Direction dig = holeTo();
                if (fill == null) {
                    System.out.println("No place to fill");
                    // no place to fill, check if we need to shave off height instead
                    Direction digLoc = digTo();
                    System.out.println("After checking digging locations, I have: " + Clock.getBytecodesLeft());
                    if (digLoc == null) {
                        System.out.println("No place to dig");
                        // nothing to do here, move onto another location after crossing this one out
                        MapLocation closeHole = rc.getLocation().add(dig);
                        if (fillMore(closeHole)) {
                            System.out.println("There's more to do!");
                            moveTo(closeHole);
                        } else {
                            System.out.println("Sending hole");
                            infoQ.add(getMessage(Cast.InformationCategory.HOLE, closeHole));
                            MapLocation hole = closestHole();
                            System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                            if (rc.getID() % 2 == 0) {
                                if (hole != null) {
                                    System.out.println("closest hole is: " + hole);
                                    moveTo(hole);
                                } else {
                                    moveTo(enemyHQLocationSuspect);
                                }
                            } else {
                                moveTo(enemyHQLocationSuspect);
                            }
                        }
                    } else {
                        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                            if (rc.canDigDirt(digLoc)) rc.digDirt(digLoc);
                        } else {
                            if (rc.canDepositDirt(dig)) rc.depositDirt(dig);
                        }
                    }

                } else {
                    if (rc.getDirtCarrying() == 0) {
                        if (rc.canDigDirt(dig)) rc.digDirt(dig);
                    } else {
                        if (rc.canDepositDirt(fill)) rc.depositDirt(fill);
                    }
                }
            } else {
                MapLocation hole = closestHole();
                System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                if (hole != null) {
                    System.out.println("closest hole is: " + hole);
                    moveTo(hole);
                } else {
                    moveTo(enemyHQLocationSuspect);
                }
            }
        }
        if (teraformMode == 1) {
            // build the turtle
//            if (rc.getLocation().distanceSquaredTo(HQLocation) <= 2) {
//                // if adjacent, dig under
//                if (rc.getDirtCarrying() == 0) {
//                    if (rc.canDigDirt(Direction.CENTER)) rc.digDirt(Direction.CENTER);
//                }
//                else {
//                    Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
//                    if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
//                }
//            }
//            else if (rc.getLocation().distanceSquaredTo(HQLocation) <= 8) {
//                // dig from opposite
//                if (rc.getDirtCarrying() == 0) {
//                    Direction digDir = rc.getLocation().directionTo(HQLocation);
//                    if (rc.canDigDirt(digDir)) {
//                        rc.digDirt(digDir);
//                    }
//                } else {
//                    if (rc.canDepositDirt(Direction.CENTER)) {
//                        rc.depositDirt(Direction.CENTER);
//                    }
//                }
//            }
        }
    }

    public Direction holeTo() {
        int modX = HQLocation.x % 3;
        int modY = HQLocation.y % 3;
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            if (dig.x % 3 == modX && dig.y % 3 == modY) {
                return dir;
            }
        }
        // this shouldn't happen
        return Direction.CENTER;
    }

    public int optHeight(MapLocation loc) {
        int distFromFactory = loc.distanceSquaredTo(factoryLocation);
        return Math.min(12, (int) (Math.floor(Math.sqrt(distFromFactory)/2)) + factoryHeight);
    }

    public Direction fillTo() throws GameActionException {
        Direction dig = holeTo();
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)
                        && (rob == null || !(rob.getType().isBuilding() && rob.getTeam() == rc.getTeam()))) return dir;
            }
        }
        // if can't find anything
        return null;
    }

    public Direction digTo() throws GameActionException {
        Direction dig = holeTo();
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if ((rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) || (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0)) return dir;
            }
        }
        // if can't find anything
        return null;
    }
    // scans teraformLoc and checks
    public MapLocation closestHole() throws GameActionException {
        if (teraformLoc[0] == null) return null;
        MapLocation closest = null;
        int heuristic = 0;
        for (MapLocation hole: teraformLoc) {
            int holeH = rc.getLocation().distanceSquaredTo(hole)+HQLocation.distanceSquaredTo(hole);
            if (closest == null || holeH < heuristic) {
                closest = hole;
                heuristic = holeH;
            }
        }
        return closest;
    }

    // because we reduced everything (except deep tiles or super tall tiles) we should be able to move pretty freely
    public void moveTo(MapLocation loc) throws GameActionException {
        Direction optDir = rc.getLocation().directionTo(loc);
        Direction left = optDir.rotateLeft();
        Direction right = optDir.rotateRight();
        Direction leftLeft = left.rotateLeft();
        Direction rightRight = right.rotateRight();
        Direction leftLeftLeft = leftLeft.rotateLeft();
        Direction rightRightRight = rightRight.rotateRight();
        Direction op = optDir.opposite();
        if (canMove(optDir)) rc.move(optDir);
        else if (canMove(left)) rc.move(left);
        else if (canMove(right)) rc.move(right);
        else if (canMove(leftLeft)) rc.move(leftLeft);
        else if (canMove(rightRight)) rc.move(rightRight);
        else if (canMove(leftLeftLeft)) rc.move(leftLeftLeft);
        else if (canMove(rightRightRight)) rc.move(rightRightRight);
        else if (canMove(op)) rc.move(op);
        else {
            // TODO: call for drone help
        }
    }

    public boolean canMove(Direction dir) throws GameActionException {
        Direction hole = holeTo();
        MapLocation loc = rc.getLocation().add(dir);
        return !hole.equals(dir) && rc.canMove(dir) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc);
    }

    public boolean fillMore(MapLocation hole) throws GameActionException {
        for (Direction dir: directions) {
            MapLocation fill = hole.add(dir);
            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if (((rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)) || (rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) ||
                        (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()) || (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0))
                && !(rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying == 0)){
                    if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)) System.out.println("first");
                    if (rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) System.out.println("second");
                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()) System.out.println("third");
                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0) System.out.println("fourth");
                    System.out.println("Direction " + dir + " looks ok");
                    System.out.println("optimal height is " + optHeight(fill));
                    return true;
                }
            }
        }
        return false;
    }
}