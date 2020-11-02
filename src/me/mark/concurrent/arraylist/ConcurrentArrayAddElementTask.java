package me.mark.concurrent.arraylist;

import java.util.concurrent.ThreadLocalRandom;

public final class ConcurrentArrayAddElementTask implements Runnable {

    private final ConcurrentArrayList<Integer> list;

    public ConcurrentArrayAddElementTask(ConcurrentArrayList<Integer> list) {
        this.list = list;
    }

    @Override
    public void run() {
//        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        list.add(list.size());
    }

}
