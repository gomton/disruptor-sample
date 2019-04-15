package com.lcj.demo2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.lcj.demo1.TradeTransaction;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.WorkerPool;

public class Demo2 {
	public static void main(String[] args) throws InterruptedException {
		final int BUFFER_SIZE = 1024;
		final int THREAD_NUMBERS = 4;
		EventFactory<TradeTransaction> eventFactory = new EventFactory<TradeTransaction>() {
			public TradeTransaction newInstance() {
				return new TradeTransaction();
			}
		};
 
		RingBuffer<TradeTransaction> ringBuffer = RingBuffer.createSingleProducer(eventFactory, BUFFER_SIZE);
 
		SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
 
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUMBERS);
 
		WorkHandler<TradeTransaction>[] workHandlers = new WorkHandler[3];
		for (int i = 0; i < 3; i++) {
			WorkHandler<TradeTransaction> workHandler = new TradeTransactionInDBHandler();
			workHandlers[i] = workHandler;
		}
 
		WorkerPool<TradeTransaction> workerPool = new WorkerPool<TradeTransaction>(ringBuffer, 
				sequenceBarrier, new IgnoreExceptionHandler(), workHandlers);
 
		workerPool.start(executor);
 
		for (int i = 0; i < 800; i++) {
			long seq = ringBuffer.next();
			ringBuffer.get(seq).setPrice(Math.random() * 9999);
			ringBuffer.publish(seq);
			if (i % 10 == 0) {
				System.out.println(((ThreadPoolExecutor) executor).getActiveCount() 
						+ "============================================");
			}
		}
		Thread.sleep(10000);
		workerPool.halt();
		Thread.sleep(1);
		executor.shutdown();
	}

}
