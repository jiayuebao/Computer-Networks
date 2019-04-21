package edu.cmu.ece;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LRUCache<T1, T2> {
    class Node {
        Node prev;
        Node next;
        T1 key;
        T2 val;

        public Node(T1 key, T2 val) {
            this.key = key;
            this.val = val;
        }
    }

    Node head = new Node(null,null);
    Node tail = new Node(null,null);
    int capacity;
    Map<T1, Node> map = new ConcurrentHashMap<>();

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public T2 get(T1 key) {
        Node node = map.get(key);
        if (node == null) return null;
        delete(node);
        insert(node);
        return node.val;
    }

    public void put(T1 key, T2 value) {
        Node node = map.get(key);
        if (node != null) {
            node.val = value;
            delete(node);
            insert(node);
        } else {
            capacity--;
            Node newNode = new Node(key, value);
            if (capacity < 0) {
                evict(tail.prev);
                capacity++;
            }
            insert(newNode);
            map.put(key, newNode);
        }
    }

    private void insert(Node node) {
        Node oldNext = head.next;
        head.next = node;
        node.next = oldNext;
        node.prev = head;
        oldNext.prev = node;
    }

    private void delete(Node node) {
        Node oldPrev = node.prev;
        Node oldNext = node.next;
        oldPrev.next = oldNext;
        oldNext.prev = oldPrev;
    }

    private void evict(Node node) {
        map.remove(node.key);
        delete(node);
    }
}
