package com.lcj.ringBuffer;

import java.util.UUID;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

public class TradeHandler implements EventHandler<Trade>, WorkHandler<Trade> {
	public void onEvent(Trade event, long sequence, boolean endOfBatch) throws Exception {
		this.onEvent(event);
	}

	public void onEvent(Trade event) throws Exception {
		// ����������������߼�
		event.setId(UUID.randomUUID().toString());// ��������ID
		System.out.println(event.getId());
	}
}
