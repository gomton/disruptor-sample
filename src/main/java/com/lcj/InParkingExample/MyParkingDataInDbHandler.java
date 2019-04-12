package com.lcj.InParkingExample;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

/**
 * 
	Handler类：一个负责存储汽车数据，一个负责发送kafka信息到其他系统中，最后一个负责给车主发短信通知
 
 *
 */
public class MyParkingDataInDbHandler implements EventHandler<MyInParkingDataEvent>, WorkHandler<MyInParkingDataEvent> {

	public void onEvent(MyInParkingDataEvent myInParkingDataEvent) throws Exception {
		long threadId = Thread.currentThread().getId(); // 获取当前线程id
		String carLicense = myInParkingDataEvent.getCarLicense(); // 获取车牌号
		System.out.println(String.format("Thread Id %s 保存 %s 到数据库中 ....", threadId, carLicense));
	}

	public void onEvent(MyInParkingDataEvent myInParkingDataEvent, long sequence, boolean endOfBatch) throws Exception {
		this.onEvent(myInParkingDataEvent);
	}

}
