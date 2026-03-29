package main_bot;

import battlecode.common.*;

public class RobotPlayer {

    static int turnCount = 0;
    public static void run(RobotController rc) throws GameActionException {
        // System.out.println("I'm alive! Type: " + rc.getType());

        while (true) {
            turnCount++;
            try {
                switch (rc.getType()) {
                    // unit
                    case SOLDIER:
                        main_bot.Units.Soldier.run(rc);
                        break;
                    case MOPPER:
                        main_bot.Units.Mopper.run(rc);
                        break;
                    case SPLASHER:
                        main_bot.Units.Splasher.run(rc);
                        break;

                    // towers
                    case LEVEL_ONE_PAINT_TOWER:
                    case LEVEL_TWO_PAINT_TOWER:
                    case LEVEL_THREE_PAINT_TOWER:
                        main_bot.Towers.PaintTower.run(rc);
                        break;

                    case LEVEL_ONE_MONEY_TOWER:
                    case LEVEL_TWO_MONEY_TOWER:
                    case LEVEL_THREE_MONEY_TOWER:
                        main_bot.Towers.MoneyTower.run(rc);
                        break;

                    default:
                        break;
                }
            } 
            
            catch (GameActionException e) {
                // debug
                // System.out.println("GameActionException at turn " + turnCount + ": " + e.getMessage());
                // e.printStackTrace();
            } catch (Exception e) {
                //debug
                // System.out.println("Exception at turn " + turnCount + ": " + e.getMessage());
                // e.printStackTrace();
            } 
            
            finally {
                Clock.yield();
            }
        }
    }
}