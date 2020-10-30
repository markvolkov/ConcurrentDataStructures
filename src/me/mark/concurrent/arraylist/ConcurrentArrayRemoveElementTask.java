package me.mark.concurrent.arraylist;

import java.util.concurrent.ThreadLocalRandom;

public class ConcurrentArrayRemoveElementTask implements Runnable {

    private final ConcurrentArrayList<Integer> list;

    public ConcurrentArrayRemoveElementTask(ConcurrentArrayList<Integer> list) {
        this.list = list;
    }

    @Override
    public void run() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        list.remove(threadLocalRandom.nextInt(list.size()));
    }

}
