package laliro;

import battlecode.common.*;

class LeSplasher {
  // splasher try to splash available targets nearby, move to find better splash
  // locations if no good target, explore if no good splash locations
  static void run(RobotController rc) throws GameActionException {
    MapLocation myLoc = rc.getLocation();

    if (Utils.shouldRefill(rc)) {
      MapLocation tower = Utils.findNearestAllyTower(rc);
      if (tower != null) {
        int needed = rc.getType().paintCapacity - rc.getPaint();
        if (myLoc.isWithinDistanceSquared(tower, 2)
            && rc.canTransferPaint(tower, -needed)) {
          rc.transferPaint(tower, -needed);
        } else {
          Utils.nav(rc, tower);
        }
        return;
      }
    }

    if (rc.isActionReady() && rc.getPaint() >= 50) {
      MapLocation bestTarget = searchSplashTarget(rc);
      if (bestTarget != null && rc.canAttack(bestTarget)) {
        rc.attack(bestTarget);
      }
    }

    if (!rc.isMovementReady())
      return;

    Direction bestDir = null;
    int bestUnpainted = -1;

    for (Direction dir : BotState.directions) {
      if (!rc.canMove(dir))
        continue;
      MapLocation newLoc = myLoc.add(dir);

      int unpainted = 0;
      MapInfo[] tilesAhead = rc.senseNearbyMapInfos(newLoc, 8);
      for (MapInfo tile : tilesAhead) {
        if (!tile.isPassable())
          continue;
        PaintType paint = tile.getPaint();
        if (paint == PaintType.EMPTY || paint.isEnemy())
          unpainted++;
      }

      if (unpainted > bestUnpainted) {
        bestUnpainted = unpainted;
        bestDir = dir;
      }
    }

    if (bestDir != null) {
      rc.move(bestDir);
    } else {
      Utils.explore(rc);
    }

    if (rc.isActionReady() && rc.getPaint() >= 50) {
      MapLocation bestTarget = searchSplashTarget(rc);
      if (bestTarget != null && rc.canAttack(bestTarget)) {
        rc.attack(bestTarget);
      }
    }
  }

  // find best splash attack location within range, null if none
  static MapLocation searchSplashTarget(RobotController rc) throws GameActionException {
    MapLocation myLoc = rc.getLocation();
    int actionRadius = rc.getType().actionRadiusSquared;

    MapLocation bestTarget = null;
    int bestNewTiles = 0;

    MapLocation[] attackableLocs = rc.getAllLocationsWithinRadiusSquared(myLoc, actionRadius);
    for (MapLocation attackLoc : attackableLocs) {
      if (!rc.canAttack(attackLoc))
        continue;

      int newTiles = 0;
      MapLocation[] splashArea = rc.getAllLocationsWithinRadiusSquared(attackLoc, 4);
      for (MapLocation tileLoc : splashArea) {
        if (!rc.canSenseLocation(tileLoc))
          continue;
        MapInfo tile = rc.senseMapInfo(tileLoc);
        if (!tile.isPassable())
          continue;

        PaintType paint = tile.getPaint();
        int distToCenter = attackLoc.distanceSquaredTo(tileLoc);

        if (distToCenter <= 2) {
          if (paint == PaintType.EMPTY || paint.isEnemy())
            newTiles++;
        } else {
          if (paint == PaintType.EMPTY)
            newTiles++;
        }
      }

      if (newTiles > bestNewTiles) {
        bestNewTiles = newTiles;
        bestTarget = attackLoc;
      }
    }

    return bestTarget;
  }
}
