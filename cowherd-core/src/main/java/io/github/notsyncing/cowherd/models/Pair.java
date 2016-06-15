package io.github.notsyncing.cowherd.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Pair<K, V>
{
    private K key;
    private V value;

    public Pair(K key, V value)
    {
        this.key = key;
        this.value = value;
    }

    public Pair()
    {
    }

    public K getKey()
    {
        return key;
    }

    public void setKey(K key)
    {
        this.key = key;
    }

    public V getValue()
    {
        return value;
    }

    public void setValue(V value)
    {
        this.value = value;
    }

    public static <K, V> boolean listContainsKey(List<Pair<K, V>> list, K key)
    {
        return list.stream().anyMatch(p -> p.getKey().equals(key));
    }

    public static <K, V> V listGetValue(List<Pair<K, V>> list, K key)
    {
        return list.stream().filter(p -> p.getKey().equals(key)).map(Pair::getValue).findFirst().orElse(null);
    }

    public static <K, V> List<V> listGetValues(List<Pair<K, V>> list, K key)
    {
        return list.stream().filter(p -> p.getKey().equals(key)).map(Pair::getValue).collect(Collectors.toList());
    }

    public static <K, V> V listSetValue(List<Pair<K, V>> list, K key, V value)
    {
        Pair<K, V> pair = list.stream().filter(p -> p.getKey().equals(key)).findFirst().orElse(null);

        if (pair == null) {
            return null;
        }

        V oldValue = pair.getValue();
        pair.setValue(value);
        return oldValue;
    }

    public static <K, V> Map<K, V> listToMap(List<Pair<K, V>> list)
    {
        return list.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    public static <K, V> ConcurrentMap<K, V> listToConcurrentMap(List<Pair<K, V>> list)
    {
        return list.stream().collect(Collectors.toConcurrentMap(Pair::getKey, Pair::getValue));
    }
}
