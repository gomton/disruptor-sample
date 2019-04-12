package com.lcj.muti;

import com.lmax.disruptor.RingBuffer;

public class Producer {
	private final RingBuffer<Order> ringBuffer;

	public Producer(RingBuffer<Order> ringBuffer) {
		this.ringBuffer = ringBuffer;
	}

	/**
	 * onData���������¼���ÿ����һ�ξͷ���һ���¼� ���Ĳ������ù��¼����ݸ�������
	 */
	public void onData(String data) {
		// ���԰�ringBuffer����һ���¼����У���ônext���ǵõ�����һ���¼���
		long sequence = ringBuffer.next();
		try {
			// �����������ȡ��һ���յ��¼�������䣨��ȡ����Ŷ�Ӧ���¼�����
			Order order = ringBuffer.get(sequence);
			// ��ȡҪͨ���¼����ݵ�ҵ������
			order.setId(data);
		} finally {
			// �����¼�
			// ע�⣬���� ringBuffer.publish ������������� finally ����ȷ������õ����ã����ĳ������� sequence
			// δ���ύ��������������ķ����������������� producer��
			ringBuffer.publish(sequence);
		}
	}
}
