package com.lcj.demo3;

import java.util.concurrent.CountDownLatch;

import com.lcj.demo1.TradeTransaction;
import com.lmax.disruptor.dsl.Disruptor;

public class TradeTransactionPublisher implements Runnable{
	Disruptor<TradeTransaction> disruptor;
	private CountDownLatch latch;
	private static int LOOP=10000000;//ģ��һǧ��ν��׵ķ���
	
	public TradeTransactionPublisher(CountDownLatch latch,Disruptor<TradeTransaction> disruptor){
			this.disruptor = disruptor;
			this.latch = latch;
	}
	 
	public void run() {
		TradeTransactionEventTranslator tradeTransloator=new TradeTransactionEventTranslator();
		for(int i=0;i<LOOP;i++){
			disruptor.publishEvent(tradeTransloator);
		}
		latch.countDown();
	}

}
