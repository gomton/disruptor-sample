package com.lcj.loan.disruptor;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("payplan")
public class PayplanEventHandler extends AbstractEventHandler {
 
	private Logger logger = LoggerFactory.getLogger(PayplanEventHandler.class);
	
	private AtomicInteger count = new AtomicInteger(0);
	
	@Override
	void processEvent(Event event) {
		// TODO 根据事件key查询相关信息
		// TODO ES中重建索引 or 其他触发动作
		if(count.incrementAndGet()%1000 ==0){
			logger.info(event.getEventType()+"已消费"+count.get()+"个消息。", new Date());
		}
		if(count.get()>10000000){
			count.set(0);
		}
	}
}
