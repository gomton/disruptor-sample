package com.lcj.InParkingExample;

import com.lmax.disruptor.EventHandler;

/**
 * �ڶ��������ߣ�������֪ͨ��֪������Ա(Kafka��һ�ָ��������ķֲ�ʽ����������Ϣϵͳ)
 */
public class MyParkingDataToKafkaHandler implements EventHandler<MyInParkingDataEvent>{
 
	 
	public void onEvent(MyInParkingDataEvent myInParkingDataEvent, long sequence, boolean endOfBatch)
			throws Exception {
		long threadId = Thread.currentThread().getId(); // ��ȡ��ǰ�߳�id
		String carLicense = myInParkingDataEvent.getCarLicense(); // ��ȡ���ƺ�
		System.out.println(String.format("Thread Id %s ���� %s ����ͣ������Ϣ�� kafkaϵͳ...", threadId, carLicense));
	}
 
}
 