package com.lcj.demo1;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.YieldingWaitStrategy;

public class Demo1 {
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		final int BUFFER_SIZE = 1024;
		final int THREAD_NUMBERS = 2;
		/*
		 * createSingleProducer����һ���������ߵ�RingBuffer
		 * ��һ��������EventFactory,��������������"�¼�����"����ʵ����ְ����ǲ����������RingBuffer������
		 * �ڶ�������RingBuffer�Ĵ�С����������2��ָ������Ŀ����Ϊ�˽���������תΪ&�������Ч��
		 * ������������RingBuffer����������û�п������飨slot����ʱ�򣨿�����������(����˵���¼�������)̫���ˣ��ĵȴ�����
		 */
		final RingBuffer<TradeTransaction> ringBuffer = RingBuffer
				.createSingleProducer(new EventFactory<TradeTransaction>() {
					
					public TradeTransaction newInstance() {
						return new TradeTransaction();
					}
				}, BUFFER_SIZE, new YieldingWaitStrategy());

		// �����̳߳�
		ExecutorService executors = Executors.newFixedThreadPool(THREAD_NUMBERS);

		// ����SequenceBarrier ���������
		SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

		// ������Ϣ������
		BatchEventProcessor<TradeTransaction> transProcessor = new BatchEventProcessor<TradeTransaction>(ringBuffer,
				sequenceBarrier, new TradeTransactionInDBHandler());

		// ringBuffer����֪�������ߵ�״̬
		ringBuffer.addGatingSequences(transProcessor.getSequence());

		executors.submit(transProcessor);

		Future<?> future = executors.submit(new Callable<Void>() {

			public Void call() throws Exception {
				long seq;
				for (int i = 0; i < 1000; i++) {
					seq = ringBuffer.next();
					ringBuffer.get(seq).setPrice(Math.random() * 9999);
					ringBuffer.publish(seq);// �������slot������ʹhandler(consumer)�ɼ�
				}
				return null;
			}

		});
		//future.get();
		System.out.println("future:"+future.get());
		Thread.sleep(10000);
		transProcessor.halt();
		executors.shutdown();
	}

}
