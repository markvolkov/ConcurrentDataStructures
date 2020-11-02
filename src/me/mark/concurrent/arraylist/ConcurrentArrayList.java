package me.mark.concurrent.arraylist;

import me.mark.concurrent.interfaces.List;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.IntStream;

public class ConcurrentArrayList<T> implements me.mark.concurrent.interfaces.List<T> {

    private static final AtomicInteger DEFAULT_CAPACITY = new AtomicInteger(15);
    private static final ReentrantReadWriteLock RW_LOCK = new ReentrantReadWriteLock();
    private static final ReadLock READ_LOCK = RW_LOCK.readLock();
    private static final WriteLock WRITE_LOCK = RW_LOCK.writeLock();

    private static final int TIMEOUT = 100;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

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
        try {
            if (WRITE_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                ensureCapacity();
                objects[size.getAndIncrement()] = t;
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        try {
            if (WRITE_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                if (o == null) {
                    for (int i = 0; i < this.size(); i++) {
                        if (get(i) == null) {
                            System.arraycopy(this.objects, i + 1, this.objects, i, this.size() - i - 1);
                            size.decrementAndGet();
                            return true;
                        }
                    }
                } else {
                    for (int i = 0; i < this.size(); i++) {
                        if (Objects.equals(get(i), o)) {
                            System.arraycopy(this.objects, i + 1, this.objects, i, this.size() - i - 1);
                            size.decrementAndGet();
                            return true;
                        }
                    }
                }
            }
        } catch (InterruptedException | IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) throws IllegalAccessException {
        try {
            if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                checkRange(index);
                return (T) this.objects[index];
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        throw new IllegalAccessException("Unable to access element at index: " + index);
    }

    @Override
    public T set(int index, T element) throws IllegalAccessException {
        if (element == null) {
            throw new NullPointerException();
        }
        try {
            if (WRITE_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                checkRange(index);
                this.objects[index] = element;
                return element;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
        throw new IllegalAccessException("Unable to set element at index: " + index);
    }

    @Override
    public void add(int index, T element) {
        try {
            if (WRITE_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                checkRange(index);
                ensureCapacity();
                Object temp = get(index);
                set(index, element);
                for (int i = index + 1; i < size.get(); i++) {
                    Object current = get(i);
                    this.objects[i] = temp;
                    temp = current;
                }
                this.objects[size.getAndIncrement()] = temp;
            }
        } catch (IllegalAccessException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(int index) throws IllegalAccessException {
        try {
            if (WRITE_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                checkRange(index);
                T temp = null;
                temp = (T) this.objects[index];
                this.objects[index] = null;
                if (this.size() - index - 1 > 0) {
                    System.arraycopy(this.objects, index + 1, this.objects, index, this.size() - index - 1);
                }
                size.decrementAndGet();
                return temp;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
        throw new IllegalAccessException("Unable to remove element at index: " + index);
    }

    @Override
    public int indexOf(Object o) {
        try {
            if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                for (int i = 0; i < size.get(); i++) {
                    if (Objects.equals(objects[i], o)) {
                        return i;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        try {
            if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                for (int i = size.get() - 1; i >= 0; i--) {
                    if (Objects.equals(o, this.objects[i])) {
                        return i;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        return this.size.get();
    }

    @Override
    public boolean isEmpty() {
        return this.size() <= 0;
    }

    @Override
    public void clear() {
        try {
          if (WRITE_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            this.objects = new Object[DEFAULT_CAPACITY.get()];
            this.size = new AtomicInteger(0);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    @Override
    public Object[] toArray() throws IllegalAccessException {
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            return this.objects;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        throw new IllegalAccessException("Unable to access array.");
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
                try {
                  if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
                    T next = null;
                    try {
                      next = get(index.getAndIncrement());
                    } catch (IllegalAccessException e) {
                      e.printStackTrace();
                    } finally {
                      READ_LOCK.unlock();
                    }
                    return next;
                  }
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
                return null;
            }

        };
    }


    @Override
    public boolean checkRange(int index) {
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            if (index >= this.size() || index < 0) {
              throw new IndexOutOfBoundsException();
            }
            return true;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        return false;
    }

    private boolean internalRangeCheck(int index) {
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            return index < size() && index >= 0;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        return false;
    }

    @Override
    public void ensureCapacity() {
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            if (size() >= this.capacity.get() - 1) {
              resizeArray();
            }
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
    }

    @Override
    public void resizeArray() {
        try {
          if (WRITE_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            Object[] newArray = new Object[(int) Math.floor(this.capacity.get() * 1.5)];
            for (int i = 0; i < size.get(); i++) {
              newArray[i] = this.objects[i];
            }
            this.objects = newArray;
            this.capacity = new AtomicInteger(newArray.length);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    @Override
    public int hashCode() {
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            int result = 1;
            int prime = 17;
            result = prime * result + (this.capacity.get());
            result = prime * result + (this.size.get());
            for (Object o : this.objects) {
              result = prime * result + (o == null ? 0 : o.hashCode());
            }
            return result;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof List)) {
            return false;
        }
        if (!(o instanceof ConcurrentArrayList)) {
            return false;
        }
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
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
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
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
              try {
                ConcurrentArrayList.this.remove(index.get());
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
            }

            @Override
            public void set(T t) {
              try {
                ConcurrentArrayList.this.set(index.get(), t);
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
            }

            @Override
            public void add(T t) {
                ConcurrentArrayList.this.add(t);
            }

        };
    }

    @Override
    public Spliterator<T> splitIterator() {
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            return Spliterators.spliterator(this.objects, Spliterator.ORDERED);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        return Spliterators.spliterator(new Object[]{}, Spliterator.ORDERED);
    }

    @Override
    public String toString() {
        try {
          if (READ_LOCK.tryLock(TIMEOUT, TIME_UNIT)) {
            StringBuilder result = new StringBuilder();
            result.append("[");
            for (int i = 0; i < this.size.get(); i++) {
              try {
                result.append(get(i));
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
              if (i != this.size.get() - 1) {
                result.append(",");
              }
            }
            result.append("]");
            return result.toString();
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
            READ_LOCK.unlock();
        }
        return "";
    }
}
