package com.lcj.demo3;

import com.lcj.demo1.TradeTransaction;
import com.lmax.disruptor.EventHandler;

public class TradeTransactionVasConsumer implements EventHandler<TradeTransaction>{
 
	public void onEvent(TradeTransaction event, long sequence,
			boolean endOfBatch) throws Exception {
	     
	}

}
