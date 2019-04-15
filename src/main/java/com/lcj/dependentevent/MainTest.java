package com.lcj.dependentevent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class MainTest {

	public static void main(String[] args){
	    EventFactory<MyEvent> myEventFactory = new MyEventFactory();
	    ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
	    int ringBufferSize = 32;
	    
	    Disruptor<MyEvent> disruptor = new Disruptor<MyEvent>(myEventFactory,ringBufferSize,executor, ProducerType.SINGLE,new BlockingWaitStrategy());
	    EventHandler<MyEvent> b = new MyEventHandlerB();
	    EventHandler<MyEvent> c = new MyEventHandlerC();
	    EventHandler<MyEvent> d = new MyEventHandlerD();
	    
	    SequenceBarrier sequenceBarrier2 = disruptor.handleEventsWith(b,c).asSequenceBarrier();
	    BatchEventProcessor processord = new BatchEventProcessor(disruptor.getRingBuffer(),sequenceBarrier2,d);
	    disruptor.handleEventsWith(processord);
	//  disruptor.after(b,c).handleEventsWith(d);              // �����ܴ��������еĳ����߼�
	    RingBuffer<MyEvent> ringBuffer = disruptor.start();    // ����Disruptor
	    for(int i=0; i<10; i++) {
	        long sequence = ringBuffer.next();                 // ����λ�� 
	        try {
	            MyEvent myEvent = ringBuffer.get(sequence);
	            myEvent.setValue(i);                           // ��������
	        } finally {
	            ringBuffer.publish(sequence);                  // �ύ��������ύ����¼���һֱ����
	        }
	        try{
	            Thread.sleep(100);
	        }catch (Exception e){
	        }
	    }
	    disruptor.shutdown(); 
	}	

}
