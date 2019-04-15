package com.lcj.loan.disruptor;

public class Event {
	// �¼�����
	private String eventType;
	// �¼�key
	private String key;
	// �¼�value
	private Object value;

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String type) {
		this.eventType = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Event [eventType=" + eventType + ", key=" + key + ", value=" + value + "]";
	}
}
