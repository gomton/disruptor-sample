package com.lcj.mutiType;

import com.lmax.disruptor.WorkHandler;

public class FbWorkHandler implements WorkHandler<Event> {
	public void onEvent(Event event) throws Exception {
		String type = event.getEventType();
		PayplanEventHandler handler = new PayplanEventHandler();
		handler.onEvent(event);
	}
 
}
