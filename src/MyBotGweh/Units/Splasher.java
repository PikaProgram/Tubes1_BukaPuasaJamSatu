package MyBotGweh.Units;

import battlecode.common.*;

public class Splasher {

    static final Direction[] directions = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,
    };


    // state per robot
    static MapLocation exploreTarget = null;
    static MapLocation knownTowerLoc = null;
    static MapLocation currentTarget = null;
    static MapLocation lastFailedTarget = null;
    static Direction lastMoveDir = null;
    static MapLocation lastLoc = null;
    static int stuckTurns = 0;
    static int exploreZoneIdx = 0;
    static int[] cachedZoneOrder = null;

    public static void run(RobotController rc) throws GameActionException {
        // stuck hanlde
        MapLocation now = rc.getLocation();
        if (lastLoc != null && now.equals(lastLoc)) stuckTurns = Math.min(stuckTurns + 1, 5);
        else stuckTurns = 0;
        lastLoc = now;

        //prioritas 1 = refill
        if (needsRefill(rc)) { 
            refillPaint(rc); return; 
        }
        
        // prioritas 2 = splash
        if (splash(rc)) return;

        // prioritas 3 = move ke tempat kosong
        moveToUnpaintedArea(rc);
    }

    // refill paint kalo < 25%
    static boolean needsRefill(RobotController rc) {
        return rc.getPaint() < rc.getType().paintCapacity * 0.25;
    }

    // refil dgn prioritas paint tower
    static void refillPaint(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        
        RobotInfo bestPaintTower = null;
        RobotInfo bestAnyTower = null;
        int bestPaintDist = Integer.MAX_VALUE;
        int bestAnyDist = Integer.MAX_VALUE;

        for (RobotInfo ally : allies) {
            if (!ally.getType().isTowerType()) continue;
            if (ally.getPaintAmount() < 10) continue;

            int dist = rc.getLocation().distanceSquaredTo(ally.getLocation());

            // Prioritas: PaintTower
            if (isPaintTower(ally.getType()) && dist < bestPaintDist) {
                bestPaintDist = dist;
                bestPaintTower = ally;
            }

            if (dist < bestAnyDist) {
                bestAnyDist = dist;
                bestAnyTower = ally;
            }
        }

        // kalo bisa ke PaintTower dulu, MoneyTower if PaintTower empty
        RobotInfo nearestTower = bestPaintTower != null ? bestPaintTower : bestAnyTower;
        if (bestPaintTower != null) knownTowerLoc = bestPaintTower.getLocation();
        else if (knownTowerLoc == null && bestAnyTower != null) knownTowerLoc = bestAnyTower.getLocation();

        if (nearestTower != null) {
            MapLocation towerLoc = nearestTower.getLocation();
            if (rc.getLocation().distanceSquaredTo(towerLoc) <= 2) {
                if (rc.isActionReady()) {
                    float localCoverage = estimateCoverage(rc);
                    int targetPaint = localCoverage > 0.8f
                        ? (int)(rc.getType().paintCapacity * 0.5f)
                        : rc.getType().paintCapacity;
                    int amountToRefill = Math.max(0, targetPaint - rc.getPaint());
                    int towerBuffer = 50;
                    int available = Math.max(0, nearestTower.getPaintAmount() - towerBuffer);
                    int actualRefill = Math.min(amountToRefill, available);
                    if (actualRefill > 0 && rc.canTransferPaint(towerLoc, -actualRefill)) {
                        rc.transferPaint(towerLoc, -actualRefill);
                    }
                    currentTarget = null;
                    return;
                }
            }
            moveToward(rc, towerLoc);
        } else if (knownTowerLoc != null) {
            if (rc.getLocation().distanceSquaredTo(knownTowerLoc) <= 2) {
                RobotInfo towerCheck = rc.senseRobotAtLocation(knownTowerLoc);
                if (towerCheck == null || !towerCheck.getType().isTowerType()) {
                    knownTowerLoc = null;
                    exploreByID(rc);
                    return;
                }
            }
            moveToward(rc, knownTowerLoc);
        } else {
            exploreByID(rc);
        }
    }

    static boolean isPaintTower(UnitType type) {
        return type == UnitType.LEVEL_ONE_PAINT_TOWER
            || type == UnitType.LEVEL_TWO_PAINT_TOWER
            || type == UnitType.LEVEL_THREE_PAINT_TOWER;
    }

    static boolean splash(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation bestTarget = null;
        int bestScore = 0;
        int mapArea = rc.getMapWidth() * rc.getMapHeight();

        for (MapInfo tile : nearbyTiles) {
            MapLocation tileLoc = tile.getMapLocation();
            if (rc.getLocation().distanceSquaredTo(tileLoc) > 4) continue;
            if (!rc.canAttack(tileLoc)) continue;
            int score = countSplashValue(rc, tileLoc, nearbyTiles);
            if (score > bestScore) { bestScore = score; bestTarget = tileLoc; }
        }

        // Large maps config
        int minScore = mapArea >= 1600 ? 8 : 4;
        if (bestTarget != null && bestScore >= minScore) {
            rc.attack(bestTarget);
            return true;
        }
        return false;
    }

    //cek worth nge splash 
    // prioritas paint musuh dibanding empty
    static int countSplashValue(RobotController rc, MapLocation center, MapInfo[] nearbyTiles) {
        int score = 0;
        for (MapInfo tile : nearbyTiles) {
            if (center.distanceSquaredTo(tile.getMapLocation()) > 2) continue;
            PaintType paint = tile.getPaint();
            if (paint == PaintType.EMPTY) score += 2;
            else if (paint.isEnemy()) score += 3;
        }
        return score;
    }

    static void moveToUnpaintedArea(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;

        MapLocation myLoc = rc.getLocation();

        if (currentTarget == null || myLoc.distanceSquaredTo(currentTarget) <= 2) {
            currentTarget = findBestTarget(rc, lastFailedTarget);
            lastFailedTarget = null;
        }

        if (currentTarget != null) {
            if (rc.canSenseLocation(currentTarget)) {
                MapInfo info = rc.senseMapInfo(currentTarget);
                if (info.getPaint().isAlly() && myLoc.distanceSquaredTo(currentTarget) <= 2) {
                    currentTarget = null;
                    exploreByID(rc);
                    return;
                }
            }
            Direction dir = myLoc.directionTo(currentTarget);
            if (rc.canMove(dir)) { rc.move(dir); return; }
            if (rc.canMove(dir.rotateLeft())) { rc.move(dir.rotateLeft()); return; }
            if (rc.canMove(dir.rotateRight())) { rc.move(dir.rotateRight()); return; }

            lastFailedTarget = currentTarget;
            currentTarget = null;
        }

        exploreByID(rc);
    }


    static MapLocation findBestTarget(RobotController rc, MapLocation exclude) throws GameActionException {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;
        int bestPriority = 0;

        Direction exploreDir = exploreTarget != null ? rc.getLocation().directionTo(exploreTarget) : null;

        for (MapInfo tile : nearbyTiles) {
            if (!tile.isPassable()) continue;

            MapLocation tileLoc = tile.getMapLocation();
            if (exclude != null && tileLoc.equals(exclude)) continue;

            PaintType paint = tile.getPaint();
            int priority = paint.isEnemy() ? 2 : paint == PaintType.EMPTY ? 1 : 0;
            if (priority == 0) continue;

            int dist = rc.getLocation().distanceSquaredTo(tileLoc);
            if (exploreDir != null) {
                Direction tileDir = rc.getLocation().directionTo(tileLoc);
                if (tileDir == exploreDir || tileDir == exploreDir.rotateLeft()
                        || tileDir == exploreDir.rotateRight()) {
                    dist = dist / 2;
                }
            }
            if (priority > bestPriority || (priority == bestPriority && dist < bestDist)) {
                bestDist = dist; best = tileLoc; bestPriority = priority;
            }
        }
        return best;
    }

    static float estimateCoverage(RobotController rc) throws GameActionException {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        int painted = 0;
        int total = 0;
        
        for (MapInfo tile : nearbyTiles) {
            if (!tile.isPassable()) continue;
            if (tile.hasRuin()) continue;
            total++;
            if (tile.getPaint() != PaintType.EMPTY) painted++;
        }
        
        if (total == 0) return 0f;
        return (float) painted / total;
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

        int arrivalThreshold = rc.getMapWidth() * rc.getMapHeight() >= 1600 ? 9 : 2;
        if (exploreTarget == null || myLoc.distanceSquaredTo(exploreTarget) <= arrivalThreshold) {
            if (exploreTarget != null) exploreZoneIdx = (exploreZoneIdx + 1) % 9;
            exploreTarget = newExploreTarget(rc, myLoc, mapWidth, mapHeight);
            rc.setIndicatorString("Target: " + exploreTarget + " ID:" + rc.getID());
        }

        moveToward(rc, exploreTarget);
        if (rc.isMovementReady()) {
            for (Direction d : directions) {
                if (rc.canMove(d)) { rc.move(d); lastMoveDir = d; return; }
            }
        }
    }

    static MapLocation newExploreTarget(RobotController rc, MapLocation myLoc, int mapWidth, int mapHeight) {
        int zoneW = mapWidth / 3;
        int zoneH = mapHeight / 3;
        int[] zoneOrder = buildZoneOrder(myLoc, mapWidth, mapHeight, rc.getID());

        for (int i = 0; i < 9; i++) {
            int zone = zoneOrder[(exploreZoneIdx + i) % 9];
            int cx = Math.min(zoneW * (zone % 3) + zoneW / 2, mapWidth - 1);
            int cy = Math.min(zoneH * (zone / 3) + zoneH / 2, mapHeight - 1);
            MapLocation zoneLoc = new MapLocation(cx, cy);

            if (myLoc.distanceSquaredTo(zoneLoc) <= 9) continue;

            return zoneLoc;
        }

        return new MapLocation(mapWidth / 2, mapHeight / 2);
    }

    // prediksi dari lokasi spawn agar prioritas jalan ke lokasi musuh
    static int[] buildZoneOrder(MapLocation spawnLoc, int mapWidth, int mapHeight, int robotID) {
        if (cachedZoneOrder != null) return cachedZoneOrder;
        int zoneW = mapWidth / 3;
        int zoneH = mapHeight / 3;
        int[] order = new int[9];
        int[] distSq = new int[9];
        for (int i = 0; i < 9; i++) {
            order[i] = i;
            int cx = Math.min(zoneW * (i % 3) + zoneW / 2, mapWidth - 1);
            int cy = Math.min(zoneH * (i / 3) + zoneH / 2, mapHeight - 1);
            distSq[i] = spawnLoc.distanceSquaredTo(new MapLocation(cx, cy));
        }

        for (int i = 1; i < 9; i++) {
            int kz = order[i], kd = distSq[i];
            int j = i - 1;
            while (j >= 0 && distSq[j] < kd) {
                order[j + 1] = order[j];
                distSq[j + 1] = distSq[j];
                j--;
            }
            order[j + 1] = kz;
            distSq[j + 1] = kd;
        }
        cachedZoneOrder = order;
        exploreZoneIdx = robotID % 9;
        return order;
    }
}