package com.spaceinvaders;

import com.spaceinvaders.game.GameFrame;
import javax.swing.SwingUtilities;

/**
 * The main to run the space invaders game
 */
public class Main {
    public static void main(String[] args) {
        // Run on Swing's Event Dispatch Thread (this required for GUI)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GameFrame();
            }
        });
    }
}

// TODO: Hacer un shitty invaders (q la bala vaya pa bajo y salga por el otro lao)