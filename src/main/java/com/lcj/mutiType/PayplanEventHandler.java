package com.lcj.mutiType;

public class PayplanEventHandler {
	public void onEvent(Event event){
		try{
			System.out.println("XXXXXXXXXXXXXXXXXXXXX" + 
					event.getKey() + "--" +Thread.currentThread().getId());
		}catch(Exception e){
			
		}		
	} 
}
