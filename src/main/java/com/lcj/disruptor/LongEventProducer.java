package com.lcj.disruptor;

import java.nio.ByteBuffer;

import com.lmax.disruptor.RingBuffer;

public class LongEventProducer {
	private final RingBuffer<LongEvent> ringBuffer;

	public LongEventProducer(RingBuffer<LongEvent> ringBuffer) {
		this.ringBuffer = ringBuffer;
	}

	public void onData(ByteBuffer bb) {
		 //1.���԰�ringBuffer����һ���¼����У���ônext���ǵõ�����һ���¼���
		long sequence = ringBuffer.next();
		try {
			// 2.�����������ȡ��һ���յ��¼�������䣨��ȡ����Ŷ�Ӧ���¼�����
			LongEvent event = ringBuffer.get(sequence);
			// 3.��ȡҪͨ���¼����ݵ�ҵ������
			event.setValue(bb.getLong(0));
		} finally {
			// 4.�����¼�
			// ע�⣬���� ringBuffer.publish ������������� finally ����ȷ������õ����ã����ĳ������� sequence
			// δ���ύ��������������ķ����������������� producer��
			ringBuffer.publish(sequence);
		}
	}
}
