package com.lcj.loan.disruptor;

import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.lmax.disruptor.BlockingWaitStrategy;

import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

@Service
@Lazy(false)
public class DisruptorInitialization implements ApplicationContextAware{
	
	protected Logger logger = LoggerFactory.getLogger(DisruptorInitialization.class);
	
	@Resource
	private EventQueue eventQueue;
	
	private Map<String,EventHandler> eventHandlerMap;
	
	private Disruptor<Event> disruptor;
	
	private ApplicationContext applicationContext;
	
	private RingBuffer<Event> ringBuffer;
	
	private List<EventPublishThread> list = new ArrayList<EventPublishThread>();
 
	@PostConstruct
	public void init(){ 
		eventHandlerMap = applicationContext.getBeansOfType(EventHandler.class);
		 
		logger.info("获取事件处理机，个数为:{}", eventHandlerMap.size());
		disruptor = new Disruptor<Event>(new DefaultEventFactory(),1024,Executors.newFixedThreadPool(eventHandlerMap.size()), ProducerType.MULTI,new BlockingWaitStrategy());
		//异常处理
		disruptor.handleExceptionsWith(new ExceptionHandler(){
 
			public void handleEventException(Throwable ex, long sequence,
					Object event) {				
				logger.error(event.toString(),ex);
			 
			}
 
			public void handleOnStartException(Throwable ex) {
				logger.error("on start exception",ex);
			}
  
			public void handleOnShutdownException(Throwable ex) {
				logger.error("on shutdown exception",ex);
			}
		});
		//消费者
		@SuppressWarnings("unchecked")
		WorkHandler<Event>[] array = new WorkHandler[eventHandlerMap.size()];
		for(int i=0; i<eventHandlerMap.size();i++){
			array[i] = new WorkHandler<Event>(){
		 
				public void onEvent(Event event) throws Exception {
					String type = event.getEventType();
					eventHandlerMap.get(type).onEvent(event);
				}				
			};
		}
		disruptor.handleEventsWithWorkerPool(array);
		ringBuffer = disruptor.start();
		//生产者
		for(String eventType : eventHandlerMap.keySet()){
			EventPublishThread thread = new EventPublishThread(ringBuffer,eventQueue,eventType);
			thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(){
			 
				public void uncaughtException(Thread t, Throwable e) {
					logger.error(t.getName(),e);					
				}				
			});
			thread.start();
			logger.info("生产者线程启动！");
			list.add(thread);
		}
	}
	
	@PreDestroy
	public void destroy(){
		for(EventPublishThread thread:list){
			thread.setRunning(false);
		}
		if(disruptor != null){
			disruptor.shutdown();
		}
	}
 
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	private class EventPublishThread extends Thread{
 
		private RingBuffer<Event> ringBuffer;
		
		private EventQueue eventQueue;
		
		private String eventType;
		
		private boolean running = true;
		
		private EventPublishThread(RingBuffer<Event> ringBuffer,EventQueue eventQueue,String eventType){
			this.ringBuffer = ringBuffer;
			this.eventQueue = eventQueue;
			this.eventType = eventType;
		}
		
		@Override
		public void run() {
			LoadingCache<Long,AtomicInteger> counter =
					CacheBuilder.newBuilder()
					.expireAfterAccess(2,TimeUnit.MINUTES)
					.build(new CacheLoader<Long,AtomicInteger>(){
						@Override
						public AtomicInteger load(Long minute) throws Exception {
							return new AtomicInteger(0);
						}						
					});
			while(running){
				String failKey = null;
				try{
					final String key = eventQueue.rpop(eventType);
    				logger.info(eventType+"从队列中获取元素:{}", key);
					if(key != null){
						failKey = key;
						eventQueue.lpush(eventType+"_processing", key);
						ringBuffer.publishEvent(new EventTranslator<Event>(){
					 
							public void translateTo(Event event,long sequence) {
								event.setEventType(eventType);
								event.setKey(key);
							}							
						});
						logger.info(eventType+"向disruptor发布事件:{}", key);
						
					}else{
						//释放cpu，避免长期占用cpu，导致负载增加
						Thread.sleep(2000l);
					}
				}catch(Exception e){
					if(failKey != null){
						//TODO 记录到NO-SQL数据库中 判断是否需求重新消费
					}
					logger.error(eventType,e);
					//限流处理，防止日志雪崩
					long currentMinutes = System.currentTimeMillis()/1000/60;
					try{
						//TODO微信报警
						if(counter.get(currentMinutes).incrementAndGet() > 10){
							logger.error(eventType+"生产消息错误超标!");
							Thread.sleep(1000*60*5);
						}
					}catch(Exception ex){
						logger.error(eventType,ex);
					}
				}
			}			
		}
 
		public void setRunning(boolean running) {
			this.running = running;
		}
	}
	
	public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
		String s = "Sat%20Apr%2020%202019%2011:13:31%20GMT+0800%20(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4)";
		String aaa = URLDecoder.decode(s,"UTF-8");
		System.out.println(aaa);
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "applicationContext.xml" });
		context.start();
	}
}