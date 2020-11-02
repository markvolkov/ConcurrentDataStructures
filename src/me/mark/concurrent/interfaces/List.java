package me.mark.concurrent.interfaces;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;

public interface List<T> extends Iterable<T> {

  boolean add(T t);

  boolean remove(Object o);

  T get(int index) throws IllegalAccessException;

  T set(int index, T element) throws IllegalAccessException;

  void add(int index, T element);

  T remove(int index) throws IllegalAccessException;

  int indexOf(Object o);

  int lastIndexOf(Object o);

  boolean contains(Object o);

  int size();

  boolean isEmpty();

  void clear();

  Object[] toArray() throws IllegalAccessException;

  Iterator<T> iterator();

  boolean checkRange(int index);

  void ensureCapacity();

  void resizeArray();

  int hashCode();

  boolean equals(Object o);

  ListIterator<T> listIterator();

  Spliterator<T> splitIterator();

}
