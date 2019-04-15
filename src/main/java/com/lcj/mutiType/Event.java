package com.lcj.mutiType;

public class Event {
	// 事件类型
	private String eventType;
	// 事件key
	private String key;
	// 事件value
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
}
