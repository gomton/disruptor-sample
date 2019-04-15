package com.lcj.dependentevent;

import com.lmax.disruptor.EventHandler;

public class MyEventHandlerC implements EventHandler<MyEvent> {
	public void onEvent(MyEvent myEvent, long l, boolean b) throws Exception {
		System.out.println("Comsume Event C : " + myEvent.getValue()  + " " + Thread.currentThread().getId());
	}
}
