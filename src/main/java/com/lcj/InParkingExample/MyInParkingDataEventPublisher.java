package com.lcj.InParkingExample;

import java.util.concurrent.CountDownLatch;

import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * Producer�ࣺ�����ϱ�ͣ������ 
 * �����ߣ�����ͣ�����ĳ���
 */
public class MyInParkingDataEventPublisher implements Runnable{
	
	private CountDownLatch countDownLatch; // ���ڼ�����ʼ���������ȳ�ʼ��ִ����Ϻ�֪ͨ���̼߳�������
	private Disruptor<MyInParkingDataEvent> disruptor;
	private static final Integer NUM = 10; // 1,10,100,1000
	
	public MyInParkingDataEventPublisher(CountDownLatch countDownLatch,
			Disruptor<MyInParkingDataEvent> disruptor) {
		this.countDownLatch = countDownLatch;
		this.disruptor = disruptor;
	}
	
 
	public void run() {
		MyInParkingDataEventTranslator eventTranslator = new MyInParkingDataEventTranslator();
		try {
			for(int i = 0; i < NUM; i ++) {
				disruptor.publishEvent(eventTranslator);
				Thread.sleep(1000); // ����һ���ӽ�һ����
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			countDownLatch.countDown(); // ִ����Ϻ�֪ͨ await()����
			System.out.println(NUM + "�����Ѿ�ȫ���������ͣ������");
		}
	}
	
}
 
class MyInParkingDataEventTranslator implements EventTranslator<MyInParkingDataEvent> {
 
	 
	public void translateTo(MyInParkingDataEvent myInParkingDataEvent, long sequence) {
		this.generateData(myInParkingDataEvent);
	}
	
	private MyInParkingDataEvent generateData(MyInParkingDataEvent myInParkingDataEvent) {
		myInParkingDataEvent.setCarLicense("���ƺţ� ��A-" + (int)(Math.random() * 100000)); // �������һ�����ƺ�
		System.out.println("Thread Id " + Thread.currentThread().getId() + " д��һ��event");
		return myInParkingDataEvent;
	}
	
}
 