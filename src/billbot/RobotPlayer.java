package billbot;

import battlecode.common.*;
import java.util.Random;

public class RobotPlayer {

    static int turnCount = 0;
    static final Random rng = new Random(6147);
    static MapLocation spawnLoc = null;

    static final int HOME_RADIUS = 64;

    static final Direction[] directions = {
            Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    static final UnitType[] towerTypes = {
            UnitType.LEVEL_ONE_PAINT_TOWER,
            UnitType.LEVEL_ONE_MONEY_TOWER,
            UnitType.LEVEL_ONE_DEFENSE_TOWER
    };

    public static void run(RobotController rc) throws GameActionException {

        if (spawnLoc == null) spawnLoc = rc.getLocation();

        MapLocation loc = rc.getLocation();

        if (rc.getType() == UnitType.SOLDIER
                && rc.senseMapInfo(loc).getPaint() == PaintType.EMPTY
                && rc.canAttack(loc)) {
            rc.attack(loc);
        }

        while (true) {

            turnCount++;

            try {

                switch (rc.getType()) {
                    case SOLDIER:
                        runSoldier(rc);
                        break;

                    case MOPPER:
                        runMopper(rc);
                        break;

                    case SPLASHER:
                        runSplasher(rc);
                        break;

                    default:
                        runTower(rc);
                }

            } catch (GameActionException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Clock.yield();
        }
    }

   //Tower Logic

    static void runTower(RobotController rc) throws GameActionException {

        int round = rc.getRoundNum();
        int chips = rc.getChips();
        MapLocation loc = rc.getLocation();

        switch (rc.getType().getBaseType()) {

            case LEVEL_ONE_PAINT_TOWER:

                if (chips > 2500 && rc.canUpgradeTower(loc)) {
                    RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

                    if (allies.length >= 3 || chips > 3500) {
                        rc.upgradeTower(loc);
                        return;
                    }
                }
                break;

            default:

                if (chips > 3600 && rc.canUpgradeTower(loc)) {
                    RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

                    if (allies.length > 3 || chips > 4000) {
                        rc.upgradeTower(loc);
                        return;
                    }
                }
        }

        int roll = rng.nextInt(100);
        UnitType typeToSpawn;

        switch (round < 200 ? 0 : round < 1000 ? 1 : 2) {

            case 0:
                typeToSpawn = roll < 90 ? UnitType.SOLDIER : UnitType.SPLASHER;
                break;

            case 1:
                typeToSpawn = roll < 35 ? UnitType.SOLDIER :
                        roll < 70 ? UnitType.SPLASHER : UnitType.MOPPER;
                break;

            default:
                typeToSpawn = roll < 20 ? UnitType.SOLDIER :
                        roll < 70 ? UnitType.SPLASHER : UnitType.MOPPER;
        }

        for (int i = directions.length; i-- > 0;) {

            MapLocation spawn = loc.add(directions[i]);

            if (rc.canBuildRobot(typeToSpawn, spawn)) {
                rc.buildRobot(typeToSpawn, spawn);
                return;
            }
        }
    }

    // Soldier Logic

    static void runSoldier(RobotController rc) throws GameActionException {

        MapLocation myLoc = rc.getLocation();
        int distFromSpawn = myLoc.distanceSquaredTo(spawnLoc);

        paintSelf(rc);

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);

        MapLocation closestRuin = null;
        int minDist = 99999;

        for (int i = nearbyTiles.length; i-- > 0;) {

            MapInfo tile = nearbyTiles[i];

            if (!tile.hasRuin()) continue;

            MapLocation ruinLoc = tile.getMapLocation();
            RobotInfo robot = rc.senseRobotAtLocation(ruinLoc);

            if (robot != null && robot.getType().isTowerType()) continue;

            int dist = myLoc.distanceSquaredTo(ruinLoc);

            if (dist < minDist) {
                minDist = dist;
                closestRuin = ruinLoc;
            }
        }

        if (closestRuin != null) {
            buildTower(rc, closestRuin);
            return;
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        for (int i = enemies.length; i-- > 0;) {

            RobotInfo enemy = enemies[i];

            if (!enemy.getType().isTowerType()) continue;

            if (rc.canAttack(enemy.location)) {
                rc.attack(enemy.location);
                return;
            }

            greedyMoveToward(rc, enemy.location);
            return;
        }

        if (distFromSpawn < HOME_RADIUS) {

            for (int i = nearbyTiles.length; i-- > 0;) {

                MapInfo tile = nearbyTiles[i];

                if (tile.getPaint() != PaintType.EMPTY) continue;

                MapLocation loc = tile.getMapLocation();

                if (rc.canAttack(loc)) {
                    rc.attack(loc);
                    return;
                }

                greedyMoveToward(rc, loc);
                return;
            }
        }
    }


    static void buildTower(RobotController rc, MapLocation ruin) throws GameActionException {

        boolean[][] pattern = rc.getTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER);

        MapLocation bestMoveTarget = null;
        int bestDist = 99999;

        for (int i = 5; i-- > 0;) {
            for (int j = 5; j-- > 0;) {

                int offsetX = i - 2;
                int offsetY = j - 2;

                MapLocation tileLoc = new MapLocation(ruin.x + offsetX, ruin.y + offsetY);

                if (!rc.onTheMap(tileLoc)) continue;

                MapInfo tileInfo = rc.senseMapInfo(tileLoc);

                PaintType targetPaint = pattern[i][j] ?
                        PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;

                if (tileInfo.getPaint() == targetPaint) continue;

                if (rc.canAttack(tileLoc)) {
                    rc.attack(tileLoc, pattern[i][j]);
                    return;
                }

                int dist = rc.getLocation().distanceSquaredTo(tileLoc);

                if (dist < bestDist) {
                    bestDist = dist;
                    bestMoveTarget = tileLoc;
                }
            }
        }

        if (bestMoveTarget != null) {
            greedyMoveToward(rc, bestMoveTarget);
            return;
        }

        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruin)) {
            rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruin);
        }
    }

    static void paintSelf(RobotController rc) throws GameActionException {

        MapLocation loc = rc.getLocation();

        if (rc.senseMapInfo(loc).getPaint() != PaintType.ALLY_PRIMARY
                && rc.canAttack(loc)) {
            rc.attack(loc);
        }
    }

    // Splasher Logic

    static void runSplasher(RobotController rc) throws GameActionException {

        MapLocation myLoc = rc.getLocation();

        MapLocation bestTarget = null;
        int bestScore = 0;

        for (int dx = 3; dx >= -3; dx--) {
            for (int dy = 3; dy >= -3; dy--) {

                MapLocation target = new MapLocation(myLoc.x + dx, myLoc.y + dy);

                if (!rc.onTheMap(target)) continue;

                int score = evaluateSplash(rc, target);

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = target;
                }
            }
        }

        if (bestTarget != null && bestScore > 30 && rc.canAttack(bestTarget)) {
            rc.attack(bestTarget);
            return;
        }

        MapInfo[] tiles = rc.senseNearbyMapInfos(-1);

        MapLocation closestEnemyPaint = null;
        int minDist = 99999;

        for (int i = tiles.length; i-- > 0;) {

            MapInfo tile = tiles[i];

            if (!tile.getPaint().isEnemy()) continue;

            int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());

            if (dist < minDist) {
                minDist = dist;
                closestEnemyPaint = tile.getMapLocation();
            }
        }

        if (closestEnemyPaint != null) {
            greedyMoveToward(rc, closestEnemyPaint);
            return;
        }

        explore(rc);
    }

    static int evaluateSplash(RobotController rc, MapLocation center) throws GameActionException {

        int score = 0;

        for (int dx = 1; dx >= -1; dx--) {
            for (int dy = 1; dy >= -1; dy--) {

                MapLocation tile = new MapLocation(center.x + dx, center.y + dy);

                if (!rc.onTheMap(tile)) continue;

                PaintType paint = rc.senseMapInfo(tile).getPaint();

                switch (paint) {
                    case ENEMY_PRIMARY:
                    case ENEMY_SECONDARY:
                        score += 10;
                        break;

                    case EMPTY:
                        score += 1;
                }
            }
        }

        return score;
    }

    
    //Mopper Logic

    static void runMopper(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos(-1);

        MapLocation closestEnemyPaint = null;
        int minDist = 99999;

        for (int i = tiles.length; i-- > 0;) {

            MapInfo tile = tiles[i];

            if (!tile.getPaint().isEnemy()) continue;

            int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());

            if (dist < minDist) {
                minDist = dist;
                closestEnemyPaint = tile.getMapLocation();
            }
        }

        if (closestEnemyPaint != null) {

            if (rc.canAttack(closestEnemyPaint)) {
                rc.attack(closestEnemyPaint);
                return;
            }

            greedyMoveToward(rc, closestEnemyPaint);

            Direction dir = rc.getLocation().directionTo(closestEnemyPaint);

            if (rc.canMopSwing(dir)) rc.mopSwing(dir);

            return;
        }

        explore(rc);
    }


    static void greedyMoveToward(RobotController rc, MapLocation target) throws GameActionException {

        MapLocation myLoc = rc.getLocation();

        Direction bestDir = null;
        int bestScore = 99999;

        for (int i = directions.length; i-- > 0;) {

            Direction dir = directions[i];

            if (!rc.canMove(dir)) continue;

            MapLocation newLoc = myLoc.add(dir);

            int score = newLoc.distanceSquaredTo(target);

            RobotInfo[] nearby = rc.senseNearbyRobots(newLoc, 2, rc.getTeam());

            score += nearby.length * 3;

            if (score < bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        if (bestDir != null) rc.move(bestDir);
    }

    static void explore(RobotController rc) throws GameActionException {

        int id = rc.getID();

        MapLocation exploreTarget = new MapLocation(
                (id * 13) % rc.getMapWidth(),
                (id * 17) % rc.getMapHeight()
        );

        if (rc.getLocation().distanceSquaredTo(exploreTarget) < 4) {

            exploreTarget = new MapLocation(
                    rng.nextInt(rc.getMapWidth()),
                    rng.nextInt(rc.getMapHeight())
            );
        }

        greedyMoveToward(rc, exploreTarget);
    }
}