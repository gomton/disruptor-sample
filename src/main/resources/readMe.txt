---���ζ���ringbuffer
���ݻ���������ͬ�߳�֮�䴫�����ݵ�BUFFER��RingBuffer�Ǵ洢��Ϣ�ĵط���
ͨ��һ����Ϊcursor��Sequence����ָʾ���е�ͷ��
Э�������������RingBuffer�������Ϣ���������������߶��ж�RingBuffer�Ƿ�Ϊ�ա�
������ǣ���ʾ����β��Sequence��û����RingBuffer�У�������������ά����
�����ĺô��Ƕ�������ߴ�����Ϣ�ķ�ʽ������������һ��RingBuffer��ʵ����Ϣ�ĵ������ಥ����ˮ���Լ����ǵ���ϡ�
��RingBuffer��ά����һ����ΪgatingSequences��Sequence�������������Sequence��

---Producer/Consumer
Producer�������ߣ�������ͼ�е�P1. ֻ�Ƿ�ָ���� Disruptor �����¼�
(���ǰ�д�뻺����е�һ��Ԫ�ض���Ϊһ���¼�)���û����롣

---Consumer��EventProcessor��һ������µİ汾����EventProcessor���������Consumer��
������ʵ�ֲ��ԣ�һ����SingleThreadedStrategy�����̲߳��ԣ���һ���� MultiThreadedStrategy�����̲߳��ԣ���
���ֲ��Զ�Ӧ��ʵ����ΪSingleProducerSequencer��MultiProducerSequencer
����ʵ����Sequencer�֮࣬���Խ�Sequencer����Ϊ���Ƕ���ͨ��Sequence��ʵ������д��Sequence�ĸ���μ��ۡ� ��
���Ƕ����������ߺ�������֮����١���ȷ�ش������ݵĲ����㷨��
����ʹ���ĸ������Լ��ĳ���������[���̵߳Ĳ���ʹ����AtomicLong��Java�ṩ��CAS��������
�����̵߳�ʹ��long��û����Ҳû��CAS������ζ�ŵ��̰߳汾��ǳ��죬��Ϊ��ֻ��һ�������ߣ������������ϵĳ�ͻ]

Producer����event���ݣ�EventHandler��Ϊ����������event�������߼�����������Ϣ�Ľ���ͨ��Sequence�����ơ�

��Sequence
Sequence��һ����������ţ�˵���˾��Ǽ���������Σ�������Ҫ���̼߳乲������Sequence�����ô��ݣ��������̰߳�ȫ�ģ�
�ٴΣ�Sequence֧��CAS���������Ϊ�����Ч�ʣ�Sequenceͨ��padding������α�������ڽ��α��������⣬
���Բμ������½���ϸ�Ľ��ܡ�
ͨ��˳��������������Ź���ͨ������н��������ݣ��¼�����������(�¼�)�Ĵ����������������������������
һ�� Sequence ���ڸ��ٱ�ʶĳ���ض����¼�������( RingBuffer/Consumer )�Ĵ�����ȡ�
�����߶�RingBuffer�Ļ�����ʣ���������������֮���Э���Լ�������֮���Э��������ͨ��Sequenceʵ�֡�
����ÿһ����Ҫ�����������Sequence��

˵������Ȼһ�� AtomicLong Ҳ�������ڱ�ʶ���ȣ������� Sequence ����������⻹����һ��Ŀ�ģ�
�Ǿ��Ƿ�ֹ��ͬ�� Sequence ֮���CPU����α����(Flase Sharing)���⡣

