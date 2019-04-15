package com.lcj.demo3;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lcj.demo1.TradeTransaction;
import com.lcj.demo1.TradeTransactionInDBHandler;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;

public class Demo3 {
	public static void main(String[] args) throws Exception{
		long beginTime = System.currentTimeMillis();
		
		int bufferSize = 1024;
		
		ExecutorService executor=Executors.newFixedThreadPool(4);
		
		Disruptor<TradeTransaction> disruptor=new Disruptor<TradeTransaction>(new EventFactory<TradeTransaction>() {
			
			public TradeTransaction newInstance() {
				return new TradeTransaction();
			}
		}, bufferSize, executor, ProducerType.SINGLE, new BusySpinWaitStrategy());
 
		EventHandlerGroup<TradeTransaction> handlerGroup=disruptor.handleEventsWith(new TradeTransactionVasConsumer(),new TradeTransactionInDBHandler());
	
		TradeTransactionJMSNotifyHandler jmsConsumer=new TradeTransactionJMSNotifyHandler();
		//声明在C1,C2完事之后执行JMS消息发送操作 也就是流程走到C3
		handlerGroup.then(jmsConsumer);
		
		disruptor.start();
		CountDownLatch latch = new CountDownLatch(1);
		executor.submit(new TradeTransactionPublisher(latch, disruptor));
		latch.await();//等待生产者完事.
		disruptor.shutdown();
		executor.shutdown();
		
		System.out.println("总耗时:"+(System.currentTimeMillis()-beginTime));
	}
}
