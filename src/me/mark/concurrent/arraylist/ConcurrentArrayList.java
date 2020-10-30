package me.mark.concurrent.arraylist;

import me.mark.concurrent.interfaces.List;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.IntStream;

public class ConcurrentArrayList<T> implements me.mark.concurrent.interfaces.List<T> {

  private static final AtomicInteger DEFAULT_CAPACITY = new AtomicInteger(15);
  private static final ReentrantReadWriteLock RW_LOCK = new ReentrantReadWriteLock();
  private static final ReadLock READ_LOCK = RW_LOCK.readLock();
  private static final WriteLock WRITE_LOCK = RW_LOCK.writeLock();

  //Bounds of array
  private AtomicInteger capacity;
  private volatile Object[] objects;
  //Internal object count
  private AtomicInteger size;

  public ConcurrentArrayList() {
    this(DEFAULT_CAPACITY.get());
  }

  public ConcurrentArrayList(int capacity) {
    if (capacity <= 0) {
      this.objects = new Object[DEFAULT_CAPACITY.get()];
      this.capacity = DEFAULT_CAPACITY;
    } else {
      this.capacity = new AtomicInteger(capacity);
      this.objects = new Object[this.capacity.get()];
    }
    this.size = new AtomicInteger(0);
  }

  @Override
  public boolean add(T t) {
    WRITE_LOCK.lock();
    try {
      ensureCapacity();
      objects[size.getAndIncrement()] = t;
    } finally {
      WRITE_LOCK.unlock();
    }
    return true;
  }

