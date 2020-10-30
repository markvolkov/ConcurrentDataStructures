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
    List<Thread> arrayTasks = new ArrayList<>(TASK_COUNT);
    System.out.println("Generating " + TASK_COUNT + " tasks...");
    for (int i = 0; i < TASK_COUNT; i++) {
      Thread listThread = new Thread(new ConcurrentArrayAddElementTask(list), "Thread-AddTask-" + i);
      arrayTasks.add(listThread);
      listThread.start();
    }
    Iterator<Thread> threadIterator = arrayTasks.iterator();
    while(threadIterator.hasNext()) {
      Thread thread = threadIterator.next();
      thread.join();
      threadIterator.remove();
      System.out.println(thread.getName() + " has finished!");
    }
    System.out.println(list);
    System.out.println("Removing all elements asynchronously...");
    for (int i = 0; i < TASK_COUNT; i++) {
      Future future = EXECUTOR_SERVICE.submit(new ConcurrentArrayRemoveElementTask(list));
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
