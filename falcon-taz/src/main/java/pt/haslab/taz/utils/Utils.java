package pt.haslab.taz.utils;

import java.util.*;

/**
 * Created by nunomachado on 09/03/18.
 */
public class Utils {

    /**
     * Inserts a value in a list associated with a key and creates the
     * list if it doesn't exist already
     * Returns true iff the value didn't exist before in the list
     */
    public static <K,V> boolean insertInMapToLists(Map<K,List<V>> map, K key, V value) {
        List<V> values_list = map.get(key);

        if(values_list == null) {
            values_list = new ArrayList<V>();
            map.put(key, values_list);
        }
        boolean contains = values_list.contains(value);
        values_list.add(value);
        return !contains;
    }

    public static <K,V> boolean insertInMapToSets(Map<K,Set<V>> map, K key, V value) {
        Set<V> values_list = map.get(key);

        if(values_list == null) {
            values_list = new HashSet<V>();
            map.put(key, values_list);
        }
        boolean contains = values_list.contains(value);
        values_list.add(value);
        return !contains;
    }

    public static <K,V> void insertAllInMapToSet(Map<K,Set<V>> map, K key, Collection<V> values) {
        if(values == null){
            return;
        }
        Set<V> valuesSet = map.get(key);

        if(valuesSet == null) {
            valuesSet = new HashSet<V>();
            map.put(key, valuesSet);
        }
        valuesSet.addAll(values);
    }
}
