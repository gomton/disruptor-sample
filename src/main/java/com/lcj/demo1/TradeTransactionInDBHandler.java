package com.lcj.demo1;

import java.util.UUID;

import com.lmax.disruptor.EventHandler;

public class TradeTransactionInDBHandler implements EventHandler<TradeTransaction>{
	
	public void onEvent(TradeTransaction event, long sequence, boolean endOfBatch) throws Exception {
		this.onEvent(event);
	}
	
	public void onEvent(TradeTransaction event) throws Exception{
		event.setId(UUID.randomUUID().toString());
		System.out.println(event.getId());
	}
}