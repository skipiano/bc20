package pure_teraform;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    public enum actionPhase{
        NON_ATTACKING,
        PREPARE,
        ATTACK,
        SURRENDER,
        DEFENSE
    }

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {
        Robot me = null;
        switch (rc.getType()) {
            case HQ:                 me = new HQ(rc);           break;
            case MINER:              me = new Miner(rc);        break;
            case REFINERY:           me = new Refinery(rc);     break;
            case VAPORATOR:          me = new Vaporator(rc);    break;
            case DESIGN_SCHOOL:      me = new LandscaperFactory(rc); break;
            case FULFILLMENT_CENTER: me = new DroneFactory(rc);     break;
            case LANDSCAPER:         me = new Landscaper(rc);   break;
            case DELIVERY_DRONE:     me = new Drone(rc);         break;
            case NET_GUN:            me = new Shooter(rc);      break;
        }

        while(true) {
            try {
                me.takeTurn();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
