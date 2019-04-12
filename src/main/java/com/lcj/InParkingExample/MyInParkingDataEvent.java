package com.lcj.InParkingExample;
/** 
 代码包含以下内容：
1） 事件对象Event
2）三个消费者Handler
3）一个生产者Processer
4）执行Main方法
Event类：汽车信息
*/
public class MyInParkingDataEvent {
	private String carLicense; // 车牌号
	 
	public String getCarLicense() {
		return carLicense;
	}
 
	public void setCarLicense(String carLicense) {
		this.carLicense = carLicense;
	}

}
