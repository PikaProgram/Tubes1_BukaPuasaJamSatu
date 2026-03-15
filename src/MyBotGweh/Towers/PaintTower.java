package MyBotGweh.Towers;

import battlecode.common.*;

public class PaintTower {

    static int splasherSpawned = 0;
    static int mopperSpawned = 0;
    static int soldierSpawned = 0;

    public static void run(RobotController rc) throws GameActionException {
        MoneyTower.attackEnemy(rc);

        //debug
        // if (rc.getRoundNum() % 50 == 0) {
        //     System.out.println("PaintTower round=" + rc.getRoundNum() + " paint=" + rc.getPaint());
        // }
        spawnUnit(rc);
    }

    static void spawnUnit(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        if(rc.getPaint() < 150) return; // Minimal paint untuk spawn unit

        int total = soldierSpawned + mopperSpawned + splasherSpawned;
        
        // Rasio soldier mopper splasher = 3:1:1
        UnitType toSpawn;
        int mod = total % 5;
        if (mod < 3) {
            toSpawn = UnitType.SOLDIER;
        } else if (mod == 3) {
            toSpawn = UnitType.MOPPER;
        } else {
            toSpawn = UnitType.SPLASHER;
        }

        MapLocation towerLoc = rc.getLocation();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                MapLocation spawnLoc = new MapLocation(towerLoc.x + dx, towerLoc.y + dy);
                if (rc.canBuildRobot(toSpawn, spawnLoc)) {
                    rc.buildRobot(toSpawn, spawnLoc);
                    if (toSpawn == UnitType.SOLDIER) soldierSpawned++;
                    else if (toSpawn == UnitType.MOPPER) mopperSpawned++;
                    else splasherSpawned++;
                    return;
                }
            }
        }
    }
}