package com.lcj.loan.annotation;

public interface EventHandler {
	/**
	 * ͨ�����ͺ���η��ض����е�key
	 * @param type
	 * @param para
	 * @return
	 */
	String getKey(int type,Object[] para);
}