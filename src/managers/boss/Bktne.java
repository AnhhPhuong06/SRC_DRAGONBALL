/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package managers.boss;

import managers.boss.BossManager;

/**
 *
 * @author Administrator
 */
public class Bktne extends BossManager {

    private static Bktne instance;

    public static Bktne gI() {
        if (instance == null) {
            instance = new Bktne();
        }
        return instance;
    }

}
