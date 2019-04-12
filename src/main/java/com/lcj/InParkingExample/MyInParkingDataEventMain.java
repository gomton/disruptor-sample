package com.lcj.InParkingExample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;

public class MyInParkingDataEventMain {
	public static void main(String[] args) {
		long beginTime=System.currentTimeMillis();
		int bufferSize = 2048; // 2��N�η�
		try {
			// �����̳߳أ�������Disruptor���ĸ�������
			ExecutorService executor = Executors.newFixedThreadPool(4);
			
			// ��ʼ��һ�� Disruptor
			Disruptor<MyInParkingDataEvent> disruptor = new Disruptor<MyInParkingDataEvent>(
					new EventFactory<MyInParkingDataEvent>() {
		 
				public MyInParkingDataEvent newInstance() {
					return new MyInParkingDataEvent(); // Event ��ʼ������
				}
			}, bufferSize, executor, ProducerType.SINGLE, new YieldingWaitStrategy());
			
			// ʹ��disruptor������������ MyParkingDataInDbHandler �� MyParkingDataToKafkaHandler
			EventHandlerGroup<MyInParkingDataEvent> handlerGroup = disruptor.handleEventsWith(
					new MyParkingDataInDbHandler(), new MyParkingDataToKafkaHandler());
			
			// ���������������ߴ�������������� smsHandler
			MyParkingDataSmsHandler myParkingDataSmsHandler = new MyParkingDataSmsHandler();
			handlerGroup.then(myParkingDataSmsHandler);
			
			// ����Disruptor
			disruptor.start();
			
			CountDownLatch countDownLatch = new CountDownLatch(1); // һ���������߳�׼�����˾Ϳ���֪ͨ���̼߳���������
			// ��������������
			executor.submit(new MyInParkingDataEventPublisher(countDownLatch, disruptor));
			countDownLatch.await(); // �ȴ������߽���
			
			disruptor.shutdown();
			executor.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("�ܺ�ʱ:"+(System.currentTimeMillis()-beginTime));
	
	}
}
