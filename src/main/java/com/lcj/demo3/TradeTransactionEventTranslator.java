package com.lcj.demo3;

import java.util.Random;

import com.lcj.demo1.TradeTransaction;
import com.lmax.disruptor.EventTranslator;

public class TradeTransactionEventTranslator implements EventTranslator<TradeTransaction> {

	private Random random = new Random();

	public void translateTo(TradeTransaction event, long sequence) {
		this.generateTradeTransaction(event);
	}

	private TradeTransaction generateTradeTransaction(TradeTransaction trade) {
		trade.setPrice(random.nextDouble() * 9999);
		return trade;
	}
}
