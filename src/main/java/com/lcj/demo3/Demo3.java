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
		//������C1,C2����֮��ִ��JMS��Ϣ���Ͳ��� Ҳ���������ߵ�C3
		handlerGroup.then(jmsConsumer);
		
		disruptor.start();
		CountDownLatch latch = new CountDownLatch(1);
		executor.submit(new TradeTransactionPublisher(latch, disruptor));
		latch.await();//�ȴ�����������.
		disruptor.shutdown();
		executor.shutdown();
		
		System.out.println("�ܺ�ʱ:"+(System.currentTimeMillis()-beginTime));
	}
}
