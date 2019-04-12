package com.lcj.InParkingExample;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

/**
 * 
	Handler�ࣺһ������洢�������ݣ�һ��������kafka��Ϣ������ϵͳ�У����һ�����������������֪ͨ
 
 *
 */
public class MyParkingDataInDbHandler implements EventHandler<MyInParkingDataEvent>, WorkHandler<MyInParkingDataEvent> {

	public void onEvent(MyInParkingDataEvent myInParkingDataEvent) throws Exception {
		long threadId = Thread.currentThread().getId(); // ��ȡ��ǰ�߳�id
		String carLicense = myInParkingDataEvent.getCarLicense(); // ��ȡ���ƺ�
		System.out.println(String.format("Thread Id %s ���� %s �����ݿ��� ....", threadId, carLicense));
	}

	public void onEvent(MyInParkingDataEvent myInParkingDataEvent, long sequence, boolean endOfBatch) throws Exception {
		this.onEvent(myInParkingDataEvent);
	}

}
