package laliro;

import battlecode.common.*;

class Utils {
  // refill paint when < 30% capacity
  static boolean shouldRefill(RobotController rc) {
    return rc.getPaint() < rc.getType().paintCapacity * 0.3;
  }

  // find nearest ally tower location, null if none found
  static MapLocation findNearestAllyTower(RobotController rc) throws GameActionException {
    RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
    MapLocation myLoc = rc.getLocation();
    MapLocation nearest = null;
    int bestDist = Integer.MAX_VALUE;

    for (RobotInfo ally : allies) {
      if (ally.getType().isTowerType()) {
        int dist = myLoc.distanceSquaredTo(ally.getLocation());
        if (dist < bestDist) {
          bestDist = dist;
          nearest = ally.getLocation();
        }
      }
    }
    return nearest;
  }

  // simple bug navigation like nav logic
  static void nav(RobotController rc, MapLocation target) throws GameActionException {
    if (!rc.isMovementReady())
      return;

    MapLocation myLoc = rc.getLocation();
    if (myLoc.equals(target))
      return;

    Direction toTarget = myLoc.directionTo(target);

    if (rc.canMove(toTarget)) {
      rc.move(toTarget);
      return;
    }

    Direction left = toTarget.rotateLeft();
    Direction right = toTarget.rotateRight();

    if (rc.canMove(left)) {
      rc.move(left);
      return;
    }
    if (rc.canMove(right)) {
      rc.move(right);
      return;
    }

    Direction left2 = left.rotateLeft();
    Direction right2 = right.rotateRight();

    if (rc.canMove(left2)) {
      rc.move(left2);
      return;
    }
    if (rc.canMove(right2)) {
      rc.move(right2);
    }
  }

  // random explore
  static void explore(RobotController rc) throws GameActionException {
    MapLocation myLoc = rc.getLocation();

    if (BotState.exploreTarget == null || myLoc.distanceSquaredTo(BotState.exploreTarget) < 5
        || BotState.turnCount % 50 == 0) {
      int mapW = rc.getMapWidth();
      int mapH = rc.getMapHeight();
      BotState.exploreTarget = new MapLocation(BotState.rng.nextInt(mapW), BotState.rng.nextInt(mapH));
    }

    nav(rc, BotState.exploreTarget);
  }

  static void paintSelfTile(RobotController rc) throws GameActionException {
    MapLocation myLoc = rc.getLocation();
    if (rc.canSenseLocation(myLoc)) {
      MapInfo tile = rc.senseMapInfo(myLoc);
      if (!tile.getPaint().isAlly() && rc.canAttack(myLoc)) {
        rc.attack(myLoc);
      }
    }
  }

  // get directions in order of preference for movement
  static Direction[] getOrderedDirections(Direction center) {
    return new Direction[] {
        center,
        center.rotateLeft(),
        center.rotateRight(),
        center.rotateLeft().rotateLeft(),
        center.rotateRight().rotateRight(),
        center.rotateLeft().rotateLeft().rotateLeft(),
        center.rotateRight().rotateRight().rotateRight(),
        center.opposite(),
    };
  }
}
