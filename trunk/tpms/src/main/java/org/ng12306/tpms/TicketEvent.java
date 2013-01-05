package org.ng12306.tpms;

import java.io.Serializable;

// Ĭ�ϴ���ʹ��ObjectOutputStream�������󱣴浽�ļ���
// ��Ȼ���л���ҪһЩ���㣬�����io���ٶ���˵��Ӧ���Ǻܿ���
// �����ʱ����Serializable�ӿ�������
public class TicketEvent implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8576176057379004677L;
	private TicketEventType _type;
    public TicketEventType getType() { return _type; }

    // ��disruptor����������к�
    // ��Ϊ�����첽���ͷ�����Ϣ�������Ҫ��һ��Ψһ�ı�ʶ
    // ��ƥ����Ӧ��Ϣ��ԭ����������Ϣ��������Ӧ���㹻��
    // ����������������Ӧ��Ϣ�����ת��һȦ��������Ϣ
    // ��ƥ���������������ǵĻ�������ÿ��������
    public long sequence = -1;

    public TicketEvent(TicketEventType type) {
	_type = type;
    }
}
