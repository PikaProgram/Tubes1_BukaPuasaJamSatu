package laliro;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import java.util.Random;

// Shared state for all units
class BotState {
  static int turnCount = 0;
  static final Random rng = new Random();

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

  static MapLocation exploreTarget = null;
}
