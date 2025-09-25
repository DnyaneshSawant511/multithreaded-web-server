import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private static class CacheEntry {
        byte[] data;
        long expiryTime;
    }

    private static final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long TTL = 60 * 1000; // 60 seconds

    public static byte[] get(String key) {

        CacheEntry entry = cache.get(key);
        if (entry == null) return null;

        if (System.currentTimeMillis() > entry.expiryTime) {
            cache.remove(key);
            return null;
        }
        return entry.data;
    }

    public static void put(String key, byte[] value) {
        CacheEntry entry = new CacheEntry();
        entry.data = value;
        entry.expiryTime = System.currentTimeMillis() + TTL;
        cache.put(key, entry);
    }

    public static boolean contains(String key) {
        return get(key) != null;
    }

    public static void clear() {
        cache.clear();
    }
}