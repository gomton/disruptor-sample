package com.lcj.loan.disruptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lcj.loan.annotation.EventQueue;

public abstract class AbstractEventHandler implements EventHandler {
	@Resource
	protected EventQueue eventQueue;
	
	private Logger logger = LoggerFactory.getLogger(AbstractEventHandler.class);
	
	LoadingCache<Long,AtomicInteger> counter =
			CacheBuilder.newBuilder()
			.expireAfterAccess(2,TimeUnit.MINUTES)
			.build(new CacheLoader<Long,AtomicInteger>(){
				@Override
				public AtomicInteger load(Long minute) throws Exception {
					return new AtomicInteger(0);
				}						
			});
 
 
	public void onEvent(Event event) {
		try{
			processEvent(event);
		}catch(Exception e){
			logger.error("on event error",e);
			long currentMinutes = System.currentTimeMillis()/1000/60;
			try{
				if(counter.get(currentMinutes).incrementAndGet() > 10){
					logger.error("消费事件错误次数超标！");
					//TODO 微信报警
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			fail(event,e);
		}
		try{
			success(event);
		}catch(Exception e){
			 
		}
	}
	
	abstract void processEvent(Event event);
	
	protected void success(Event event){
		long l = eventQueue.lrem(event.getEventType()+"_processing", 1, event.getKey());
//		logger.doInfo(event.getEventType()+"消息成功："+event.getKey(),null);
	}
	
	protected void fail(Event event,Exception e){
		throw new RuntimeException(e);
	}
}
