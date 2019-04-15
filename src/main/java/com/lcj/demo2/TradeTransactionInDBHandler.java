package com.lcj.demo2;

import java.util.UUID;

import com.lcj.demo1.TradeTransaction;
import com.lmax.disruptor.WorkHandler;

public class TradeTransactionInDBHandler implements WorkHandler<TradeTransaction> {
	public void onEvent(TradeTransaction event) throws Exception {
		event.setId(UUID.randomUUID().toString());
		System.out.println(event.getId() + " price : " + event.getPrice());		
	}
}
