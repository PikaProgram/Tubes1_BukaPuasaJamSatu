package main_bot.Units;

import battlecode.common.*;

public class Mopper {

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static Direction exploreDir = null;
    static int stepsInSameDir = 0;
    static final int MAX_STEPS = 6;
    static MapLocation exploreTarget = null;
    static Direction lastMoveDir = null;
    static MapLocation lastLoc = null;
    static int stuckTurns = 0;


    public static void run(RobotController rc) throws GameActionException {
        MapLocation now = rc.getLocation();
        if (lastLoc != null && now.equals(lastLoc)) stuckTurns = Math.min(stuckTurns + 1, 5);
        else stuckTurns = 0;
        lastLoc = now;

        // Prioritas 1: transfer cat ke soldier cat low
        if (transferToAlly(rc)) return;

        // Prioritas 2: swing ke arah yang paling banyak musuh
        if (mopSwing(rc)) return;

        // Prioritas 3: hapus cet musuh 
        if (mopEnemy(rc)) return;

        // Prioritas 4: gerak ke cet musuh yang terlihat
        if (moveToEnemyPaint(rc)) return;

        // Prioritas 5: ikuti soldier kalo ada
        followSoldier(rc);

        //debug
        // if (rc.getRoundNum() % 100 == 0) {
        //     System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
        // }
    }

    static boolean transferToAlly(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;
        if (rc.getPaint() < rc.getType().paintCapacity * 0.5) return false;

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        
        RobotInfo bestTarget = null;
        int lowestPaint = Integer.MAX_VALUE;

        for (RobotInfo ally : allies) {
            if (rc.getLocation().distanceSquaredTo(ally.getLocation()) > 2) continue;
            
            // Transfer ke tower 
            int cap = ally.getType().isTowerType() ? 1000 : ally.getType().paintCapacity;
            int threshold = ally.getType().isTowerType() ? 300 : (int)(cap * 0.4);
            
            if (ally.getPaintAmount() >= threshold) continue;

            if (ally.getPaintAmount() < lowestPaint) {
                lowestPaint = ally.getPaintAmount();
                bestTarget = ally;
            }
        }

        if (bestTarget == null) return false;

        int cap = bestTarget.getType().isTowerType() ? 1000 : bestTarget.getType().paintCapacity;
        int transferAmount = Math.min(rc.getPaint() / 2, cap - bestTarget.getPaintAmount());

        if (transferAmount > 0 && rc.canTransferPaint(bestTarget.getLocation(), transferAmount)) {
            rc.transferPaint(bestTarget.getLocation(), transferAmount);
            return true;
        }
        return false;
    }

    static boolean mopSwing(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;

        // Hitung musuh di setiap arah)
        Direction[] cardinals = {
            Direction.NORTH, Direction.EAST, 
            Direction.SOUTH, Direction.WEST
        };

        Direction bestDir = null;
        int maxEnemies = 0;

        for (Direction dir : cardinals) {
            if (!rc.canMopSwing(dir)) continue;

            int count = countEnemiesInDirection(rc, dir);
            if (count > maxEnemies) {
                maxEnemies = count;
                bestDir = dir;
            }
        }

        if (bestDir != null && maxEnemies > 0) {
            rc.mopSwing(bestDir);
            return true;
        }
        return false;
    }

