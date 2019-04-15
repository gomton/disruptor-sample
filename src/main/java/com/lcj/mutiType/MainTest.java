package com.lcj.mutiType;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class MainTest {
	public static void main(String[] args){
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
	    int ringBufferSize = 32;
	    
	    Disruptor<Event> disruptor = new Disruptor<Event>(new DefaultEventFactory(),
	    		ringBufferSize,executor, ProducerType.MULTI,new BlockingWaitStrategy());
	    FbWorkHandler handler = new FbWorkHandler();
	    FbWorkHandler[] arr = new FbWorkHandler[10];
	    for(int i=0;i<10;i++){
	    	arr[i] = handler;
	    }
	    disruptor.handleEventsWithWorkerPool(arr);
	    RingBuffer<Event> ringBuffer =  disruptor.start();
	    for(int i=0;i<100;i++){
	    	final String str = i+"";
	    	ringBuffer.publishEvent(new EventTranslator<Event>(){
			 
				public void translateTo(Event event, long sequence) {
					event.setEventType("1");
					event.setKey(str);
					event.setValue("XXXXXXXXXXX");
				}	    		
	    	});
	    	System.out.println("================"+ringBuffer.remainingCapacity()+" "+i);
	    }
	    disruptor.shutdown(); 
	}
}
