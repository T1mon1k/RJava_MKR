/*
 * Copyright (c) 2014, NTUU KPI, Computer systems department and/or its affiliates. All rights reserved.
 * NTUU KPI PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 */

package ua.kpi.comsys.test2.implementation;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.math.BigInteger;
import java.util.NoSuchElementException;

import ua.kpi.comsys.test2.NumberList;

/**
 * Custom implementation of {@link NumberList}.
 * <p>
 * Represents a non-negative integer number stored as a linear doubly-linked list.
 * Each list element contains exactly one digit of the number in the hexadecimal
 * numeral system (base 16).
 * <p>
 * The additional operation defined by the assignment is the algebraic and logical
 * AND operation over two numbers.
 * <p>
 * The method {@link #changeScale()} returns a new list representing the same number
 * converted to the binary numeral system (base 2).
 *
 * @author Співак Артем Михайлович, IO-35, НЗК: 3519
 */
public class NumberListImpl implements NumberList {

    /** Node of the list. */
    private static final class Node {
        byte value;
        Node next;
        Node prev;

        Node(byte value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    /** Current base for digits stored in this list. */
    private int base;

    /** Decimal value of the number represented by this list. */
    private BigInteger decimalValue = BigInteger.ZERO;

    /**
     * Default constructor. Creates an empty list (no digits).
     */
    public NumberListImpl() {
        this.base = 16;
    }

    /**
     * Constructs new {@link NumberListImpl} by a decimal number from a file (string format).
     * The file may be resolved either from filesystem path or from classpath resources.
     *
     * @param file file where a decimal number is stored
     */
    public NumberListImpl(File file) {
        this.base = 16;
        String s = readAllTrim(file);
        initFromDecimalString(s);
    }

    /**
     * Constructs new {@link NumberListImpl} by a decimal number in string notation.
     *
     * @param value decimal number in string notation
     */
    public NumberListImpl(String value) {
        this.base = 16;
        initFromDecimalString(value);
    }

    private NumberListImpl(BigInteger decimal, int base) {
        this.base = base;
        this.decimalValue = (decimal == null) ? BigInteger.ZERO : decimal.max(BigInteger.ZERO);
        rebuildDigitsFromDecimal();
    }

    /**
     * Saves the number stored in the list into the specified file in decimal notation.
     *
     * @param file output file
     */
    public void saveList(File file) {
        if (file == null)
            return;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(toDecimalString());
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns student's record book number.
     *
     * @return record book number
     */
    public static int getRecordBookNumber() {
        return 3519;
    }

    /**
     * Returns a new list representing the same number in the additional base.
     * Does not modify the original list.
     *
     * @return number in the additional base
     */
    public NumberListImpl changeScale() {
        return new NumberListImpl(this.decimalValue, 2);
    }

    /**
     * Performs the additional operation defined by the assignment (C7).
     * Operands must remain unchanged.
     *
     * @param arg second operand
     * @return result as a new list in the primary base
     */
    public NumberListImpl additionalOperation(NumberList arg) {
        BigInteger a = this.decimalValue;
        BigInteger b = toBigInteger(arg);

        if (b.signum() < 0)
            b = BigInteger.ZERO;

        if (a.signum() < 0)
            a = BigInteger.ZERO;

        BigInteger r = a.and(b);

        return new NumberListImpl(r, 16);
    }

    /**
     * Returns decimal string representation of the number.
     *
     * @return decimal string
     */
    public String toDecimalString() {
        return decimalValue.toString();
    }

    @Override
    public String toString() {
        if (size == 0)
            return "";
        StringBuilder sb = new StringBuilder(size);
        Node n = head;
        for (int i = 0; i < size; i++) {
            int d = n.value & 0xFF;
            sb.append(digitToChar(d));
            n = n.next;
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NumberListImpl))
            return false;
        NumberListImpl that = (NumberListImpl) o;
        return this.decimalValue.equals(that.decimalValue);
    }

    @Override
    public int hashCode() {
        return decimalValue.hashCode();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Byte))
            return false;
        byte v = (Byte) o;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v)
                return true;
            n = n.next;
        }
        return false;
    }

    @Override
    public Iterator<Byte> iterator() {
        return new Itr(0);
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        Node n = head;
        for (int i = 0; i < size; i++) {
            arr[i] = n.value;
            n = n.next;
        }
        return arr;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(Byte e) {
        if (e == null)
            throw new NullPointerException("Null digits are not allowed");
        ensureDigitInBase(e, base);
        linkLast(e);
        recalcDecimalFromDigits();
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Byte))
            return false;
        byte v = (Byte) o;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v) {
                unlink(n);
                recalcDecimalFromDigits();
                return true;
            }
            n = n.next;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null)
            return true;
        for (Object o : c) {
            if (!contains(o))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Byte> c) {
        if (c == null || c.isEmpty())
            return false;
        boolean changed = false;
        for (Byte b : c) {
            add(b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Byte> c) {
        if (c == null || c.isEmpty())
            return false;

        if (index < 0)
            index = 0;

        if (index > size)
            index = size;

        int i = index;
        boolean changed = false;
        for (Byte b : c) {
            add(i++, b);
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null || c.isEmpty())
            return false;
        boolean changed = false;
        for (Object o : c) {
            while (remove(o)) changed = true;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            if (size == 0)
                return false;
            clear();
            return true;
        }
        boolean changed = false;
        Node n = head;
        int i = 0;
        while (i < size) {
            Node next = n.next;
            if (!c.contains(n.value)) {
                unlink(n);
                changed = true;
            } else {
                i++;
            }
            n = next;
        }
        if (changed)
            recalcDecimalFromDigits();
        return changed;
    }

    @Override
    public void clear() {
        head = tail = null;
        size = 0;
        decimalValue = BigInteger.ZERO;
    }

    @Override
    public Byte get(int index) {
        Node n = nodeAt(index);
        return (n == null) ? null : n.value;
    }

    @Override
    public Byte set(int index, Byte element) {
        if (element == null)
            throw new NullPointerException("Null digits are not allowed");
        ensureDigitInBase(element, base);
        Node n = nodeAt(index);
        if (n == null)
            return null;
        byte old = n.value;
        n.value = element;
        recalcDecimalFromDigits();
        return old;
    }

    @Override
    public void add(int index, Byte element) {
        if (element == null)
            throw new NullPointerException("Null digits are not allowed");
        ensureDigitInBase(element, base);

        if (index <= 0) {
            linkFirst(element);
        } else if (index >= size) {
            linkLast(element);
        } else {
            Node succ = nodeAt(index);
            Node pred = succ.prev;

            Node newNode = new Node(element);
            newNode.next = succ;

            newNode.prev = pred;
            succ.prev = newNode;
            if (pred != null)
                pred.next = newNode;

            if (index == 0)
                head = newNode;

            size++;
        }
        recalcDecimalFromDigits();
    }

    @Override
    public Byte remove(int index) {
        Node n = nodeAt(index);
        if (n == null)
            return null;
        byte old = n.value;
        unlink(n);
        recalcDecimalFromDigits();
        return old;
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Byte))
            return -1;
        byte v = (Byte) o;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v)
                return i;
            n = n.next;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Byte))
            return -1;
        byte v = (Byte) o;
        int last = -1;
        Node n = head;
        for (int i = 0; i < size; i++) {
            if (n.value == v)
                last = i;
            n = n.next;
        }
        return last;
    }

    @Override
    public ListIterator<Byte> listIterator() {
        return new ListItr(0);
    }

    @Override
    public ListIterator<Byte> listIterator(int index) {
        return new ListItr(index);
    }

    @Override
    public List<Byte> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0)
            fromIndex = 0;
        if (toIndex > size)
            toIndex = size;
        if (toIndex < fromIndex)
            toIndex = fromIndex;

        NumberListImpl sub = new NumberListImpl();
        sub.base = this.base;

        Node n = nodeAt(fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            sub.linkLast(n.value);
            n = n.next;
        }
        sub.recalcDecimalFromDigits();
        return sub;
    }

    @Override
    public boolean swap(int index1, int index2) {
        if (index1 == index2)
            return true;
        if (index1 < 0 || index2 < 0 || index1 >= size || index2 >= size)
            return false;
        Node a = nodeAt(index1);
        Node b = nodeAt(index2);
        if (a == null || b == null)
            return false;
        byte tmp = a.value;
        a.value = b.value;
        b.value = tmp;
        recalcDecimalFromDigits();
        return true;
    }

    @Override
    public void sortAscending() {
        if (size < 2)
            return;
        for (int i = 0; i < size - 1; i++) {
            Node a = nodeAt(i);
            Node b = a.next;
            for (int j = i + 1; j < size; j++) {
                if ((a.value & 0xFF) > (b.value & 0xFF)) {
                    byte t = a.value;
                    a.value = b.value;
                    b.value = t;
                }
                b = b.next;
            }
        }
        recalcDecimalFromDigits();
    }

    @Override
    public void sortDescending() {
        if (size < 2)
            return;
        for (int i = 0; i < size - 1; i++) {
            Node a = nodeAt(i);
            Node b = a.next;
            for (int j = i + 1; j < size; j++) {
                if ((a.value & 0xFF) < (b.value & 0xFF)) {
                    byte t = a.value;
                    a.value = b.value;
                    b.value = t;
                }
                b = b.next;
            }
        }
        recalcDecimalFromDigits();
    }

    @Override
    public void shiftLeft() {
        if (size < 2)
            return;
        byte first = head.value;
        Node n = head;
        for (int i = 0; i < size - 1; i++) {
            n.value = n.next.value;
            n = n.next;
        }
        tail.value = first;
        recalcDecimalFromDigits();
    }

    @Override
    public void shiftRight() {
        if (size < 2)
            return;
        byte last = tail.value;
        Node n = tail;
        for (int i = 0; i < size - 1; i++) {
            n.value = n.prev.value;
            n = n.prev;
        }
        head.value = last;
        recalcDecimalFromDigits();
    }

    private void linkFirst(byte v) {
        Node newNode = new Node(v);
        if (size == 0) {
            head = tail = newNode;
            size = 1;
            return;
        }
        newNode.next = head;
        newNode.prev = null;
        head.prev = newNode;

        head = newNode;
        size++;
    }

    private void linkLast(byte v) {
        Node newNode = new Node(v);
        if (size == 0) {
            head = tail = newNode;
            size = 1;
            return;
        }
        tail.next = newNode;
        newNode.prev = tail;
        tail = newNode;
        size++;
    }

    private void unlink(Node x) {
        if (x == null || size == 0)
            return;

        if (size == 1) {
            head = tail = null;
            size = 0;
            return;
        }

        Node next = x.next;
        Node prev = x.prev;

        if (x == head) {
            head = next;
        } else {
            if (prev != null)
                prev.next = next;
        }

        if (x == tail) {
            tail = prev;
        } else {
            if (next != null)
                next.prev = prev;
        }

        size--;
    }

    private Node nodeAt(int index) {
        if (index < 0 || index >= size)
            return null;

        if (index <= size / 2) {
            Node n = head;
            for (int i = 0; i < index; i++) n = n.next;
            return n;
        } else {
            Node n = tail;
            for (int i = size - 1; i > index; i--) n = n.prev;
            return n;
        }
    }

    private void initFromDecimalString(String s) {
        String trimmed = (s == null) ? "" : s.trim();

        if (trimmed.isEmpty() || !trimmed.matches("\\d+")) {
            this.base = 16;
            clear();
            return;
        }

        BigInteger v;
        try {
            v = new BigInteger(trimmed);
        } catch (Exception e) {
            this.base = 16;
            clear();
            return;
        }

        if (v.signum() < 0) {
            this.base = 16;
            clear();
            return;
        }

        this.base = 16;
        this.decimalValue = v;
        rebuildDigitsFromDecimal();
    }

    private void rebuildDigitsFromDecimal() {
        clearNodesOnly();
        if (decimalValue == null) decimalValue = BigInteger.ZERO;

        if (decimalValue.equals(BigInteger.ZERO)) {
            linkLast((byte) 0);
            return;
        }

        BigInteger b = BigInteger.valueOf(base);
        BigInteger v = decimalValue;

        StringBuilder rev = new StringBuilder();
        while (v.signum() > 0) {
            BigInteger[] dr = v.divideAndRemainder(b);
            int digit = dr[1].intValue();
            rev.append((char) digit);
            v = dr[0];
        }

        for (int i = rev.length() - 1; i >= 0; i--) {
            byte d = (byte) rev.charAt(i);
            linkLast(d);
        }
    }

    private void clearNodesOnly() {
        head = tail = null;
        size = 0;
    }

    private void recalcDecimalFromDigits() {
        if (size == 0) {
            decimalValue = BigInteger.ZERO;
            return;
        }
        BigInteger b = BigInteger.valueOf(base);
        BigInteger v = BigInteger.ZERO;
        Node n = head;
        for (int i = 0; i < size; i++) {
            int d = n.value & 0xFF;
            v = v.multiply(b).add(BigInteger.valueOf(d));
            n = n.next;
        }
        decimalValue = v;
    }

    private static void ensureDigitInBase(byte digit, int base) {
        int d = digit & 0xFF;
        if (d < 0 || d >= base) {
            throw new IllegalArgumentException("Digit out of range for base " + base + ": " + d);
        }
    }

    private static char digitToChar(int d) {
        return (d < 10) ? (char) ('0' + d) : (char) ('A' + (d - 10));
    }


    private static BigInteger toBigInteger(NumberList list) {
        if (list == null) return BigInteger.ZERO;
        if (list instanceof NumberListImpl) {
            return ((NumberListImpl) list).decimalValue;
        }
        BigInteger b = BigInteger.valueOf(16);
        BigInteger v = BigInteger.ZERO;
        for (Byte x : list) {
            if (x == null)
                return BigInteger.ZERO;
            int d = x & 0xFF;
            if (d < 0 || d >= 16)
                return BigInteger.ZERO;
            v = v.multiply(b).add(BigInteger.valueOf(d));
        }
        return v;
    }

    private static String readAllTrim(File file) {
        if (file == null) return "";

        if (file.exists() && file.isFile()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            } catch (IOException ignored) {
            }
            return sb.toString().trim();
        }

        String[] candidates = new String[] {
            file.getPath().replace('\\', '/'),
            file.getName()
        };

        for (String name : candidates) {
            try (InputStream is = NumberListImpl.class.getClassLoader().getResourceAsStream(name)) {
                if (is == null)
                    continue;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                return sb.toString().trim();
            } catch (IOException ignored) {
            }
        }

        return "";
    }

    private class Itr implements Iterator<Byte> {
        Node current;
        Node lastReturned;
        int nextIndex;

        Itr(int index) {
            this.nextIndex = Math.max(0, Math.min(index, size));
            this.current = (this.nextIndex == size) ? null : nodeAt(this.nextIndex);
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }

        @Override
        public Byte next() {
            if (!hasNext())
                throw new NoSuchElementException();
            lastReturned = current;
            current = current.next;
            nextIndex++;
            return lastReturned.value;
        }

        @Override
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            Node target = lastReturned;
            lastReturned = null;
            unlink(target);
            recalcDecimalFromDigits();
            nextIndex--;
        }
    }

    private class ListItr implements ListIterator<Byte> {
        Node next;
        Node lastReturned;
        int nextIndex;

        ListItr(int index) {
            this.nextIndex = Math.max(0, Math.min(index, size));
            this.next = (this.nextIndex == size) ? null : nodeAt(this.nextIndex);
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }

        @Override
        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public Byte next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.value;
        }

        @Override
        public Byte previous() {
            if (!hasPrevious())
                throw new NoSuchElementException();
            if (next == null) {
                next = tail;
            } else {
                next = next.prev;
            }
            lastReturned = next;
            nextIndex--;
            return lastReturned.value;
        }

        @Override
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            Node target = lastReturned;
            lastReturned = null;
            unlink(target);
            recalcDecimalFromDigits();
            if (nextIndex > size) nextIndex = size;
        }

        @Override
        public void set(Byte e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (e == null)
                throw new NullPointerException("Null digits are not allowed");
            ensureDigitInBase(e, base);
            lastReturned.value = e;
            recalcDecimalFromDigits();
        }

        @Override
        public void add(Byte e) {
            if (e == null)
                throw new NullPointerException("Null digits are not allowed");
            ensureDigitInBase(e, base);
            NumberListImpl.this.add(nextIndex, e);
            nextIndex++;
        }
    }
}
