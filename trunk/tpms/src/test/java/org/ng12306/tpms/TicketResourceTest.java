package org.ng12306.tpms;

import org.junit.*;
import static org.junit.Assert.*;
import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;
import java.util.Date;

public class TicketResourceTest {
    private static SelectorThread threadSelector;

    private static Client c;

    @BeforeClass
    public static void setUp() throws Exception {
        threadSelector = Main.startServer();
	EventBus.start();
        c = Client.create();
    }

    @AfterClass
    public static void tearDown() throws Exception {
	EventBus.shutdown();
        threadSelector.stopEndpoint();
    }

	// ���Ը��ݳ��κŲ�ѯ����
    @Test
    public void testQueryTrainByTrainNo() {
        WebResource r = c.resource(Main.BASE_URI.toString() + "ticket/id/1");
	ClientResponse response = r.get(ClientResponse.class);
	assertEquals(200, response.getStatus());
	assertNotNull(response.getEntity(String.class));
    }

    // TODO: ��Ӹ���Ĳ���������
    @Test
    public void ���Թ�Ʊ���̨Disruptor������() throws Exception {
	WebResource r = c.resource(Main.BASE_URI.toString() + "ticket/id/G101");
	ClientResponse response = r.accept("application/json").get(ClientResponse.class);
	assertEquals(200, response.getStatus());
	Train[] results = response.getEntity(Train[].class);
	Train result = results[0];

	assertEquals("G101", result.name);
	assertEquals("������", result.departure);
	assertEquals("�Ϻ�����", result.termination);
	
	// һ�����εķ���ʱ��Ӧ��ֻ��ʱ�䣬û�����ڡ�
	assertEquals("07:00",
		     result.departureTime);
	assertEquals("12:23",
		     result.arrivalTime);

	// TODO: ����������������,��Ϊ��û�г��εľ�����λ����.
	// ��ҵ��������ķ������֮���������������������
	assertEquals(2, result.availables.length);
    }
}
