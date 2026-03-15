package MyBotGweh.Towers;

import battlecode.common.*;

public class MoneyTower {

    static int soldierSpawned = 0;

    public static void run(RobotController rc) throws GameActionException {
        attackEnemy(rc);
        spawnUnit(rc);
    }

    static void attackEnemy(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length == 0) return;

        RobotInfo weakest = enemies[0];
        for (RobotInfo enemy : enemies) {
            if (enemy.getHealth() < weakest.getHealth()) weakest = enemy;
        }
        if (rc.canAttack(weakest.getLocation())) rc.attack(weakest.getLocation());
    }

    static void spawnUnit(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;

        int mapArea = rc.getMapWidth() * rc.getMapHeight();
        UnitType toSpawn;
        if (mapArea >= 1600) {
            // Large: Soldier:Mopper:Splasher = 2:1:2
            int mod = soldierSpawned % 5;
            if (mod < 2) toSpawn = UnitType.SOLDIER;
            else if (mod == 2) toSpawn = UnitType.MOPPER;
            else toSpawn = UnitType.SPLASHER;
        } else if (mapArea >= 900) {
            // Medium: Soldier:Mopper:Splasher = 3:1:1
            int mod = soldierSpawned % 5;
            if (mod < 3) toSpawn = UnitType.SOLDIER;
            else if (mod == 3) toSpawn = UnitType.MOPPER;
            else toSpawn = UnitType.SPLASHER;
        } else {
            // Small: Soldier:Mopper = 3:1
            int mod = soldierSpawned % 4;
            toSpawn = (mod < 3) ? UnitType.SOLDIER : UnitType.MOPPER;
        }

        MapLocation towerLoc = rc.getLocation();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                MapLocation spawnLoc = new MapLocation(towerLoc.x + dx, towerLoc.y + dy);
                if (rc.canBuildRobot(toSpawn, spawnLoc)) {
                    rc.buildRobot(toSpawn, spawnLoc);
                    soldierSpawned++;
                    return;
                }
            }
        }
    }

}