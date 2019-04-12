package com.lcj.ringBuffer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.YieldingWaitStrategy;

public class RingBufferExample {
	public static void main(String[] args) throws Exception {
		int BUFFER_SIZE = 1024;
		int THREAD_NUMBERS = 4;
		/*
		 * createSingleProducer����һ���������ߵ�RingBuffer��
		 * ��һ��������EventFactory����������������"�¼�����"����ʵ����ְ����ǲ����������RingBuffer�����顣
		 * �ڶ���������RingBuffer�Ĵ�С����������2��ָ���� Ŀ����Ϊ�˽���ģ����תΪ&�������Ч��
		 * ������������RingBuffer����������û�п��������ʱ��(�����������ߣ�����˵���¼��������� ̫����)�ĵȴ�����
		 */
		final RingBuffer<Trade> ringBuffer = RingBuffer.createSingleProducer(new EventFactory<Trade>() {
			 
			public Trade newInstance() {
				return new Trade();
			}
		}, BUFFER_SIZE, new YieldingWaitStrategy());

		// �����̳߳�
		ExecutorService executors = Executors.newFixedThreadPool(THREAD_NUMBERS);

		// ����SequenceBarrier
		SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

		/****************** @beg �������������� 2017-1-11 ******************/
		// ������Ϣ������
		BatchEventProcessor<Trade> transProcessor = new BatchEventProcessor<Trade>(ringBuffer, sequenceBarrier,
				new TradeHandler());

		// ��һ����Ŀ�ľ��ǰ������ߵ�λ����Ϣ����ע�뵽������ ���ֻ��һ�������ߵ��������ʡ��
		ringBuffer.addGatingSequences(transProcessor.getSequence());

		// ����Ϣ�������ύ���̳߳�
		executors.submit(transProcessor);
		/****************** @end �������������� 2017-1-11 ******************/

		// ������ڶ�������� ���ظ�ִ������3�д��� ��TradeHandler����������������

		/****************** @beg �������������� 2017-1-11 ******************/

		Future<?> future = executors.submit(new Callable<Void>() {
		 
			public Void call() throws Exception {
				long seq;
				for (int i = 0; i < 10; i++) {
					seq = ringBuffer.next();// ռ���� --ringBufferһ����������
					ringBuffer.get(seq).setPrice(Math.random() * 9999);// ������������ ����
					ringBuffer.publish(seq);// ����������������ʹhandler(consumer)�ɼ�
				}
				return null;
			}
		});

		/****************** @end �������������� 2017-1-11 ******************/

		future.get();// �ȴ������߽���
		Thread.sleep(2000);// ����1�룬�����Ѷ��������
		transProcessor.halt();// ֪ͨ�¼�(����˵��Ϣ)������ ���Խ����ˣ����������Ͻ���!!!��
		executors.shutdown();// ��ֹ�߳�
	}
}
