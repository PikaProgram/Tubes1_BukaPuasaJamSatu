package alternative_bots_1;

import battlecode.common.*;

class LeSoldier {
  static void run(RobotController rc) throws GameActionException {
    MapLocation myLoc = rc.getLocation();

    // 1st priority: if low on paint, return to nearest ally tower to refill
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
        Utils.paintSelfTile(rc);
        return;
      }
    }

    // 2nd priority: if ruin nearby, approach, mark, paint, complete tower
    MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
    MapLocation bestRuin = null;
    int bestDist = Integer.MAX_VALUE;

    for (MapLocation ruin : nearbyRuins) {
      RobotInfo robotAtRuin = rc.senseRobotAtLocation(ruin);
      if (robotAtRuin != null && robotAtRuin.getType().isTowerType())
        continue;

      int dist = myLoc.distanceSquaredTo(ruin);
      if (dist < bestDist) {
        bestDist = dist;
        bestRuin = ruin;
      }
    }

    if (bestRuin != null) {
      buildRuin(rc, bestRuin);
      return;
    }

    // 3rd priority: explore and paint tiles
    MapInfo[] nearbyInfos = rc.senseNearbyMapInfos();
    for (MapInfo info : nearbyInfos) {
      MapLocation loc = info.getMapLocation();
      if (rc.canCompleteResourcePattern(loc)) {
        rc.completeResourcePattern(loc);
        return;
      }
    }

    if (rc.canMarkResourcePattern(myLoc)) {
      rc.markResourcePattern(myLoc);
    }

    // Move toward unpainted or enemy-painted tiles
    MapInfo[] nearby = rc.senseNearbyMapInfos();
    MapLocation bestPaintTarget = null;
    int bestScore = Integer.MIN_VALUE;

    for (MapInfo tile : nearby) {
      if (!tile.isPassable())
        continue;
      MapLocation tileLoc = tile.getMapLocation();
      PaintType paint = tile.getPaint();

      int score = 0;
      if (paint == PaintType.EMPTY)
        score = 10;
      else if (paint.isEnemy())
        score = 15;
      else
        continue;

      score -= myLoc.distanceSquaredTo(tileLoc);

      if (score > bestScore) {
        bestScore = score;
        bestPaintTarget = tileLoc;
      }
    }

    if (bestPaintTarget != null) {
      Utils.nav(rc, bestPaintTarget);
    } else {
      Utils.explore(rc);
    }

    Utils.paintSelfTile(rc);

    myLoc = rc.getLocation();
    if (rc.isActionReady()) {
      MapInfo[] tilesAfterMove = rc.senseNearbyMapInfos(myLoc, rc.getType().actionRadiusSquared);
      for (MapInfo tile : tilesAfterMove) {
        if (!tile.isPassable())
          continue;
        PaintType paint = tile.getPaint();
        if (!paint.isAlly() && rc.canAttack(tile.getMapLocation())) {
          rc.attack(tile.getMapLocation());
          break;
        }
      }
    }
  }

  // Build ruins into towers, prioritizing paint, then money tower
  static void buildRuin(RobotController rc, MapLocation ruinLoc) throws GameActionException {
    RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
    int paintTowers = 0, moneyTowers = 0;
    for (RobotInfo ally : allies) {
      UnitType type = ally.getType();
      if (type == UnitType.LEVEL_ONE_PAINT_TOWER || type == UnitType.LEVEL_TWO_PAINT_TOWER
          || type == UnitType.LEVEL_THREE_PAINT_TOWER) {
        paintTowers++;
      } else if (type == UnitType.LEVEL_ONE_MONEY_TOWER || type == UnitType.LEVEL_TWO_MONEY_TOWER
          || type == UnitType.LEVEL_THREE_MONEY_TOWER) {
        moneyTowers++;
      }
    }

    // Prioritize Paint, then Money, dgaf abt Defense lul.
    UnitType towerType = null;

    if (paintTowers <= moneyTowers) {
      towerType = UnitType.LEVEL_ONE_PAINT_TOWER;
    } else if (moneyTowers < paintTowers) {
      towerType = UnitType.LEVEL_ONE_MONEY_TOWER;
    }

    if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
      rc.completeTowerPattern(towerType, ruinLoc);
      return;
    }

    if (rc.canMarkTowerPattern(towerType, ruinLoc)) {
      rc.markTowerPattern(towerType, ruinLoc);
    }

    // try to paint available tiles, approach ruin if none in range
    boolean paintedSomething = false;
    MapInfo[] patternTiles = rc.senseNearbyMapInfos(ruinLoc, 8);
    for (MapInfo tile : patternTiles) {
      PaintType mark = tile.getMark();
      PaintType paint = tile.getPaint();
      if (mark != PaintType.EMPTY && mark != paint) {
        boolean useSecondary = (mark == PaintType.ALLY_SECONDARY);
        if (rc.canAttack(tile.getMapLocation())) {
          rc.attack(tile.getMapLocation(), useSecondary);
          paintedSomething = true;
          break;
        }
      }
    }

    if (!paintedSomething) {
      Utils.nav(rc, ruinLoc);
      patternTiles = rc.senseNearbyMapInfos(ruinLoc, 8);
      for (MapInfo tile : patternTiles) {
        PaintType mark = tile.getMark();
        PaintType paint = tile.getPaint();
        if (mark != PaintType.EMPTY && mark != paint) {
          boolean useSecondary = (mark == PaintType.ALLY_SECONDARY);
          if (rc.canAttack(tile.getMapLocation())) {
            rc.attack(tile.getMapLocation(), useSecondary);
            break;
          }
        }
      }
    }

    if (rc.canCompleteTowerPattern(towerType, ruinLoc)) {
      rc.completeTowerPattern(towerType, ruinLoc);
    }
  }
}
