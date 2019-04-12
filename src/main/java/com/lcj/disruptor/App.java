package com.lcj.disruptor;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class App {

	public static void main(String[] args) {
		 EventFactory<LongEvent> eventFactory = new LongEventFactory();
		 ExecutorService executor = Executors.newSingleThreadExecutor();
		 int ringBufferSize = 1024 * 1024;
		 Disruptor<LongEvent> disruptor = new Disruptor<LongEvent>(eventFactory,
				 ringBufferSize,executor,ProducerType.SINGLE,new YieldingWaitStrategy());
		 EventHandler<LongEvent> eventHandler = new LongEventHandler();
		 disruptor.handleEventsWith(eventHandler);
		 
		 disruptor.start();
		 RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
		 LongEventProducer producer = new LongEventProducer(ringBuffer);
		 
		 ByteBuffer byteBuffer = ByteBuffer.allocate(8);
		 for(long l = 0; l < 100 ; l++) {
			 byteBuffer.putLong(0,1);
			 producer.onData(byteBuffer);
		 }
		 
		 /*long sequence = ringBuffer.next();
		 try {
			 LongEvent event = ringBuffer.get(sequence);
			 long data = (new Date()).getTime();
			 event.setValue(data);
		 }finally {
			 ringBuffer.publish(sequence);
		 }*/
		 
		 disruptor.shutdown();
		 executor.shutdown();
	}

}
