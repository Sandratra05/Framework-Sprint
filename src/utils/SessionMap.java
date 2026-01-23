package utils;

import java.util.*;
import jakarta.servlet.http.HttpSession;

/**
 * Wrapper Map<String, Object> qui délègue à HttpSession.
 * Les modifications sur cette map mettent à jour la session.
 */
public class SessionMap implements Map<String, Object> {
    private final HttpSession session;

    public SessionMap(HttpSession session) {
        this.session = session;
    }

    @Override
    public int size() {
        if (session == null) return 0;
        Enumeration<String> names = session.getAttributeNames();
        int count = 0;
        while (names.hasMoreElements()) {
            names.nextElement();
            count++;
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (session == null) return false;
        return session.getAttribute(key.toString()) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (session == null) return false;
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (Objects.equals(session.getAttribute(name), value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        if (session == null) return null;
        return session.getAttribute(key.toString());
    }

    @Override
    public Object put(String key, Object value) {
        if (session == null) return null;
        Object old = session.getAttribute(key);
        session.setAttribute(key, value);
        return old;
    }

    @Override
    public Object remove(Object key) {
        if (session == null) return null;
        Object old = session.getAttribute(key.toString());
        session.removeAttribute(key.toString());
        return old;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        if (session == null) return;
        for (Entry<? extends String, ? extends Object> e : m.entrySet()) {
            session.setAttribute(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        if (session == null) return;
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            session.removeAttribute(names.nextElement());
        }
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        if (session == null) return keys;
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            keys.add(names.nextElement());
        }
        return keys;
    }

    @Override
    public Collection<Object> values() {
        Collection<Object> vals = new ArrayList<>();
        if (session == null) return vals;
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            vals.add(session.getAttribute(names.nextElement()));
        }
        return vals;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new HashSet<>();
        if (session == null) return entries;
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            entries.add(new AbstractMap.SimpleEntry<>(name, session.getAttribute(name)));
        }
        return entries;
    }
}