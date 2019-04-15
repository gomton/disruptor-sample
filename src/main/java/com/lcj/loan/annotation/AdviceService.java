package com.lcj.loan.annotation;
 
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

 
 
@Service
public class AdviceService implements ApplicationContextAware{
 
	private ConcurrentHashMap<String, AtomicBoolean> flagMap = new ConcurrentHashMap<String, AtomicBoolean>();
 
	private ConcurrentHashMap<String, AtomicInteger> errorMap = new ConcurrentHashMap<String, AtomicInteger>();
 
	@Resource
	private EventQueue eventQueue;
	
	private ApplicationContext applicationContext;
 
	@PostConstruct
	public void init() {
		Set<String> set = applicationContext.getBeansOfType(EventHandler.class).keySet();
		for (String eventType : set) {
			flagMap.put(eventType, new AtomicBoolean(true));
			errorMap.put(eventType, new AtomicInteger(0));
		}
	}
 
	public void processEvent(String eventType, String... args) {
		boolean flag = flagMap.get(eventType).get();
		Long num = Long.MAX_VALUE;
		if (flag) {
			try {
				num = eventQueue.lpush(eventType, args);
//				logger.doInfo(eventType + "发布事件：" + args, num);
				if (num > 1024 * 2) {
					flagMap.get(eventType).set(false);
					// TODO 微信报警 
					System.out.println(eventType + "缓存队列已满！");
				}
			} catch (Exception e) {
				System.out.println(  e.getMessage());
				// TODO 任务进入NOSQL
				if (errorMap.get(eventType).addAndGet(1) > 30) {
					errorMap.get(eventType).set(0);
					// TODO 微信报警
					System.out.println(eventType + "发布事件错误次数超标！");
				}
			}
 
		} else {
			num = eventQueue.llen(eventType);
			if (num < 1024) {
				flagMap.get(eventType).set(true);
			}
			//	logger.doInfo(eventType + "缓存池已满，进入临时替补队列！", null);
			// TODO 任务进入NOSQL
		}
 
	}
	
 
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		
	}
 
	public static void main(String[] args) throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "applicationContext.xml" });
		context.start();
		final AdviceService service = context.getBean(AdviceService.class);
		EventQueue queue = context.getBean(EventQueue.class);
		queue.del("payplan_processing");
		queue.del("withhold_processing");
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < 1003; i++) {
					// service.processEvent("withhold",
					// UUID.randomUUID().toString());
					service.processEvent("withhold", i + "");
					if (i % 1000 == 0) {
						try {
							Thread.sleep(Math.abs(new Random().nextInt(100)));
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}.start();
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < 997; i++) {
					// service.processEvent("payplan",
					// UUID.randomUUID().toString());
					service.processEvent("payplan", i + "");
					if (i % 1000 == 0) {
						try {
							Thread.sleep(Math.abs(new Random().nextInt(100)));
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
 
			}
		}.start();
		new MyThread(queue).start();
 
		// context.close();
	}
 
}
 
class MyThread extends Thread {
	EventQueue queue;
 
	public MyThread(EventQueue queue) {
		this.queue = queue;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(200l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("payplan:" + queue.llen("payplan"));
			System.out.println("payplan_processing:"+ queue.llen("payplan_processing"));
			System.out.println("withhold:" + queue.llen("withhold"));
			System.out.println("withhold_processing:"+ queue.llen("withhold_processing"));
		}
	}
}
