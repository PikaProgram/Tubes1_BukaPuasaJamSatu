package alternative_bots_1;

import battlecode.common.*;

public class RobotPlayer {
  public static void run(RobotController rc) throws GameActionException {
    BotState.rng.setSeed(rc.getID());

    while (true) {
      BotState.turnCount += 1;
      try {
        switch (rc.getType()) {
          case SOLDIER:
            LeSoldier.run(rc);
            break;
          case MOPPER:
            LeMopper.run(rc);
            break;
          case SPLASHER:
            LeSplasher.run(rc);
            break;
          default: // Tower
            LeTower.run(rc);
            break;
        }
      } catch (GameActionException e) {
        System.out.println("GameActionException");
        e.printStackTrace();
      } catch (Exception e) {
        System.out.println("Exception");
        e.printStackTrace();
      } finally {
        Clock.yield();
      }
    }
  }
}
