package com.lcj.loan.annotation;

import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class RecordAspect implements ApplicationContextAware {
 
	private ApplicationContext applicationContext;
	private Map<String, EventHandler> eventHandlerMap;
 
	@Autowired
	private AdviceService adviceService;

	@Pointcut("@annotation(com.lcj.loan.annotation.Action)")
	public void annotationPointCut() {
	}

	/**
	 * * 监控的dao方法，有可能因为事物回滚，导致输出冗余日志 * @param joinPoint
	 */
	@After("annotationPointCut()")
	public void after(JoinPoint joinPoint) {
		try {
			record(joinPoint);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@PostConstruct
	public void init() {
		eventHandlerMap = applicationContext.getBeansOfType(EventHandler.class);
	}

	public void record(JoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Action action = method.getAnnotation(Action.class);
		EventHandler handler = eventHandlerMap.get(action.eventType());
		String key = handler.getKey(action.type(), joinPoint.getArgs());
		adviceService.processEvent(action.eventType(), key);
	}

	public void recordLog(JoinPoint joinPoint) {
		StringBuilder builder = new StringBuilder();
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Action action = method.getAnnotation(Action.class);
		if (action.type() == 1) {
			//TXdAfterPayPlanDto dto = (TXdAfterPayPlanDto) joinPoint.getArgs()[0];
			//if (dto != null) {
		//		builder.append(dto.getLoanNumber() + " KKKK|| ");
			//}
		}
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		builder.append(signature.getDeclaringTypeName() + "." + method.getName());
		for (StackTraceElement e : stackTrace) {
			if (e.getClassName().startsWith("com.lcj.loan") && !e.getClassName().contains("$$")
					&& !e.getClassName().contains("annotation")) {
				builder.append(" | ");
				builder.append(e.getClassName() + "." + e.getMethodName());
			}
		}
		//asyncRecordService.asyncRecord(builder.toString());
	}

	 public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
