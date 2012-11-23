package org.ng12306.tpms;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

@XmlRootElement
public class Train {
    // ���κ�
    public String name;

    // ʼ��վ
    public String departure;

    // ����ʱ��
    public String departureTime;

    // �յ�վ
    public String termination;

    // ����ʱ��
    public String arrivalTime;

    // ��Ʊ��Ϣ
	@XmlElement
    public String[][] availables;
}
