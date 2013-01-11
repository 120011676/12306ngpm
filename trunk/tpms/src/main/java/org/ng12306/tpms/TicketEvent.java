package org.ng12306.tpms;

import java.io.Serializable;
import org.jboss.netty.channel.Channel;

// Ĭ�ϴ���ʹ��ObjectOutputStream�������󱣴浽�ļ���
// ��Ȼ���л���ҪһЩ���㣬�����io���ٶ���˵��Ӧ���Ǻܿ���
// �����ʱ����Serializable�ӿ�������
public class TicketEvent implements Serializable {
    private TicketEventType _type;
    public TicketEventType getType() { return _type; }

    public transient Channel channel;
    
    // ��disruptor����������к�
    // ��Ϊ�����첽���ͷ�����Ϣ�������Ҫ��һ��Ψһ�ı�ʶ
    // ��ƥ����Ӧ��Ϣ��ԭ����������Ϣ��������Ӧ���㹻��
    // ����������������ӦϢ�����ת��һȦ��������Ϣ
    // ��ƥ���������������ǵĻ�������ÿ��������
    public long sequence = -1;

    protected TicketEvent(TicketEventType type) {
	_type = type;
    }
}