  @Override
  public boolean remove(Object o) {
    WRITE_LOCK.lock();
    try {
      if (o == null) {
        for (int i = 0; i < this.size(); i++) {
          if (get(i) == null) {
            System.arraycopy(this.objects, i + 1, this.objects, i,this.size() - i - 1);
            size.decrementAndGet();
            break;
          }
        }
      } else {
        for (int i = 0; i < this.size(); i++) {
          if (Objects.equals(get(i), o)) {
            System.arraycopy(this.objects, i + 1, this.objects, i,this.size() - i - 1);
            size.decrementAndGet();
            break;
          }
        }
      }
    } finally {
      WRITE_LOCK.unlock();
    }
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get(int index) {
    READ_LOCK.lock();
    checkRange(index);
    try {
      return (T) this.objects[index];
    } finally {
      READ_LOCK.unlock();
    }
  }

  @Override
  public T set(int index, T element) {
    if (element == null) {
      throw new NullPointerException();
    }
    WRITE_LOCK.lock();
    checkRange(index);
    try {
      this.objects[index] = element;
    } finally {
      WRITE_LOCK.unlock();
    }
    return element;
  }

  @Override
  public void add(int index, T element) {
    WRITE_LOCK.lock();
    checkRange(index);
    try {
      ensureCapacity();
      Object temp = get(index);
      set(index, element);
      for (int i = index + 1; i < size.get(); i++) {
        Object current = get(i);
        this.objects[i] = temp;
        temp = current;
      }
      this.objects[size.getAndIncrement()] = temp;
    } finally {
      WRITE_LOCK.unlock();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public T remove(int index) {
    WRITE_LOCK.lock();
    checkRange(index);
    T temp = null;
    try {
      temp = (T) this.objects[index];
      this.objects[index] = null;
      if (this.size() - index - 1 > 0) {
        System.arraycopy(this.objects, index + 1, this.objects, index,this.size() - index - 1);
      }
      size.decrementAndGet();
    } finally {
      WRITE_LOCK.unlock();
    }
    return temp;
  }

  @Override
  public int indexOf(Object o) {
    READ_LOCK.lock();
    try {
      for (int i = 0; i < size.get(); i++) {
        if (Objects.equals(objects[i], o)) {
          return i;
        }
      }
    } finally {
      READ_LOCK.unlock();
    }
    return -1;
  }

  @Override
  public int lastIndexOf(Object o) {
    READ_LOCK.lock();
    try {
      for (int i = size.get() - 1; i >= 0; i--) {
        if (Objects.equals(o, this.objects[i])) {
          return i;
        }
      }
    } finally {
      READ_LOCK.unlock();
    }
    return -1;
  }

  @Override
  public boolean contains(Object o) {
    return indexOf(o) != -1;
  }

  @Override
  public int size() {
    READ_LOCK.lock();
    try {
      return this.size.get();
    } finally {
      READ_LOCK.unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    return this.size() <= 0;
  }

  @Override
  public void clear() {
    WRITE_LOCK.lock();
    try {
      this.objects = new Object[this.DEFAULT_CAPACITY.get()];
      this.size = new AtomicInteger(0);
    } finally {
      WRITE_LOCK.unlock();
    }
  }

  @Override
  public Object[] toArray() {
    READ_LOCK.lock();
    try {
      return this.objects;
    } finally {
      READ_LOCK.unlock();
    }
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      private AtomicInteger index = new AtomicInteger(0);

      @Override
      public boolean hasNext() {
        return internalRangeCheck(index.get());
      }

      @Override
      public T next() {
        READ_LOCK.lock();
        T next = null;
        try {
          next = get(index.getAndIncrement());
        } finally {
          READ_LOCK.unlock();
        }
        return next;
      }

    };
  }


  @Override
  public boolean checkRange(int index) {
    READ_LOCK.lock();
    try {
      if (index >= this.size() || index < 0) {
        throw new IndexOutOfBoundsException();
      }
      return true;
    } finally {
      READ_LOCK.unlock();
    }
  }

  private boolean internalRangeCheck(int index) {
    READ_LOCK.lock();
    try {
      if (index >= size() || index < 0) {
        return false;
      }
      return true;
    } finally {
      READ_LOCK.unlock();
    }
  }

  @Override
  public void ensureCapacity() {
    READ_LOCK.lock();
    try {
      if (size() >= this.capacity.get() - 1) {
        resizeArray();
      }
    } finally {
      READ_LOCK.unlock();
    }
  }

  @Override
  public void resizeArray() {
    WRITE_LOCK.lock();
    try {
      Object[] newArray = new Object[(int) Math.floor(this.capacity.get() * 1.5)];
      for (int i = 0; i < size.get(); i++) {
        newArray[i] = this.objects[i];
      }
      this.objects = newArray;
      this.capacity = new AtomicInteger(newArray.length);
    } finally {
      WRITE_LOCK.unlock();
    }
  }

  @Override
  public int hashCode() {
    READ_LOCK.lock();
    int result = 1;
    try {
      int prime = 17;
      result = prime * result + (this.capacity.get());
      result = prime * result + (this.size.get());
      for (Object o : this.objects) {
        result = prime * result + (o == null ? 0 : o.hashCode());
      }
    } finally {
      READ_LOCK.unlock();
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof List)) {
      return false;
    }
    if (!(o instanceof ConcurrentArrayList)) {
      return false;
    }
    READ_LOCK.lock();
    try {
      ConcurrentArrayList other = (ConcurrentArrayList) o;
      if (!(other.size.equals(this.size)) || (!(other.capacity.equals(this.capacity)))) {
        return false;
      }
      for (int i = 0; i < size.get(); i++) {
        if (Objects.equals(other.objects[i], this.objects[i])) {
          continue;
        }
        return false;
      }
    } finally {
      READ_LOCK.unlock();
    }
    return true;
  }

  @Override
  public ListIterator<T> listIterator() {
    return new ListIterator<T>() {

      private AtomicInteger index = new AtomicInteger(0);

      @Override
      public boolean hasNext() {
        return internalRangeCheck(index.get());
      }

      @Override
      @SuppressWarnings("unchecked")
      public T next() {
        return (T) objects[index.getAndIncrement()];
      }

      @Override
      public boolean hasPrevious() {
        return index.get() < size.get() && index.get() > 0;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T previous() {
        return (T) objects[index.getAndDecrement()];
      }

      @Override
      public int nextIndex() {
        AtomicInteger next = new AtomicInteger(index.get());
        return next.incrementAndGet();
      }

      @Override
      public int previousIndex() {
        AtomicInteger prev = new AtomicInteger(index.get());

        return prev.decrementAndGet();
      }

      @Override
      public void remove() {
        ConcurrentArrayList.this.remove(index.get());
      }

      @Override
      public void set(T t) {
        ConcurrentArrayList.this.set(index.get(), t);
      }

      @Override
      public void add(T t) {
        ConcurrentArrayList.this.add(t);
      }

    };
  }

  @Override
  public Spliterator<T> splitIterator() {
    READ_LOCK.lock();
    try {
      return Spliterators.spliterator(this.objects, Spliterator.ORDERED);
    } finally {
      READ_LOCK.unlock();
    }
  }

  @Override
  public String toString() {
    READ_LOCK.lock();
    try {
      StringBuilder result = new StringBuilder();
      result.append("[");
      for (int i = 0; i < this.size.get(); i++) {
        result.append(get(i));
        if (i != this.size.get() - 1) {
          result.append(",");
        }
      }
      result.append("]");
      return result.toString();
    } finally {
      READ_LOCK.unlock();
    }
  }
}
