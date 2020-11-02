package me.mark.concurrent;

import me.mark.concurrent.arraylist.ConcurrentArrayList;
import me.mark.concurrent.arraylist.ConcurrentArrayAddElementTask;
import me.mark.concurrent.arraylist.ConcurrentArrayRemoveElementTask;
import org.omg.SendingContext.RunTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class ConcurrentArrayListTest {

  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  private static final int TASK_COUNT = 25;

  public static void main(String[] args) throws InterruptedException, ExecutionException {
    ConcurrentArrayList<Integer> list = new ConcurrentArrayList<>();
    System.out.println("Generating " + TASK_COUNT + " tasks...");
    for (int i = 0; i < TASK_COUNT; i++) {
      Future<?> future = EXECUTOR_SERVICE.submit(new ConcurrentArrayAddElementTask(list));
      future.get();
    }
    System.out.println(list);
    System.out.println("Removing all elements asynchronously...");
    for (int i = 0; i < TASK_COUNT; i++) {
      Future<?> future = EXECUTOR_SERVICE.submit(new ConcurrentArrayRemoveElementTask(list));
      future.get();
    }
    System.out.println(list);
    EXECUTOR_SERVICE.shutdown();
    try {
      EXECUTOR_SERVICE.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