    static int countEnemiesInDirection(RobotController rc, Direction dir) throws GameActionException {
        MapLocation pos = rc.getLocation();
        int count = 0;

        MapLocation step1 = pos.add(dir);
        MapLocation step2 = pos.add(dir).add(dir);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies) {
            MapLocation eLoc = enemy.getLocation();
            if (eLoc.distanceSquaredTo(step1) <= 1 || 
                eLoc.distanceSquaredTo(step2) <= 1) {
                count++;
            }
        }
        return count;
    }

    static boolean mopEnemy(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);

        MapLocation bestTarget = null;
        int nearestDist = Integer.MAX_VALUE;

        for (MapInfo tile : nearbyTiles) {
            if (!tile.getPaint().isEnemy()) continue;
            
            MapLocation tileLoc = tile.getMapLocation();
            int dist = rc.getLocation().distanceSquaredTo(tileLoc);
            
            if (dist <= 2 && dist < nearestDist) { 
                nearestDist = dist;
                bestTarget = tileLoc;
            }
        }

        if (bestTarget != null && rc.canAttack(bestTarget)) {
            rc.attack(bestTarget);
            return true;
        }
        return false;
    }

    static void followSoldier(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        RobotInfo nearestSoldier = null;
        int nearestDist = Integer.MAX_VALUE;

        for (RobotInfo ally : allies) {
            if (ally.getType() != UnitType.SOLDIER) continue;
            int dist = rc.getLocation().distanceSquaredTo(ally.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestSoldier = ally;
            }
        }

        if (nearestSoldier != null) {
            // ikut soldier tapi gak kedeketan >2
            if (nearestDist > 2) {
                moveToward(rc, nearestSoldier.getLocation());
            }

        } else {
            exploreByID(rc);
        }
    }

    static boolean moveToEnemyPaint(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return false;

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation bestTarget = null;
        int nearestDist = Integer.MAX_VALUE;

        for (MapInfo tile : nearbyTiles) {
            if (!tile.getPaint().isEnemy()) continue;
            if (!tile.isPassable()) continue;

            int dist = rc.getLocation().distanceSquaredTo(tile.getMapLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                bestTarget = tile.getMapLocation();
            }
        }

        if (bestTarget != null) {
            moveToward(rc, bestTarget);
            return true;
        }
        return false;
    }

    static void moveToward(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return;
        Direction dir = rc.getLocation().directionTo(target);
        if (dir == Direction.CENTER) return;
        boolean leftBias = ((rc.getID() + rc.getRoundNum()) & 1) == 0;
        Direction left = dir.rotateLeft(), right = dir.rotateRight();
        Direction left2 = left.rotateLeft(), right2 = right.rotateRight();
        Direction[] opts = leftBias
            ? new Direction[] {dir, left, right, left2, right2}
            : new Direction[] {dir, right, left, right2, left2};
        for (Direction d : opts) {
            if (!rc.canMove(d)) continue;
            if (stuckTurns < 2 && lastMoveDir != null && d == lastMoveDir.opposite()) continue;
            rc.move(d);
            lastMoveDir = d;
            return;
        }
        if (stuckTurns >= 2) {
            for (Direction d : directions) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    lastMoveDir = d;
                    return;
                }
            }
        }
    }

    static void exploreByID(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;

        MapLocation myLoc = rc.getLocation();
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();

        if (exploreTarget == null || myLoc.distanceSquaredTo(exploreTarget) <= 2) {
            exploreTarget = newExploreTarget(rc, myLoc, mapWidth, mapHeight);
            rc.setIndicatorString("Target: " + exploreTarget + " ID:" + rc.getID());
        }

        moveToward(rc, exploreTarget);
        if (rc.isMovementReady()) {
            for (Direction d : directions) {
                if (rc.canMove(d)) {
                    rc.move(d);
                    lastMoveDir = d;
                    return;
                }
            }
        }
    }

    static MapLocation newExploreTarget(RobotController rc, MapLocation myLoc, int mapWidth, int mapHeight) {
        int zoneW = mapWidth / 3;
        int zoneH = mapHeight / 3;
        int[] zoneOrder = getZoneOrder(rc.getID());

        for (int i = 0; i < 9; i++) {
            int zone = zoneOrder[i];
            int cx = Math.min(zoneW * (zone % 3) + zoneW / 2, mapWidth - 1);
            int cy = Math.min(zoneH * (zone / 3) + zoneH / 2, mapHeight - 1);
            MapLocation zoneLoc = new MapLocation(cx, cy);

            if (myLoc.distanceSquaredTo(zoneLoc) <= 9) continue;

            return zoneLoc;
        }

        return new MapLocation(mapWidth / 2, mapHeight / 2);
    }

    static int[] getZoneOrder(int robotID) {
        int lastDigit = robotID % 10;
        int[] digitToZone = {0, 2, 4, 6, 8, 1, 3, 5, 7, 0};
        int startZone = digitToZone[lastDigit];
        
        int[] result = new int[9];
        boolean[] used = new boolean[9];
        int idx = 0;
        
        result[idx++] = startZone;
        used[startZone] = true;
        
        for (int offset = 4; offset >= 1; offset--) {
            int zone1 = (startZone + offset) % 9;
            int zone2 = (startZone + 9 - offset) % 9;
            if (!used[zone1]) { result[idx++] = zone1; used[zone1] = true; }
            if (!used[zone2]) { result[idx++] = zone2; used[zone2] = true; }
        }
        for (int i = 0; i < 9; i++) {
            if (!used[i]) { result[idx++] = i; used[i] = true; }
        }
        
        return result;
    }
}