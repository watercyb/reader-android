package com.blueberryjoy.reader.main.window;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;

public class WindowIndexCache {
    @NonNull
    private final Node head = new Node();
    @NonNull
    private final Node tail = new Node();
    private Node first;
    private int n;
    private String titleCache = "";

    public WindowIndexCache(int n) {
        head.next = tail;
        tail.prev = head;
        first = head;
        this.n = n;
    }

    //Idx<0: do not update idx.
    public void addPage(String url, int idx, String title) {
        titleCache = title;
        url = decodeUrl(url);
        if (first.urls.contains(url)) {
            if (idx >= 0)
                first.idx = idx;
        } else {
            Node node = first.next;
            while (node != tail && !node.urls.contains(url)) {
                node = node.next;
            }
            if (node == tail) {
                insert(head, url, Math.max(idx, 0));
                first = head.next;
                if (n > 0) {
                    n--;
                } else {
                    delete(tail.prev);
                }
            } else {
                moveToFirst(node);
                first = node;
                if (idx >= 0)
                    node.idx = idx;
            }
        }
    }

    public void setIdx(int idx) {
        first.idx = idx;
    }

    //Move the matched node to the first.
    public int getIdx(@NonNull String url) {
        url = decodeUrl(url);
        if (first.urls.contains(url))
            return first.idx;
        Node node = first.next;
        while (node != tail && !node.urls.contains(url)) {
            node = node.next;
        }
        if (node == tail) {
            //Very rare times that the new page loaded when a same-page-loading at the final stage (after delay), this will be called since the same-page-loading will request cached idx using the new page url.
            insert(head, url, 0);
            first = head.next;
            if (n > 0) {
                n--;
            } else {
                delete(tail.prev);
            }
            return 0;
        } else {
            moveToFirst(node);
            first = node;
            return node.idx;
        }
    }

    public void setFirstUrl(String url) {
        first.urls.add(url);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private String decodeUrl(@NonNull String url) {
        if (!url.startsWith("file")) return url;
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    public boolean checkTitle(String title) {
        return titleCache.equals(title);
    }

    private void moveToFirst(@NonNull Node node) {
        delete(node);
        insert(head, node);
    }

    private void delete(@NonNull Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void insert(@NonNull Node node, @NonNull Node target) {
        node.next.prev = target;
        target.next = node.next;
        node.next = target;
        target.prev = node;
    }

    private void insert(@NonNull Node node, String url, int idx) {
        Node newNode = new Node(url, idx);
        node.next.prev = newNode;
        newNode.next = node.next;
        node.next = newNode;
        newNode.prev = node;
    }
}

class Node {
    @NonNull
    final HashSet<String> urls = new HashSet<>();
    int idx = 0;
    Node next;
    Node prev;

    public Node() {
    }

    public Node(String url, int idx) {
        urls.add(url);
        this.idx = idx;
    }
}