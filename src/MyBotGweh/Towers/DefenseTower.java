package MyBotGweh.Towers;

import battlecode.common.*;

public class DefenseTower {

    public static void run(RobotController rc) throws GameActionException {
        // Defense tower: fokus serang musuh, gajadi dipake
        // fokus paint + destroy
        MoneyTower.attackEnemy(rc);
        MoneyTower.spawnUnit(rc);
    }
}