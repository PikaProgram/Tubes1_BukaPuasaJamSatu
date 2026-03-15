package laliro;

import battlecode.common.*;

class LeTower {
  static void run(RobotController rc) throws GameActionException {
    // Attack the lowest HP enemy in range, if any
    if (!rc.isActionReady())
      return;

    RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
    if (enemies.length == 0)
      return;

    RobotInfo bestTarget = null;
    int lowestHP = Integer.MAX_VALUE;
    for (RobotInfo enemy : enemies) {
      if (enemy.getHealth() < lowestHP && rc.canAttack(enemy.getLocation())) {
        lowestHP = enemy.getHealth();
        bestTarget = enemy;
      }
    }
    if (bestTarget != null) {
      rc.attack(bestTarget.getLocation());
    }

    /**
     * Spawn units based on rounds, split into 3 phases:
     * 1. Early (0-200): 65% soldier, 25% mopper, 10% splasher
     * 2. Mid (200-800): 35% soldier, 35% splasher, 30% mopper
     * 3. Late (800+): 20% soldier, 50% splasher, 30% mopper
     */
    int round = rc.getRoundNum();
    UnitType unitToSpawn = null;

    int roll = BotState.rng.nextInt(100);

    if (round < 200) {
      if (roll < 65)
        unitToSpawn = UnitType.SOLDIER;
      else if (roll < 90)
        unitToSpawn = UnitType.MOPPER;
      else
        unitToSpawn = UnitType.SPLASHER;
    } else if (round < 800) {
      if (roll < 35)
        unitToSpawn = UnitType.SOLDIER;
      else if (roll < 70)
        unitToSpawn = UnitType.SPLASHER;
      else
        unitToSpawn = UnitType.MOPPER;
    } else {
      if (roll < 20)
        unitToSpawn = UnitType.SOLDIER;
      else if (roll < 70)
        unitToSpawn = UnitType.SPLASHER;
      else
        unitToSpawn = UnitType.MOPPER;
    }

    // Try to spawn units in direction toward map center
    MapLocation towerLoc = rc.getLocation();
    MapLocation mapCenter = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
    Direction toCenter = towerLoc.directionTo(mapCenter);

    Direction[] orderedDirs = Utils.getOrderedDirections(toCenter);

    for (Direction dir : orderedDirs) {
      MapLocation spawnLoc = towerLoc.add(dir);
      if (rc.canBuildRobot(unitToSpawn, spawnLoc)) {
        rc.buildRobot(unitToSpawn, spawnLoc);
        return;
      }
    }

    // If preferred unit type can't be spawned, try other types based on i made it up
    // No heuristics here kekw.
    UnitType[] fallbacks = { UnitType.SOLDIER, UnitType.MOPPER, UnitType.SPLASHER };
    for (UnitType fallback : fallbacks) {
      if (fallback == unitToSpawn)
        continue;
      for (Direction dir : orderedDirs) {
        MapLocation spawnLoc = towerLoc.add(dir);
        if (rc.canBuildRobot(fallback, spawnLoc)) {
          rc.buildRobot(fallback, spawnLoc);
          return;
        }
      }
    }
  }
}
