package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import java.util.Random;

public class ItTools {

    public static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return new Random().nextInt((max - min) + 1) + min;
    }

}
