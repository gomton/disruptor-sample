package com.lcj.loan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.lcj.loan.redis.JRedisSerializationUtils;

import java.util.Set;

import redis.clients.jedis.BinaryJedisCluster;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;

public class RedisClient {
	private final String name;
	private final JedisPool jedisPool;
	private final BinaryJedisCluster cluster;
	private final boolean isCluster;

	public BinaryJedisCluster getCluster() {
		return cluster;
	}

	public Pool<Jedis> getJedisPool() {
		return jedisPool;
	}

	private final ThreadLocal<Jedis> threadLocalJedis = new ThreadLocal<Jedis>();

	public RedisClient(String name, JedisPool jedisPool, BinaryJedisCluster cluster) {
	        this.name = name;
	        this.jedisPool = jedisPool;
	        this.cluster = cluster;
	        this.isCluster = this.cluster != null;
	    }

	/**
	 * ��� key value �Ե� redis ��� key �Ѿ���������ֵ�� SET �͸�д��ֵ���������͡� ����ĳ��ԭ����������ʱ�䣨TTL���ļ���˵��
	 * �� SET ����ɹ����������ִ��ʱ�� �����ԭ�е� TTL ���������
	 */
	public String set(String key, Object value) {
		return isCluster ? clusterSet(key, value) : jedisSet(key, value);
	}

	private String clusterSet(String key, Object value) {
		if (value == null) {
			cluster.del(keyToBytes(key));
			return null;
		} else {
			return cluster.set(keyToBytes(key), valueToBytes(value));
		}
	}

