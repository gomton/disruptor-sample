package com.lcj.demo3;

import com.lcj.demo1.TradeTransaction;
import com.lmax.disruptor.EventHandler;

public class TradeTransactionJMSNotifyHandler implements EventHandler<TradeTransaction> {
	public void onEvent(TradeTransaction event, 
				long sequence, boolean endOfBatch) throws Exception {
		System.out.println(" in TradeTransactionJMSNotifyHandler " + event.getId() +" seq : " + sequence);
	}
}
