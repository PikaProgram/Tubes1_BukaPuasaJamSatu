package MyBotGweh.Units;

import battlecode.common.*;

public class Soldier {

    static final Direction[] directions = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,
    };

    static Direction lastMoveDir = null;
    static MapLocation lastLoc = null;
    static int stuckTurns = 0;

    public static void run(RobotController rc) throws GameActionException {
        MapLocation now = rc.getLocation();
        if (lastLoc != null && now.equals(lastLoc)) stuckTurns = Math.min(stuckTurns + 1, 5);
        else stuckTurns = 0;
        lastLoc = now;

        if (needsRefill(rc)) {
            refillPaint(rc);
            paintCurrentTile(rc);
            return;
        }
        if (attackEnemyTower(rc)) return;

        moveToNearestUnpainted(rc);
        paintCurrentTile(rc);
        // DEBUG
        // if (rc.getRoundNum() % 100 == 0) {
        //     System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
        // }
    }

    static boolean attackEnemyTower(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().isTowerType()) {
                MapLocation towerLoc = enemy.getLocation();
                if (rc.canAttack(towerLoc)) { rc.attack(towerLoc); return true; }
                if (rc.isMovementReady()) moveToward(rc, towerLoc);
                return true;
            }
        }
        return false;
    }

    static boolean needsRefill(RobotController rc) {
        int mapArea = rc.getMapWidth() * rc.getMapHeight();
        // Large maps rarely hit 80% coverage, so use higher threshold (paint often).
        // Small/Medium hit 80% coverage within ~50 rounds, so use low threshold (stay on field).
        float threshold = mapArea >= 1600 ? 0.42f : 0.25f;
        return rc.getPaint() < rc.getType().paintCapacity * threshold;
    }

    static MapLocation knownTowerLoc = null;

    static void refillPaint(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo bestPaintTower = null, bestAnyTower = null;
        int bestPaintDist = Integer.MAX_VALUE, bestAnyDist = Integer.MAX_VALUE;

        for (RobotInfo ally : allies) {
            if (!ally.getType().isTowerType()) continue;
            if (ally.getPaintAmount() < 10) continue;
            int dist = rc.getLocation().distanceSquaredTo(ally.getLocation());
            if (isPaintTower(ally.getType()) && dist < bestPaintDist) {
                bestPaintDist = dist; bestPaintTower = ally;
            }
            if (dist < bestAnyDist) { bestAnyDist = dist; bestAnyTower = ally; }
        }

        RobotInfo nearestTower = bestPaintTower != null ? bestPaintTower : bestAnyTower;
        if (bestPaintTower != null) knownTowerLoc = bestPaintTower.getLocation();
        else if (bestAnyTower != null) knownTowerLoc = bestAnyTower.getLocation();

        if (nearestTower != null) {
            MapLocation towerLoc = nearestTower.getLocation();
            if (rc.getLocation().distanceSquaredTo(towerLoc) <= 2) {
                if (rc.isActionReady()) {
                    float localCoverage = estimateCoverage(rc);
                    int targetPaint = localCoverage > 0.8f
                        ? (int)(rc.getType().paintCapacity * 0.5f)
                        : rc.getType().paintCapacity;
                    int amountToRefill = Math.max(0, targetPaint - rc.getPaint());
                    int available = Math.max(0, nearestTower.getPaintAmount() - 50);
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

    static void paintCurrentTile(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        MapLocation loc = rc.getLocation();
        MapInfo tile = rc.senseMapInfo(loc);
        if (tile.getPaint() == PaintType.EMPTY || tile.getPaint().isEnemy()) {
            if (rc.canAttack(loc)) rc.attack(loc);
        }
    }

    static MapLocation currentTarget = null;
    static MapLocation lastFailedTarget = null;

    static void moveToNearestUnpainted(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;
        MapLocation myLoc = rc.getLocation();

        if (currentTarget == null || myLoc.distanceSquaredTo(currentTarget) <= 2) {
            currentTarget = findBestTarget(rc, lastFailedTarget);
            lastFailedTarget = null;
        }

        if (currentTarget != null) {
            if (rc.canSenseLocation(currentTarget)) {
                MapInfo targetInfo = rc.senseMapInfo(currentTarget);
                if (targetInfo.getPaint().isAlly() && myLoc.distanceSquaredTo(currentTarget) <= 2) {
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
        int mapArea = rc.getMapWidth() * rc.getMapHeight();
        boolean largeMap = mapArea >= 1600;

        if (largeMap) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            int bestScore = Integer.MIN_VALUE;
            for (MapInfo tile : nearbyTiles) {
                if (!tile.isPassable()) continue;
                MapLocation tileLoc = tile.getMapLocation();
                if (exclude != null && tileLoc.equals(exclude)) continue;
                PaintType paint = tile.getPaint();
                int priority = paint.isEnemy() ? 2 : paint == PaintType.EMPTY ? 1 : 0;
                if (priority == 0) continue;
                int dist = rc.getLocation().distanceSquaredTo(tileLoc);
                int score = priority * 100 - dist;
                Direction preferredDir = lastMoveDir;
                if (preferredDir != null) {
                    Direction tileDir = rc.getLocation().directionTo(tileLoc);
                    if (tileDir == preferredDir || tileDir == preferredDir.rotateLeft()
                        || tileDir == preferredDir.rotateRight()) {
                        score += 30;
                    }
                }
                score -= countAlliesNear(allies, tileLoc, 2) * 20;
                if (score > bestScore) { bestScore = score; best = tileLoc; }
            }
        } else {
            int bestDist = Integer.MAX_VALUE, bestPriority = 0;
            for (MapInfo tile : nearbyTiles) {
                if (!tile.isPassable()) continue;
                MapLocation tileLoc = tile.getMapLocation();
                if (exclude != null && tileLoc.equals(exclude)) continue;
                PaintType paint = tile.getPaint();
                int priority = paint.isEnemy() ? 2 : paint == PaintType.EMPTY ? 1 : 0;
                if (priority == 0) continue;
                int dist = rc.getLocation().distanceSquaredTo(tileLoc);
                if (priority > bestPriority || (priority == bestPriority && dist < bestDist)) {
                    bestDist = dist; best = tileLoc; bestPriority = priority;
                }
            }
        }
        return best;
    }

    static int countAlliesNear(RobotInfo[] allies, MapLocation loc, int radiusSq) {
        int count = 0;
        for (RobotInfo ally : allies) {
            if (ally.getLocation().distanceSquaredTo(loc) <= radiusSq) count++;
        }
        return count;
    }

    static MapLocation exploreTarget = null;
    static int explorePhase = 0;
    static int[] cachedExploreOrder = null;

    static void exploreByID(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;
        MapLocation myLoc = rc.getLocation();
        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();

        if (exploreTarget == null || myLoc.distanceSquaredTo(exploreTarget) <= 4) {
            exploreTarget = nextTarget(rc, myLoc, mapWidth, mapHeight);
        }

        moveToward(rc, exploreTarget);
        if (rc.isMovementReady()) {
            for (Direction d : directions) {
                if (rc.canMove(d)) { rc.move(d); lastMoveDir = d; exploreTarget = null; return; }
            }
        }
    }

    static MapLocation nextTarget(RobotController rc, MapLocation myLoc, int mapWidth, int mapHeight) {
        MapLocation[] targets = {
            new MapLocation(mapWidth / 4,     mapHeight / 4),
            new MapLocation(3 * mapWidth / 4, mapHeight / 4),
            new MapLocation(mapWidth / 4,     3 * mapHeight / 4),
            new MapLocation(3 * mapWidth / 4, 3 * mapHeight / 4),
            new MapLocation(mapWidth / 2,     mapHeight / 2),
            new MapLocation(mapWidth / 8,     mapHeight / 2),
            new MapLocation(7 * mapWidth / 8, mapHeight / 2),
            new MapLocation(mapWidth / 2,     mapHeight / 8),
            new MapLocation(mapWidth / 2,     7 * mapHeight / 8),
        };

        if (cachedExploreOrder == null) {
            cachedExploreOrder = new int[targets.length];
            int[] distSq = new int[targets.length];
            for (int i = 0; i < targets.length; i++) {
                cachedExploreOrder[i] = i;
                distSq[i] = myLoc.distanceSquaredTo(targets[i]);
            }
            // Insertion sort descending by distance
            for (int i = 1; i < targets.length; i++) {
                int ki = cachedExploreOrder[i], kd = distSq[i];
                int j = i - 1;
                while (j >= 0 && distSq[j] < kd) {
                    cachedExploreOrder[j + 1] = cachedExploreOrder[j];
                    distSq[j + 1] = distSq[j];
                    j--;
                }
                cachedExploreOrder[j + 1] = ki;
                distSq[j + 1] = kd;
            }
            explorePhase = rc.getID() % targets.length;
        }

        for (int i = 0; i < targets.length; i++) {
            MapLocation candidate = targets[cachedExploreOrder[(explorePhase + i) % targets.length]];
            if (myLoc.distanceSquaredTo(candidate) > 16) {
                explorePhase = (explorePhase + 1) % targets.length;
                return candidate;
            }
        }
        explorePhase = 0;
        return targets[cachedExploreOrder[0]];
    }
    static float estimateCoverage(RobotController rc) throws GameActionException {
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        int painted = 0, total = 0;
        for (MapInfo tile : nearbyTiles) {
            if (!tile.isPassable() || tile.hasRuin()) continue;
            total++;
            if (tile.getPaint() != PaintType.EMPTY) painted++;
        }
        if (total == 0) return 0f;
        return (float) painted / total;
    }
}