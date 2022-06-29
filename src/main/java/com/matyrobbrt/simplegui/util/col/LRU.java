package com.matyrobbrt.simplegui.util.col;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class LRU<T> extends AbstractCollection<T> {

    private final Map<T, Entry<T>> lookupMap = new Object2ObjectOpenHashMap<>();

    private final Entry<T> head, tail;
    private int size;

    public LRU() {
        head = new Entry<T>(null);
        tail = new Entry<T>(null);
        head.next = tail;
        tail.prev = head;
    }

    private void remove(Entry<T> entry) {
        entry.next.prev = entry.prev;
        entry.prev.next = entry.next;
        size--;
        lookupMap.remove(entry.value);
    }

    private void addFirst(Entry<T> entry) {
        entry.prev = head;
        entry.next = head.next;
        entry.next.prev = entry;
        head.next = entry;
        size++;
        lookupMap.put(entry.value, entry);
    }

    @Override
    public boolean add(T element) {
        addFirst(new Entry<>(element));
        return true;
    }

    public void moveUp(T element) {
        final var entry = lookupMap.get(element);
        if (entry == null) {
            return;
        }
        remove(entry);
        addFirst(entry);
    }

    @Override
    public boolean remove(Object element) {
        final var entry = lookupMap.get(element);
        if (entry == null) {
            return false;
        }
        remove(entry);
        return true;
    }

    @Override
    public boolean contains(Object element) {
        return lookupMap.containsKey(element);
    }

    @Override
    public int size() {
        return size;
    }

    public void reverseIterate(Consumer<T> callback) {
        var ptr = tail.prev;
        while (ptr != head) {
            callback.accept(ptr.value);
            ptr = ptr.prev;
        }
    }

    private static class Entry<T> {

        private final T value;
        private Entry<T> prev, next;

        private Entry(T value) {
            this.value = value;
        }
    }

    @Nonnull
    @Override
    public LRUIterator iterator() {
        return new LRUIterator();
    }

    public LRUIterator descendingIterator() {
        return new LRUIterator().reverse();
    }

    public class LRUIterator implements Iterator<T> {

        private boolean reverse = false;
        private Entry<T> current = head;

        @Override
        public boolean hasNext() {
            return reverse ? current.prev != head : current.next != tail;
        }

        @Override
        public T next() {
            if (reverse) {
                current = current.prev;
                if (current == head) {
                    throw new NoSuchElementException("Reached beginning of LRU!");
                }
            } else {
                current = current.next;
                if (current == tail) {
                    throw new NoSuchElementException("Reached end of LRU!");
                }
            }
            return current == null ? null : current.value;
        }

        public LRUIterator reverse() {
            reverse = true;
            current = tail;
            return this;
        }
    }
}
