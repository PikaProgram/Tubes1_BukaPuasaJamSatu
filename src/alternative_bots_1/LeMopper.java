package alternative_bots_1;

import battlecode.common.*;

class LeMopper {
  static void run(RobotController rc) throws GameActionException {
    MapLocation myLoc = rc.getLocation();

    if (rc.getPaint() < 15) {
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

    if (rc.isActionReady()) {
      paintToAlly(rc);
    }

    if (rc.isActionReady()) {
      mopSwing(rc);
    }

    if (rc.isActionReady()) {
      mopEnemyPaint(rc);
    }

    // Move towards nearest enemy paint if no action taken, explore otherwise
    if (!rc.isMovementReady())
      return;

    MapInfo[] nearby = rc.senseNearbyMapInfos();

    MapLocation nearestEnemy = null;
    int bestDist = Integer.MAX_VALUE;
    for (MapInfo tile : nearby) {
      if (tile.getPaint().isEnemy()) {
        int dist = myLoc.distanceSquaredTo(tile.getMapLocation());
        if (dist < bestDist) {
          bestDist = dist;
          nearestEnemy = tile.getMapLocation();
        }
      }
    }

    if (nearestEnemy != null) {
      Utils.nav(rc, nearestEnemy);
    } else {
      RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
      MapLocation nearestCombatAlly = null;
      int bestAllyDist = Integer.MAX_VALUE;
      for (RobotInfo ally : allies) {
        if (ally.getType() == UnitType.SOLDIER || ally.getType() == UnitType.SPLASHER) {
          int dist = myLoc.distanceSquaredTo(ally.getLocation());
          if (dist < bestAllyDist) {
            bestAllyDist = dist;
            nearestCombatAlly = ally.getLocation();
          }
        }
      }

      if (nearestCombatAlly != null && bestAllyDist > 8) {
        Utils.nav(rc, nearestCombatAlly);
      } else {
        Utils.explore(rc);
      }
    }

    if (rc.isActionReady()) {
      paintToAlly(rc);
    }
    if (rc.isActionReady()) {
      mopSwing(rc);
    }
    if (rc.isActionReady()) {
      mopEnemyPaint(rc);
    }
  }

  // Transfer paint to nearby ally with lowest paint percentage under 50%
  static void paintToAlly(RobotController rc) throws GameActionException {
    RobotInfo[] allies = rc.senseNearbyRobots(2, rc.getTeam());
    RobotInfo neediest = null;
    double lowestPct = 1.0;

    for (RobotInfo ally : allies) {
      if (ally.getType().isTowerType())
        continue;
      double pct = (double) ally.getPaintAmount() / ally.getType().paintCapacity;
      if (pct < lowestPct && pct < 0.5) {
        lowestPct = pct;
        neediest = ally;
      }
    }

    if (neediest != null) {
      int transferAmount = Math.min(rc.getPaint() / 2,
          neediest.getType().paintCapacity - neediest.getPaintAmount());
      if (transferAmount > 0 && rc.canTransferPaint(neediest.getLocation(), transferAmount)) {
        rc.transferPaint(neediest.getLocation(), transferAmount);
      }
    }
  }

  // Attempt to mop swing an adjacent enemy robot, if any
  static void mopSwing(RobotController rc) throws GameActionException {
    Direction[] swingDirs = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
    Direction bestDir = null;
    for (Direction dir : swingDirs) {
      if (rc.canMopSwing(dir)) {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies) {
          if (enemy.getLocation().equals(rc.getLocation().add(dir))) {
            bestDir = dir;
            break;
          }
        }
      }
    }
    if (bestDir != null) {
      rc.mopSwing(bestDir);
    }
  }

  // Mop any available enemy paint nearby
  static void mopEnemyPaint(RobotController rc) throws GameActionException {
    MapInfo[] nearby = rc.senseNearbyMapInfos(rc.getLocation(), 2);
    for (MapInfo tile : nearby) {
      if (tile.getPaint().isEnemy() && rc.canAttack(tile.getMapLocation())) {
        rc.attack(tile.getMapLocation());
        return;
      }
    }
  }
}
