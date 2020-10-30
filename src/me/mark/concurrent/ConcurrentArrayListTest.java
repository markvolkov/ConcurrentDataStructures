package me.mark.concurrent;

import me.mark.concurrent.arraylist.ConcurrentArrayList;
import me.mark.concurrent.arraylist.ConcurrentArrayTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ConcurrentArrayListTest {

  private static final int TASK_COUNT = 25;

  public static void main(String[] args) throws InterruptedException {
    ConcurrentArrayList<Integer> list = new ConcurrentArrayList<>();
    List<Thread> arrayTasks = new ArrayList<>(TASK_COUNT);
    System.out.println("Generating " + TASK_COUNT + " tasks...");
    for (int i = 0; i < TASK_COUNT; i++) {
      Thread listThread = new Thread(new ConcurrentArrayTask(list), "Thread-" + i);
      arrayTasks.add(listThread);
      listThread.start();
    }
    for (Thread task : arrayTasks) {
      task.join();
      System.out.println(task.getName() + " has finished!");
    }
    System.out.println(list);
  }

}
