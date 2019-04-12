package com.lcj.InParkingExample;

import com.lmax.disruptor.EventHandler;

/**
 * �����������ߣ�sms���ŷ��񣬸�֪˾�����Ѿ�����ͣ�������Ʒѿ�ʼ��
 */
public class MyParkingDataSmsHandler implements EventHandler<MyInParkingDataEvent>{
 
 
	public void onEvent(MyInParkingDataEvent myInParkingDataEvent, long sequence, boolean endOfBatch)
			throws Exception {
		long threadId = Thread.currentThread().getId(); // ��ȡ��ǰ�߳�id
		String carLicense = myInParkingDataEvent.getCarLicense(); // ��ȡ���ƺ�
		System.out.println(String.format("Thread Id %s ��  %s �ĳ�������һ�����ţ�����֪���Ʒѿ�ʼ�� ....", threadId, carLicense));
	}
 
}
 