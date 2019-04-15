package com.lcj.loan.annotation;

public interface EventHandler {
	/**
	 * 通过类型和入参返回队列中的key
	 * @param type
	 * @param para
	 * @return
	 */
	String getKey(int type,Object[] para);
}