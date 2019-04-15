package com.lcj.mutiType;

import com.lmax.disruptor.EventFactory;

public class DefaultEventFactory implements EventFactory<Event>{
	public Event newInstance() {
		return new Event();
	}
}
