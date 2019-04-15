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
		// TODO �����¼�key��ѯ�����Ϣ
		// TODO ES���ؽ����� or ������������
		if(count.incrementAndGet()%1000 ==0){
			logger.info(event.getEventType()+"������"+count.get()+"����Ϣ��", new Date());
		}
		if(count.get()>10000000){
			count.set(0);
		}
	}
}
