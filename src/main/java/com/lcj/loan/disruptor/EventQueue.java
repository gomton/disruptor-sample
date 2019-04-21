package com.lcj.loan.disruptor;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lcj.loan.redis.RedisClient;

import redis.clients.jedis.ShardedJedis;

/**
 * ��װ�˶�redis���еķ���
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
	 * �����б��еĵ�һ��Ԫ�أ����򷵻�null
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
	 * ���б�ͷ����Ԫ�أ����ز�����б��е�Ԫ����
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
	 * ���б�ͷ��ʼɾ��ǰcount��valueֵ
	 * 
	 * @param key
	 * @param count
	 * @param value
	 * @return ɾ����Ԫ�ظ���
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
	 * ��ȡ�б���Ԫ�صĸ���������б����ڣ�����0.���key���ڵ����б����ͣ��򷵻ش���
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
	 * ɾ������key
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
