package com.lcj.loan.disruptor;

import com.lmax.disruptor.EventFactory;

public class DefaultEventFactory implements EventFactory<Event>{
 
	public Event newInstance() {
		return new Event();
	}
}