	private String jedisSet(String key, Object value) {
		Jedis jedis = getJedis();
		try {
			if (value == null) {
				jedis.del(keyToBytes(key));
				return null;
			} else {
				return jedis.set(keyToBytes(key), valueToBytes(value));
			}
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��� key value �Ե� redis ��� key �Ѿ���������ֵ�� SET �͸�д��ֵ���������͡� ����ĳ��ԭ����������ʱ�䣨TTL���ļ���˵��
	 * �� SET ����ɹ����������ִ��ʱ�� �����ԭ�е� TTL ���������
	 */
	public void update(String key, Object value) {
		if (isCluster) {
			clusterUpdate(key, value);
		} else {
			jedisUpdate(key, value);
		}
	}

	private void clusterUpdate(String key, Object value) {
		Jedis jedis = getJedis();
		try {
			byte[] bytes = keyToBytes(key);
			if (value == null) {
				cluster.del(bytes);
			} else {
				Long ttl = cluster.ttl(bytes);
				if (ttl > 0) {
					cluster.setex(bytes, ttl.intValue(), valueToBytes(value));
				} else {
					cluster.set(bytes, valueToBytes(value));
				}
			}
		} finally {
			close(jedis);
		}
	}

	private void jedisUpdate(String key, Object value) {
		Jedis jedis = getJedis();
		try {
			byte[] bytes = keyToBytes(key);
			if (value == null) {
				jedis.del(bytes);
			} else {
				Long ttl = jedis.pttl(bytes);
				if (ttl > 0) {
					jedis.psetex(bytes, ttl, valueToBytes(value));
				} else {
					jedis.set(bytes, valueToBytes(value));
				}
			}
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��� key value �Ե� redis������ key ������ʱ����Ϊ seconds (����Ϊ��λ)�� ��� key �Ѿ����ڣ� SETEX
	 * �����д��ֵ��
	 */
	public String setex(String key, int seconds, Object value) {
		return isCluster ? clusterSetex(key, seconds, value) : jedisSetex(key, seconds, value);
	}

	public String clusterSetex(String key, int seconds, Object value) {
		if (value == null) {
			cluster.del(keyToBytes(key));
			return null;
		}
		return cluster.setex(keyToBytes(key), seconds, valueToBytes(value));
	}

	public String jedisSetex(String key, int seconds, Object value) {
		Jedis jedis = getJedis();
		try {
			if (value == null) {
				jedis.del(keyToBytes(key));
				return null;
			}
			return jedis.setex(keyToBytes(key), seconds, valueToBytes(value));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���� key �������� value ֵ ��� key ��������ô��������ֵ nil ��
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.get(keyToBytes(key)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���� key �������� value ֵ ��� key ��������ô��������ֵ nil ��
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(List<String> keys) {
		if (keys == null) {
			return Collections.emptyList();
		}
		Jedis jedis = getJedis();
		try {
			List<T> list = new ArrayList<T>();
			for (String key : keys) {
				list.add((T) valueFromBytes(jedis.get(keyToBytes(key))));
			}
			return list;
		} finally {
			close(jedis);
		}
	}

	/**
	 * ɾ��������һ�� key �����ڵ� key �ᱻ���ԡ�
	 */
	public long del(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.del(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ɾ�������Ķ�� key �����ڵ� key �ᱻ���ԡ�
	 * 
	 * @param <T>
	 */
	public <T> long del(List<T> keys) {
		if (keys == null) {
			return 0;
		}
		Jedis jedis = getJedis();
		try {
			return jedis.del(keysToBytesList(keys));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ɾ�������Ķ�� key ��ͷ��
	 * 
	 * @param <T>
	 */
	public void clear(String region) {
		Jedis jedis = getJedis();
		try {
			Set<String> keys = jedis.keys(region + ":*");
			if (keys == null || keys.size() == 0) {
				return;
			}
			jedis.del(keysToBytesSet(keys));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ɾ�������Ķ�� key �����ڵ� key �ᱻ���ԡ�
	 */
	public long del(String... keys) {
		if (keys == null) {
			return 0;
		}
		Jedis jedis = getJedis();
		try {
			return jedis.del(keysToBytesArray(keys));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������з��ϸ���ģʽ pattern �� key �� KEYS * ƥ�����ݿ������� key �� KEYS h?llo ƥ�� hello �� hallo
	 * �� hxllo �ȡ� KEYS h*llo ƥ�� hllo �� heeeeello �ȡ� KEYS h[ae]llo ƥ�� hello �� hallo
	 * ������ƥ�� hillo �� ��������� \ ����
	 */
	public Set<String> keys(String pattern) {
		Jedis jedis = getJedis();
		try {
			return jedis.keys(pattern);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ͬʱ����һ������ key-value �ԡ� ���ĳ������ key �Ѿ����ڣ���ô MSET ������ֵ����ԭ���ľ�ֵ������ⲻ������ϣ����Ч�����뿼��ʹ��
	 * MSETNX �����ֻ�������и��� key �������ڵ�����½������ò����� MSET ��һ��ԭ����(atomic)���������и��� key
	 * ������ͬһʱ���ڱ����ã�ĳЩ���� key �����¶���һЩ���� key û�иı������������ܷ�����
	 * 
	 * <pre>
	 * ���ӣ�
	 * Cache cache = RedisKit.use();            // ʹ�� Redis �� cache
	 * cache.mset("k1", "v1", "k2", "v2");      // ������ key value ��ֵ��
	 * List list = cache.mget("k1", "k2");      // ���ö����ֵ�õ������������ֵ
	 * </pre>
	 */
	public String mset(String... keysValues) {
		if (keysValues.length % 2 != 0)
			throw new IllegalArgumentException("wrong number of arguments for met, keysValues length can not be odd");
		Jedis jedis = getJedis();
		try {
			byte[][] kv = new byte[keysValues.length][];
			for (int i = 0; i < keysValues.length; i++) {
				if (i % 2 == 0)
					kv[i] = keyToBytes(keysValues[i]);
				else
					kv[i] = valueToBytes(keysValues[i]);
			}
			return jedis.mset(kv);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��������(һ������)���� key ��ֵ�� ��������� key ���棬��ĳ�� key �����ڣ���ô��� key ��������ֵ nil
	 * ����ˣ�����������ʧ�ܡ�
	 */
	@SuppressWarnings("rawtypes")
	public List mget(String... keys) {
		Jedis jedis = getJedis();
		try {
			byte[][] keysBytesArray = keysToBytesArray(keys);
			List<byte[]> data = jedis.mget(keysBytesArray);
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �� key �д��������ֵ��һ�� ��� key �����ڣ���ô key ��ֵ���ȱ���ʼ��Ϊ 0 ��Ȼ����ִ�� DECR ������
	 * ���ֵ������������ͣ����ַ������͵�ֵ���ܱ�ʾΪ���֣���ô����һ������ ��������ֵ������ 64 λ(bit)�з������ֱ�ʾ֮�ڡ�
	 * ���ڵ���(increment) / �ݼ�(decrement)�����ĸ�����Ϣ����μ� INCR ���
	 */
	public Long decr(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.decr(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �� key �������ֵ��ȥ���� decrement �� ��� key �����ڣ���ô key ��ֵ���ȱ���ʼ��Ϊ 0 ��Ȼ����ִ�� DECRBY ������
	 * ���ֵ������������ͣ����ַ������͵�ֵ���ܱ�ʾΪ���֣���ô����һ������ ��������ֵ������ 64 λ(bit)�з������ֱ�ʾ֮�ڡ�
	 * ���ڸ������(increment) / �ݼ�(decrement)�����ĸ�����Ϣ����μ� INCR ���
	 */
	public Long decrBy(String key, long longValue) {
		Jedis jedis = getJedis();
		try {
			return jedis.decrBy(keyToBytes(key), longValue);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �� key �д��������ֵ��һ�� ��� key �����ڣ���ô key ��ֵ���ȱ���ʼ��Ϊ 0 ��Ȼ����ִ�� INCR ������
	 * ���ֵ������������ͣ����ַ������͵�ֵ���ܱ�ʾΪ���֣���ô����һ������ ��������ֵ������ 64 λ(bit)�з������ֱ�ʾ֮�ڡ�
	 */
	public Long incr(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.incr(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �� key �������ֵ�������� increment �� ��� key �����ڣ���ô key ��ֵ���ȱ���ʼ��Ϊ 0 ��Ȼ����ִ�� INCRBY ���
	 * ���ֵ������������ͣ����ַ������͵�ֵ���ܱ�ʾΪ���֣���ô����һ������ ��������ֵ������ 64 λ(bit)�з������ֱ�ʾ֮�ڡ�
	 * ���ڵ���(increment) / �ݼ�(decrement)�����ĸ�����Ϣ���μ� INCR ���
	 */
	public Long incrBy(String key, long longValue) {
		Jedis jedis = getJedis();
		try {
			return jedis.incrBy(keyToBytes(key), longValue);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ������ key �Ƿ���ڡ�
	 */
	public boolean exists(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.exists(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �ӵ�ǰ���ݿ����������(��ɾ��)һ�� key ��
	 */
	public String randomKey() {
		Jedis jedis = getJedis();
		try {
			return jedis.randomKey();
		} finally {
			close(jedis);
		}
	}

	/**
	 * �� key ����Ϊ newkey �� �� key �� newkey ��ͬ������ key ������ʱ������һ������ �� newkey �Ѿ�����ʱ��
	 * RENAME ������Ǿ�ֵ��
	 */
	public String rename(String oldkey, String newkey) {
		Jedis jedis = getJedis();
		try {
			return jedis.rename(keyToBytes(oldkey), keyToBytes(newkey));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ����ǰ���ݿ�� key �ƶ������������ݿ� db ���С� �����ǰ���ݿ�(Դ���ݿ�)�͸������ݿ�(Ŀ�����ݿ�)����ͬ���ֵĸ��� key ������ key
	 * �������ڵ�ǰ���ݿ⣬��ô MOVE û���κ�Ч���� ��ˣ�Ҳ����������һ���ԣ��� MOVE ������(locking)ԭ��(primitive)��
	 */
	public Long move(String key, int dbIndex) {
		Jedis jedis = getJedis();
		try {
			return jedis.move(keyToBytes(key), dbIndex);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �� key ԭ���Եشӵ�ǰʵ�����͵�Ŀ��ʵ����ָ�����ݿ��ϣ�һ�����ͳɹ��� key ��֤�������Ŀ��ʵ���ϣ�����ǰʵ���ϵ� key �ᱻɾ����
	 */
	public String migrate(String host, int port, String key, int destinationDb, int timeout) {
		Jedis jedis = getJedis();
		try {
			return jedis.migrate(valueToBytes(host), port, keyToBytes(key), destinationDb, timeout);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �л���ָ�������ݿ⣬���ݿ������� index ������ֵָ������ 0 ��Ϊ��ʼ����ֵ�� Ĭ��ʹ�� 0 �����ݿ⡣ ע�⣺�� Jedis
	 * ���󱻹ر�ʱ�����ݿ��ֻ����±�����Ϊ��ʼֵ�����Ա����� select(...) ����������Ҫʹ�����·�ʽ֮һ�� 1��ʹ��
	 * RedisInterceptor���ڱ��߳��ڹ���ͬһ�� Jedis ���� 2��ʹ�� Redis.call(ICallback) ���в��� 3�����л�ȡ
	 * Jedis ������в���
	 */
	public String select(int databaseIndex) {
		Jedis jedis = getJedis();
		try {
			return jedis.select(databaseIndex);
		} finally {
			close(jedis);
		}
	}

	/**
	 * Ϊ���� key ��������ʱ�䣬�� key ����ʱ(����ʱ��Ϊ 0 )�����ᱻ�Զ�ɾ���� �� Redis �У���������ʱ��� key
	 * ����Ϊ����ʧ�ġ�(volatile)��
	 */
	public Long expire(String key, int seconds) {
		Jedis jedis = getJedis();
		try {
			return jedis.expire(keyToBytes(key), seconds);
		} finally {
			close(jedis);
		}
	}

	/**
	 * EXPIREAT �����ú� EXPIRE ���ƣ�������Ϊ key ��������ʱ�䡣��ͬ���� EXPIREAT ������ܵ�ʱ������� UNIX
	 * ʱ���(unix timestamp)��
	 */
	public Long expireAt(String key, long unixTime) {
		Jedis jedis = getJedis();
		try {
			return jedis.expireAt(keyToBytes(key), unixTime);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� EXPIRE ������������ƣ��������Ժ���Ϊ��λ���� key ������ʱ�䣬������ EXPIRE ��������������Ϊ��λ��
	 */
	public Long pexpire(String key, long milliseconds) {
		Jedis jedis = getJedis();
		try {
			return jedis.pexpire(keyToBytes(key), milliseconds);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� EXPIREAT �������ƣ������Ժ���Ϊ��λ���� key �Ĺ��� unix ʱ������������� EXPIREAT ����������Ϊ��λ��
	 */
	public Long pexpireAt(String key, long millisecondsTimestamp) {
		Jedis jedis = getJedis();
		try {
			return jedis.pexpireAt(keyToBytes(key), millisecondsTimestamp);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ������ key ��ֵ��Ϊ value �������� key �ľ�ֵ(old value)�� �� key ���ڵ������ַ�������ʱ������һ������
	 */
	@SuppressWarnings("unchecked")
	public <T> T getSet(String key, Object value) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.getSet(keyToBytes(key), valueToBytes(value)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �Ƴ����� key ������ʱ�䣬����� key �ӡ���ʧ�ġ�(������ʱ�� key )ת���ɡ��־õġ�(һ����������ʱ�䡢�������ڵ� key )��
	 */
	public Long persist(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.persist(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���� key �������ֵ�����͡�
	 */
	public String type(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.type(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ����Ϊ��λ�����ظ��� key ��ʣ������ʱ��(TTL, time to live)��
	 */
	public Long ttl(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.ttl(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ������������� TTL ��������Ժ���Ϊ��λ���� key ��ʣ������ʱ�䣬�������� TTL ��������������Ϊ��λ��
	 */
	public Long pttl(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.pttl(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������õ�����
	 */
	public Long objectRefcount(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.objectRefcount(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ����û�б����ʵĿ���ʱ��
	 */
	public Long objectIdletime(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.objectIdletime(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ����ϣ�� key �е��� field ��ֵ��Ϊ value �� ��� key �����ڣ�һ���µĹ�ϣ������������ HSET ������ ����� field
	 * �Ѿ������ڹ�ϣ���У���ֵ�������ǡ�
	 */
	public Long hset(String key, Object field, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.hset(keyToBytes(key), keyToBytes(field), valueToBytes(value));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ����ϣ�� key �е��� field ��ֵ��Ϊ value �������ó�ʱʱ�� ��� key �����ڣ�һ���µĹ�ϣ������������ HSET ������ �����
	 * field �Ѿ������ڹ�ϣ���У���ֵ�������ǡ�
	 * 
	 * @return
	 */
	public Long hsetEx(String key, Object field, Object value, int expire) {
		Jedis jedis = getJedis();
		try {
			Long hset = jedis.hset(keyToBytes(key), keyToBytes(field), valueToBytes(value));
			jedis.expire(keyToBytes(key + ":" + field), expire);
			return hset;
		} finally {
			close(jedis);
		}
	}

	/**
	 * ͬʱ����� field-value (��-ֵ)�����õ���ϣ�� key �С� ������Ḳ�ǹ�ϣ�����Ѵ��ڵ��� ��� key
	 * �����ڣ�һ���չ�ϣ��������ִ�� HMSET ������
	 */
	public String hmset(String key, Map<Object, Object> hash) {
		Jedis jedis = getJedis();
		try {
			Map<byte[], byte[]> para = new HashMap<byte[], byte[]>();
			for (Entry<Object, Object> e : hash.entrySet())
				para.put(keyToBytes(e.getKey()), valueToBytes(e.getValue()));
			return jedis.hmset(keyToBytes(key), para);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ع�ϣ�� key �и����� field ��ֵ��
	 */
	@SuppressWarnings("unchecked")
	public <T> T hget(String key, Object field) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.hget(keyToBytes(key), keyToBytes(field)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ع�ϣ�� key �У�һ�������������ֵ�� ����������򲻴����ڹ�ϣ����ô����һ�� nil ֵ�� ��Ϊ�����ڵ� key
	 * ������һ���չ�ϣ�����������Զ�һ�������ڵ� key ���� HMGET ����������һ��ֻ���� nil ֵ�ı�
	 */
	@SuppressWarnings("rawtypes")
	public List hmget(String key, Object... fields) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.hmget(keyToBytes(key), keysToBytesArray(fields));
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ɾ����ϣ�� key �е�һ������ָ���򣬲����ڵ��򽫱����ԡ�
	 */
	public Long hdel(String key, Object... fields) {
		Jedis jedis = getJedis();
		try {
			return jedis.hdel(keyToBytes(key), keysToBytesArray(fields));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �鿴��ϣ�� key �У������� field �Ƿ���ڡ�
	 */
	public boolean hexists(String key, Object field) {
		Jedis jedis = getJedis();
		try {
			return jedis.hexists(keyToBytes(key), keyToBytes(field));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ع�ϣ�� key �У����е����ֵ�� �ڷ���ֵ�����ÿ������(field name)֮�������ֵ(value)�����Է���ֵ�ĳ����ǹ�ϣ���С��������
	 */
	@SuppressWarnings("rawtypes")
	public Map hgetAll(String key) {
		Jedis jedis = getJedis();
		try {
			Map<byte[], byte[]> data = jedis.hgetAll(keyToBytes(key));
			Map<Object, Object> result = new HashMap<Object, Object>();
			for (Entry<byte[], byte[]> e : data.entrySet())
				result.put(keyFromBytes(e.getKey()), valueFromBytes(e.getValue()));
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ع�ϣ�� key ���������ֵ��
	 */
	@SuppressWarnings("rawtypes")
	public List hvals(String key) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.hvals(keyToBytes(key));
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ع�ϣ�� key �е�������
	 */
	public Set<String> hkeys(String key) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> keySet = jedis.hkeys(keyToBytes(key));
			return keySetFromBytesSet(keySet); // ���� key �ķ�������ʹ��
												// valueSetFromBytesSet(...)
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ع�ϣ�� key �����������
	 */
	public Long hlen(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.hlen(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �����б� key �У��±�Ϊ index ��Ԫ�ء� �±�(index)���� start �� stop ���� 0 Ϊ�ף�Ҳ����˵���� 0
	 * ��ʾ�б�ĵ�һ��Ԫ�أ��� 1 ��ʾ�б�ĵڶ���Ԫ�أ��Դ����ơ� ��Ҳ����ʹ�ø����±꣬�� -1 ��ʾ�б�����һ��Ԫ�أ� -2
	 * ��ʾ�б�ĵ����ڶ���Ԫ�أ��Դ����ơ� ��� key �����б����ͣ�����һ������
	 */
	@SuppressWarnings("unchecked")

	/**
	 * �����б� key �У��±�Ϊ index ��Ԫ�ء� �±�(index)���� start �� stop ���� 0 Ϊ�ף�Ҳ����˵���� 0
	 * ��ʾ�б�ĵ�һ��Ԫ�أ� �� 1 ��ʾ�б�ĵڶ���Ԫ�أ��Դ����ơ� ��Ҳ����ʹ�ø����±꣬�� -1 ��ʾ�б�����һ��Ԫ�أ� -2
	 * ��ʾ�б�ĵ����ڶ���Ԫ�أ��Դ����ơ� ��� key �����б����ͣ�����һ������
	 */
	public <T> T lindex(String key, long index) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.lindex(keyToBytes(key), index));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��ȡ��������ֵ
	 */
	public Long getCounter(String key) {
		Jedis jedis = getJedis();
		try {
			return Long.parseLong((String) jedis.get(key.toString()));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �����б� key �ĳ��ȡ� ��� key �����ڣ��� key ������Ϊһ�����б����� 0 . ��� key �����б����ͣ�����һ������
	 */
	public Long llen(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.llen(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �Ƴ��������б� key ��ͷԪ�ء�
	 */
	@SuppressWarnings("unchecked")
	public <T> T lpop(String key) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.lpop(keyToBytes(key)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��һ������ֵ value ���뵽�б� key �ı�ͷ ����ж�� value ֵ����ô���� value ֵ�������ҵ�˳�����β��뵽��ͷ�� ����˵��
	 * �Կ��б� mylist ִ������ LPUSH mylist a b c ���б��ֵ���� c b a �� ���ͬ��ԭ���Ե�ִ�� LPUSH mylist a
	 * �� LPUSH mylist b �� LPUSH mylist c ������� ��� key �����ڣ�һ�����б�ᱻ������ִ�� LPUSH ������ ��
	 * key ���ڵ������б�����ʱ������һ������
	 */
	public Long lpush(String key, Object... values) {
		Jedis jedis = getJedis();
		try {
			return jedis.lpush(keyToBytes(key), valuesToBytesArray(values));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���б� key �±�Ϊ index ��Ԫ�ص�ֵ����Ϊ value �� �� index ����������Χ�����һ�����б�( key ������)���� LSET
	 * ʱ������һ������ �����б��±�ĸ�����Ϣ����ο� LINDEX ���
	 */
	public String lset(String key, long index, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.lset(keyToBytes(key), index, valueToBytes(value));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ݲ��� count ��ֵ���Ƴ��б�������� value ��ȵ�Ԫ�ء� count ��ֵ���������¼��֣� count > 0 :
	 * �ӱ�ͷ��ʼ���β�������Ƴ��� value ��ȵ�Ԫ�أ�����Ϊ count �� count < 0 : �ӱ�β��ʼ���ͷ�������Ƴ��� value
	 * ��ȵ�Ԫ�أ�����Ϊ count �ľ���ֵ�� count = 0 : �Ƴ����������� value ��ȵ�ֵ��
	 */
	public Long lrem(String key, long count, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.lrem(keyToBytes(key), count, valueToBytes(value));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �����б� key ��ָ�������ڵ�Ԫ�أ�������ƫ���� start �� stop ָ���� �±�(index)���� start �� stop ���� 0
	 * Ϊ�ף�Ҳ����˵���� 0 ��ʾ�б�ĵ�һ��Ԫ�أ��� 1 ��ʾ�б�ĵڶ���Ԫ�أ��Դ����ơ� ��Ҳ����ʹ�ø����±꣬�� -1 ��ʾ�б�����һ��Ԫ�أ� -2
	 * ��ʾ�б�ĵ����ڶ���Ԫ�أ��Դ����ơ�
	 * 
	 * <pre>
	 * ���ӣ�
	 * ��ȡ list ���������ݣ�cache.lrange(listKey, 0, -1);
	 * ��ȡ list ���±� 1 �� 3 �����ݣ� cache.lrange(listKey, 1, 3);
	 * </pre>
	 */
	@SuppressWarnings("rawtypes")
	public List lrange(String key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.lrange(keyToBytes(key), start, end);
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��һ���б�����޼�(trim)������˵�����б�ֻ����ָ�������ڵ�Ԫ�أ�����ָ������֮�ڵ�Ԫ�ض�����ɾ���� �ٸ����ӣ�ִ������ LTRIM list 0 2
	 * ����ʾֻ�����б� list ��ǰ����Ԫ�أ�����Ԫ��ȫ��ɾ���� �±�(index)���� start �� stop ���� 0 Ϊ�ף�Ҳ����˵���� 0
	 * ��ʾ�б�ĵ�һ��Ԫ�أ��� 1 ��ʾ�б�ĵڶ���Ԫ�أ��Դ����ơ� ��Ҳ����ʹ�ø����±꣬�� -1 ��ʾ�б�����һ��Ԫ�أ� -2
	 * ��ʾ�б�ĵ����ڶ���Ԫ�أ��Դ����ơ� �� key �����б�����ʱ������һ������
	 */
	public String ltrim(String key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			return jedis.ltrim(keyToBytes(key), start, end);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �Ƴ��������б� key ��βԪ�ء�
	 */
	@SuppressWarnings("unchecked")
	public <T> T rpop(String key) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.rpop(keyToBytes(key)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���� RPOPLPUSH ��һ��ԭ��ʱ���ڣ�ִ���������������� ���б� source �е����һ��Ԫ��(βԪ��)�����������ظ��ͻ��ˡ� �� source
	 * ������Ԫ�ز��뵽�б� destination ����Ϊ destination �б�ĵ�ͷԪ�ء�
	 */
	@SuppressWarnings("unchecked")
	public <T> T rpoplpush(String srcKey, String dstKey) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.rpoplpush(keyToBytes(srcKey), keyToBytes(dstKey)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��һ������ֵ value ���뵽�б� key �ı�β(���ұ�)�� ����ж�� value ֵ����ô���� value
	 * ֵ�������ҵ�˳�����β��뵽��β������ ��һ�����б� mylist ִ�� RPUSH mylist a b c ���ó��Ľ���б�Ϊ a b c ��
	 * ��ͬ��ִ������ RPUSH mylist a �� RPUSH mylist b �� RPUSH mylist c �� ��� key
	 * �����ڣ�һ�����б�ᱻ������ִ�� RPUSH ������ �� key ���ڵ������б�����ʱ������һ������
	 */
	public Long rpush(String key, Object... values) {
		Jedis jedis = getJedis();
		try {
			return jedis.rpush(keyToBytes(key), valuesToBytesArray(values));
		} finally {
			close(jedis);
		}
	}

	/**
	 * BLPOP ���б������ʽ(blocking)����ԭ� ���� LPOP ����������汾���������б���û���κ�Ԫ�ؿɹ�������ʱ�����ӽ��� BLPOP
	 * ����������ֱ���ȴ���ʱ���ֿɵ���Ԫ��Ϊֹ�� ��������� key ����ʱ�������� key ���Ⱥ�˳�����μ������б�������һ���ǿ��б��ͷԪ�ء�
	 */
	@SuppressWarnings("rawtypes")
	public List blpop(String... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.blpop(keysToBytesArray(keys));
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * BLPOP ���б������ʽ(blocking)����ԭ� ���� LPOP ����������汾���������б���û���κ�Ԫ�ؿɹ�������ʱ�����ӽ��� BLPOP
	 * ����������ֱ���ȴ���ʱ���ֿɵ���Ԫ��Ϊֹ�� ��������� key ����ʱ�������� key ���Ⱥ�˳�����μ������б�������һ���ǿ��б��ͷԪ�ء�
	 */
	@SuppressWarnings("rawtypes")
	public List blpop(int timeout, String... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.blpop(timeout, keysToBytesArray(keys));
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * BRPOP ���б������ʽ(blocking)����ԭ� ���� RPOP ����������汾���������б���û���κ�Ԫ�ؿɹ�������ʱ�����ӽ��� BRPOP
	 * ����������ֱ���ȴ���ʱ���ֿɵ���Ԫ��Ϊֹ�� ��������� key ����ʱ�������� key ���Ⱥ�˳�����μ������б�������һ���ǿ��б��β��Ԫ�ء�
	 * �������������ĸ�����Ϣ����鿴 BLPOP ��� BRPOP ���˵���Ԫ�ص�λ�ú� BLPOP ��֮ͬ�⣬��������һ�¡�
	 */
	@SuppressWarnings("rawtypes")
	public List brpop(String... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.brpop(keysToBytesArray(keys));
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * BRPOP ���б������ʽ(blocking)����ԭ� ���� RPOP ����������汾���������б���û���κ�Ԫ�ؿɹ�������ʱ�����ӽ��� BRPOP
	 * ����������ֱ���ȴ���ʱ���ֿɵ���Ԫ��Ϊֹ�� ��������� key ����ʱ�������� key ���Ⱥ�˳�����μ������б�������һ���ǿ��б��β��Ԫ�ء�
	 * �������������ĸ�����Ϣ����鿴 BLPOP ��� BRPOP ���˵���Ԫ�ص�λ�ú� BLPOP ��֮ͬ�⣬��������һ�¡�
	 */
	@SuppressWarnings("rawtypes")
	public List brpop(int timeout, String... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.brpop(timeout, keysToBytesArray(keys));
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * ʹ�ÿͻ����� Redis ����������һ�� PING ��������������������Ļ����᷵��һ�� PONG ��
	 * ͨ�����ڲ�����������������Ƿ���Ȼ��Ч���������ڲ����ӳ�ֵ��
	 */
	public String ping() {
		Jedis jedis = getJedis();
		try {
			return jedis.ping();
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��һ������ member Ԫ�ؼ��뵽���� key ���У��Ѿ������ڼ��ϵ� member Ԫ�ؽ������ԡ� ���� key �����ڣ��򴴽�һ��ֻ����
	 * member Ԫ������Ա�ļ��ϡ� �� key ���Ǽ�������ʱ������һ������
	 */
	public Long sadd(String key, Object... members) {
		Jedis jedis = getJedis();
		try {
			return jedis.sadd(keyToBytes(key), valuesToBytesArray(members));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ؼ��� key �Ļ���(������Ԫ�ص�����)��
	 */
	public Long scard(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.scard(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �Ƴ������ؼ����е�һ�����Ԫ�ء� ���ֻ���ȡһ�����Ԫ�أ��������Ԫ�شӼ����б��Ƴ��Ļ�������ʹ�� SRANDMEMBER ���
	 */
	@SuppressWarnings("unchecked")
	public <T> T spop(String key) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.spop(keyToBytes(key)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ؼ��� key �е����г�Ա�� �����ڵ� key ����Ϊ�ռ��ϡ�
	 */
	@SuppressWarnings("rawtypes")
	public Set smembers(String key) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.smembers(keyToBytes(key));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * �ж� member Ԫ���Ƿ񼯺� key �ĳ�Ա��
	 */
	public boolean sismember(String key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.sismember(keyToBytes(key), valueToBytes(member));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ض�����ϵĽ�������������� keys ָ��
	 */
	@SuppressWarnings("rawtypes")
	public Set sinter(String... keys) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.sinter(keysToBytesArray(keys));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ؼ����е�һ�����Ԫ�ء�
	 */
	@SuppressWarnings("unchecked")
	public <T> T srandmember(String key) {
		Jedis jedis = getJedis();
		try {
			return (T) valueFromBytes(jedis.srandmember(keyToBytes(key)));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ؼ����е� count �����Ԫ�ء� �� Redis 2.6 �汾��ʼ�� SRANDMEMBER ������ܿ�ѡ�� count ������ ��� count
	 * Ϊ��������С�ڼ��ϻ�������ô�����һ������ count ��Ԫ�ص����飬�����е�Ԫ�ظ�����ͬ�� ��� count ���ڵ��ڼ��ϻ�������ô�����������ϡ�
	 * ��� count Ϊ��������ô�����һ�����飬�����е�Ԫ�ؿ��ܻ��ظ����ֶ�Σ�������ĳ���Ϊ count �ľ���ֵ�� �ò����� SPOP ���ƣ���
	 * SPOP �����Ԫ�شӼ������Ƴ������أ��� SRANDMEMBER ������������Ԫ�أ������Լ��Ͻ����κθĶ���
	 */
	@SuppressWarnings("rawtypes")
	public List srandmember(String key, int count) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.srandmember(keyToBytes(key), count);
			return valueListFromBytesList(data);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �Ƴ����� key �е�һ������ member Ԫ�أ������ڵ� member Ԫ�ػᱻ���ԡ�
	 */
	public Long srem(String key, Object... members) {
		Jedis jedis = getJedis();
		try {
			return jedis.srem(keyToBytes(key), valuesToBytesArray(members));
		} finally {
			close(jedis);
		}
	}

	/**
	 * ���ض�����ϵĲ�������������� keys ָ�� �����ڵ� key ����Ϊ�ռ���
	 */
	@SuppressWarnings("rawtypes")
	public Set sunion(String... keys) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.sunion(keysToBytesArray(keys));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * ����һ�����ϵ�ȫ����Ա���ü��������и�������֮��Ĳ�� �����ڵ� key ����Ϊ�ռ���
	 */
	@SuppressWarnings("rawtypes")
	public Set sdiff(String... keys) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.sdiff(keysToBytesArray(keys));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * ��һ������ member Ԫ�ؼ��� score ֵ���뵽���� key ���С� ���ĳ�� member �Ѿ������򼯵ĳ�Ա����ô������� member
	 * �� score ֵ�� ��ͨ�����²������ member Ԫ�أ�����֤�� member ����ȷ��λ���ϡ�
	 */
	public Long zadd(String key, double score, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zadd(keyToBytes(key), score, valueToBytes(member));
		} finally {
			close(jedis);
		}
	}

	public Long zadd(String key, Map<Object, Double> scoreMembers) {
		Jedis jedis = getJedis();
		try {
			Map<byte[], Double> para = new HashMap<byte[], Double>();
			for (Entry<Object, Double> e : scoreMembers.entrySet())
				para.put(valueToBytes(e.getKey()), e.getValue()); // valueToBytes
																	// is
																	// important
			return jedis.zadd(keyToBytes(key), para);
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �Ļ�����
	 */
	public Long zcard(String key) {
		Jedis jedis = getJedis();
		try {
			return jedis.zcard(keyToBytes(key));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �У� score ֵ�� min �� max ֮��(Ĭ�ϰ��� score ֵ���� min �� max )�ĳ�Ա�������� ���ڲ��� min
	 * �� max ����ϸʹ�÷�������ο� ZRANGEBYSCORE ���
	 */
	public Long zcount(String key, double min, double max) {
		Jedis jedis = getJedis();
		try {
			return jedis.zcount(keyToBytes(key), min, max);
		} finally {
			close(jedis);
		}
	}

	/**
	 * Ϊ���� key �ĳ�Ա member �� score ֵ�������� increment ��
	 */
	public Double zincrby(String key, double score, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zincrby(keyToBytes(key), score, valueToBytes(member));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �У�ָ�������ڵĳ�Ա�� ���г�Ա��λ�ð� score ֵ����(��С����)������ ������ͬ score
	 * ֵ�ĳ�Ա���ֵ���(lexicographical order )�����С� �������Ҫ��Ա�� score ֵ�ݼ�(�Ӵ�С)�����У���ʹ��
	 * ZREVRANGE ���
	 */
	@SuppressWarnings("rawtypes")
	public Set zrange(String key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.zrange(keyToBytes(key), start, end);
			Set<Object> result = new LinkedHashSet<Object>(); // ���򼯺ϱ���
																// LinkedHashSet
			valueSetFromBytesSet(data, result);
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �У�ָ�������ڵĳ�Ա�� ���г�Ա��λ�ð� score ֵ�ݼ�(�Ӵ�С)�����С� ������ͬ score
	 * ֵ�ĳ�Ա���ֵ��������(reverse lexicographical order)���С� ���˳�Ա�� score ֵ�ݼ��Ĵ���������һ���⣬
	 * ZREVRANGE �������������� ZRANGE ����һ����
	 */
	@SuppressWarnings("rawtypes")
	public Set zrevrange(String key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.zrevrange(keyToBytes(key), start, end);
			Set<Object> result = new LinkedHashSet<Object>(); // ���򼯺ϱ���
																// LinkedHashSet
			valueSetFromBytesSet(data, result);
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �У����� score ֵ���� min �� max ֮��(�������� min �� max )�ĳ�Ա�� ���򼯳�Ա�� score
	 * ֵ����(��С����)�������С�
	 */
	@SuppressWarnings("rawtypes")
	public Set zrangeByScore(String key, double min, double max) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.zrangeByScore(keyToBytes(key), min, max);
			Set<Object> result = new LinkedHashSet<Object>(); // ���򼯺ϱ���
																// LinkedHashSet
			valueSetFromBytesSet(data, result);
			return result;
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �г�Ա member ���������������򼯳�Ա�� score ֵ����(��С����)˳�����С� ������ 0 Ϊ�ף�Ҳ����˵�� score
	 * ֵ��С�ĳ�Ա����Ϊ 0 �� ʹ�� ZREVRANK ������Ի�ó�Ա�� score ֵ�ݼ�(�Ӵ�С)���е�������
	 */
	public Long zrank(String key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zrank(keyToBytes(key), valueToBytes(member));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �г�Ա member ���������������򼯳�Ա�� score ֵ�ݼ�(�Ӵ�С)���� ������ 0 Ϊ�ף�Ҳ����˵�� score
	 * ֵ���ĳ�Ա����Ϊ 0 �� ʹ�� ZRANK ������Ի�ó�Ա�� score ֵ����(��С����)���е�������
	 */
	public Long zrevrank(String key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zrevrank(keyToBytes(key), valueToBytes(member));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �Ƴ����� key �е�һ��������Ա�������ڵĳ�Ա�������ԡ� �� key ���ڵ�������������ʱ������һ������
	 */
	public Long zrem(String key, Object... members) {
		Jedis jedis = getJedis();
		try {
			return jedis.zrem(keyToBytes(key), valuesToBytesArray(members));
		} finally {
			close(jedis);
		}
	}

	/**
	 * �������� key �У���Ա member �� score ֵ�� ��� member Ԫ�ز������� key �ĳ�Ա���� key �����ڣ����� nil ��
	 */
	public Double zscore(String key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zscore(keyToBytes(key), valueToBytes(member));
		} finally {
			close(jedis);
		}
	}

	public void subscribe(JedisPubSub jedisPubSub, String... channels) {
		Jedis jedis = getJedis();
		try {
			jedis.subscribe(jedisPubSub, channels);
		} finally {
			close(jedis);
		}
	}

	public void publish(String channel, byte[] message) {
		Jedis jedis = getJedis();
		try {
			jedis.publish(keyToBytes(channel), message);
		} finally {
			close(jedis);
		}
	}

	// ---------

	public byte[] keyToBytes(String key) {
		return SafeEncoder.encode(key);
	}

	private byte[] keyToBytes(Object key) {
		return SafeEncoder.encode(key.toString());
	}

	private String keyFromBytes(byte[] bytes) {
		return SafeEncoder.encode(bytes);
	}

	private byte[][] keysToBytesArray(String... keys) {
		byte[][] result = new byte[keys.length][];
		for (int i = 0; i < result.length; i++)
			result[i] = keyToBytes(keys[i]);
		return result;
	}

	private <T> byte[][] keysToBytesList(List<T> keys) {
		if (keys == null) {
			return null;
		}
		byte[][] result = new byte[keys.size()][];
		for (int i = 0; i < result.length; i++)
			result[i] = keyToBytes(keys.get(i));
		return result;
	}

	private <T> byte[][] keysToBytesSet(Set<T> keys) {
		if (keys == null) {
			return null;
		}
		byte[][] result = new byte[keys.size()][];
		int i = 0;
		for (T key : keys) {
			result[i++] = keyToBytes(key);
		}
		return result;
	}

	private byte[][] keysToBytesArray(Object... keys) {
		byte[][] result = new byte[keys.length][];
		for (int i = 0; i < result.length; i++)
			result[i] = keyToBytes(keys[i]);
		return result;
	}

	private Set<String> keySetFromBytesSet(Set<byte[]> data) {
		Set<String> result = new HashSet<String>();
		for (byte[] keyBytes : data)
			result.add(keyFromBytes(keyBytes));
		return result;
	}

	private byte[] valueToBytes(Object object) {
		return JRedisSerializationUtils.fastSerialize(object);
	}

	protected Object valueFromBytes(byte[] bytes) {
		if (bytes != null)
			try {
				return JRedisSerializationUtils.fastDeserialize(bytes);
			} catch (Exception e) {
			 
				e.printStackTrace();
			}
		return null;
	}

	private byte[][] valuesToBytesArray(Object... objectArray) {
		byte[][] data = new byte[objectArray.length][];
		for (int i = 0; i < data.length; i++)
			data[i] = valueToBytes(objectArray[i]);
		return data;
	}

	private void valueSetFromBytesSet(Set<byte[]> data, Set<Object> result) {
		for (byte[] d : data)
			result.add(valueFromBytes(d));
	}

	@SuppressWarnings("rawtypes")
	private List valueListFromBytesList(List<byte[]> data) {
		List<Object> result = new ArrayList<Object>();
		for (byte[] d : data)
			result.add(valueFromBytes(d));
		return result;
	}

	public String getName() {
		return name;
	}

	public Jedis getJedis() {
		Jedis jedis = threadLocalJedis.get();
		if (jedis == null) {
			if (jedisPool != null) {
				jedis = jedisPool.getResource();
			}
		}
		return jedis;
	}

	public void close(Jedis jedis) {
		if (threadLocalJedis.get() == null && jedis != null)
			jedis.close();
	}

	public Jedis getThreadLocalJedis() {
		return threadLocalJedis.get();
	}

	public void setThreadLocalJedis(Jedis jedis) {
		threadLocalJedis.set(jedis);
	}

	public void removeThreadLocalJedis() {
		threadLocalJedis.remove();
	}

	public void puffsubscribe(BinaryJedisPubSub jedisPubSub, String... channels) {
		final byte[][] cs = new byte[channels.length][];
		for (int i = 0; i < cs.length; i++) {
			cs[i] = SafeEncoder.encode(channels[i]);
		}
		if (cluster == null) {
			Jedis jedis = getJedis();
			try {
				jedis.subscribe(jedisPubSub, cs);
			} finally {
				close(jedis);
			}
		} else {
			cluster.subscribe(jedisPubSub, cs);
		}
	}

	public void puffpublish(String channel, byte[] message) {
		if (cluster == null) {
			Jedis jedis = getJedis();
			try {
				jedis.publish(keyToBytes(channel), message);
			} finally {
				close(jedis);
			}
		} else {
			cluster.publish(keyToBytes(channel), message);
		}
	}

}
