package com.lcj.loan.disruptor;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lcj.loan.redis.RedisClient;

import redis.clients.jedis.ShardedJedis;

/**
 * 封装了对redis队列的访问
 * 
 * @author qianll
 *
 */
@Service
public class EventQueue {

	@Resource
	private RedisClient redisClient;

	/*
	 * @Value("${redis.client.namespace}") private String namespace;
	 */

	/**
	 * 弹出列表中的第一个元素，无则返回null
	 * 
	 * @param queueName
	 * @return
	 */
	public String lpop(String queueName) {
		String result = null;
		try {
			result = redisClient.lpop(queueName);
			if (result != null && result.equals("nil")) {
				result = null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * 从列表头插入元素，返回插入后列表中的元素数
	 * 
	 * @param queueName
	 * @param elements
	 * @return
	 */
	public Long lpush(String queueName, String... elements) {
		Long result = 0l;
		try {
			result = redisClient.lpush(queueName, elements);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * 从列表头开始删除前count个value值
	 * 
	 * @param key
	 * @param count
	 * @param value
	 * @return 删除的元素个数
	 */
	public Long lrem(String key, long count, String value) {
		Long result = 0l;
		try {
			result = redisClient.lrem(key, count, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * 获取列表中元素的个数，如果列表不存在，返回0.如果key存在但非列表类型，则返回错误
	 * 
	 * @param key
	 * @return
	 */
	public Long llen(String key) {
		Long result = 0l;
		try {
			result = redisClient.llen(key);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * 删除缓存key
	 * 
	 * @param key
	 * @return
	 */
	public Long del(String key) {
		Long result = 0l;
		try {
			result = redisClient.del(key);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public String rpop(String queueName) {
		String result = null;
		try {
			result = redisClient.rpop(queueName);
			if (result != null && result.equals("nil")) {
				result = null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