��Sequence Barrier
���ڱ��ֶ�RingBuffer�� main published Sequence ��Consumer����������Consumer�� Sequence �����á� 
Sequence Barrier �������˾��� Consumer �Ƿ��пɴ�����¼����߼���
SequenceBarrier������������֮���Լ������ߺ�RingBuffer֮�佨��������ϵ��
��Disruptor�У�������ϵʵ����ָ����Sequence�Ĵ�С��ϵ��
������A������������Bָ����������A��Sequenceһ��ҪС�ڵ���������B��Sequence��
���ִ�С��ϵ�����˴���ĳ����Ϣ���Ⱥ�˳����Ϊ���������߶�������RingBuffer��
���������ߵ�Sequenceһ��С�ڵ���RingBuffer����Ϊcursor��Sequence��
����Ϣһ�����ȱ������߷ŵ�Ringbuffer�У�Ȼ����ܱ������ߴ����������Ļ������Կ������½���������˽⡣
SequenceBarrier�ڳ�ʼ����ʱ����ռ���Ҫ�����������Sequence��RingBuffer��cursor�ᱻ�Զ��ļ������С�
��Ҫ�������������ߺ�/��RingBuffer����������������һ����Ϣʱ�����ȵȴ���SequenceBarrier�ϣ�
ֱ�����б������������ߺ�RingBuffer��Sequence���ڵ�����������ߵ�Sequence��
���������������߻�RingBuffer��Sequence�б仯ʱ����֪ͨSequenceBarrier���ѵȴ���������������ߡ�

 
��Wait Strategy
�������ߵȴ���SequenceBarrier��ʱ��������ѡ�ĵȴ����ԣ���ͬ�ĵȴ��������ӳٺ�CPU��Դ��ռ����������ͬ��
������Ӧ�ó���ѡ��
BusySpinWaitStrategy �� �����ȴ�������Linux Kernelʹ�õ������������ӳٵ�ͬʱ��CPU��Դ��ռ��Ҳ�ࡣ
BlockingWaitStrategy �� ʹ����������������CPU��Դ��ռ���٣��ӳٴ�
SleepingWaitStrategy �� �ڶ��ѭ�����Բ��ɹ���ѡ���ó�CPU���ȴ��´ε��ȣ���ε��Ⱥ��Բ��ɹ���
����ǰ˯��һ�����뼶���ʱ���ٳ��ԡ����ֲ���ƽ�����ӳٺ�CPU��Դռ�ã����ӳٲ����ȡ�
YieldingWaitStrategy �� �ڶ��ѭ�����Բ��ɹ���ѡ���ó�CPU���ȴ��´ε���ƽ�����ӳٺ�CPU��Դռ�ã�
���ӳ�Ҳ�ȽϾ��ȡ�
PhasedBackoffWaitStrategy �� ������ֲ��Ե��ۺϣ�CPU��Դ��ռ���٣��ӳٴ�
---Event
�� Disruptor �������У������ߺ�������֮����н��������ݱ���Ϊ�¼�(Event)��
������һ���� Disruptor ������ض����ͣ������� Disruptor ��ʹ���߶��岢ָ����

---EventProcessor
EventProcessor �����ض�������(Consumer)�� Sequence�����ṩ���ڵ����¼�����ʵ�ֵ��¼�ѭ��(Event Loop)��
ͨ����EventProcessor�ύ���̳߳�������ִ�У�������Processor:
����һ����������BatchEvenProcessor��ÿ��BatchEvenProcessor��һ��Sequence��
����¼�Լ�����RingBuffer����Ϣ����������ԣ�һ����Ϣ��Ȼ�ᱻÿһ��BatchEvenProcessor���ѡ�

��һ����������WorkProcessor��ÿ��WorkProcessorҲ��һ��Sequence��
���WorkProcessor������һ��Sequence���ڻ���ķ���RingBuffer��
һ����Ϣ��һ��WorkProcessor���ѣ��Ͳ��ᱻ����һ��Sequence������WorkProcessor���ѡ�
�����WorkProcessor�����Sequence�൱��βָ��

----EventHandler
Disruptor ������¼�����ӿڣ����û�ʵ�֣����ڴ����¼����� Consumer ������ʵ�֡�������ʵ��EventHandler��
Ȼ����Ϊ��δ��ݸ�EventProcessor��ʵ����