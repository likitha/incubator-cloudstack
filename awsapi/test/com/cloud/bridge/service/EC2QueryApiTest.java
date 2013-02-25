package com.cloud.bridge.service;


import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;

import java.io.OutputStream;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.bridge.persist.dao.CloudStackUserDaoImpl;
import com.cloud.bridge.service.controller.s3.ServiceProvider;
import com.cloud.bridge.service.core.ec2.EC2Engine;
import com.cloud.bridge.service.core.ec2.EC2Volume;
import com.cloud.stack.CloudStackApi;
import com.cloud.stack.models.CloudStackVolume;
import com.cloud.bridge.util.EC2RestAuth;

public class EC2QueryApiTest extends TestCase {
    @Spy EC2RestServlet restServlet = new EC2RestServlet();
    @Mock EC2Engine ec2Engine;
    @Mock EC2RestAuth restAuth;
    @Mock ServiceProvider serviceProvider;
    @Mock CloudStackUserDaoImpl userDao;
    @Mock CloudStackApi cloudStackApi;
    @Mock CloudStackVolume cloudStackVolume;

    private static String TEST_WSDL_VERSION = "2010-11-15";
    private static String TEST_AWS_ACCESS_KEY_ID = "x";
    private static String TEST_SIGNATURE = "x";
    private static String TEST_SIGNATURE_METHOD = "HmacSHA256";
    private static String TEST_VERSION = "2010-11-15";
    private static String TEST_SIGNATURE_VERSION = "2";
    private static String TEST_TIMESTAMP = "x";

    private static String TEST_VOLUME_ID = "1";
    private static String TEST_VOLUME_INSTANCE_ID = "1";
    private static String TEST_VOLUME_DEVICE = "/dev/sdb";
    private static String TEST_VOLUME_STATE = "VolumeState";

    @Before
    public void setup() {
        // TO-Do - pre-setup here
    }

    // attachVolume
    @Test
    public void testAttachVolume() throws Exception {
        System.out.println("Starting testAttachVolume");

        MockitoAnnotations.initMocks(this);

        restServlet.userDao = userDao;
        ReflectionTestUtils.setField(restServlet, "wsdlVersion", TEST_WSDL_VERSION);
        restServlet.restAuth = restAuth;
        restServlet.serviceProvider = serviceProvider;

        when(userDao.getSecretKeyByAccessKey(anyString())).thenReturn("secretKey");
        when(restAuth.verifySignature(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(serviceProvider.getEC2Engine()).thenReturn(ec2Engine);

        // test the real method attachVolume()
        when(ec2Engine.attachVolume(any(EC2Volume.class))).thenCallRealMethod();

        // Mock CloudStack:getApi to return a corresponding mock CloudStack object
        when(ec2Engine.getApi()).thenReturn(cloudStackApi);
        when(cloudStackApi.attachVolume(anyString(), anyString(), anyString())).thenReturn(cloudStackVolume);
        when(ec2Engine.mapToAmazonVolumeAttachmentState(anyString())).thenReturn(TEST_VOLUME_STATE);

        // VolumeId cannot be null and VirtualMachineId cannot be null 
        when(cloudStackVolume.getId()).thenReturn(TEST_VOLUME_ID);
        when(cloudStackVolume.getVirtualMachineId()).thenReturn(TEST_VOLUME_INSTANCE_ID);

        // when(xmlOutFactory.createXMLStreamWriter(any(OutputStream.class))).thenReturn(mock(XMLStreamWriter.class));
        doNothing().when(mock(XMLStreamWriter.class)).close();

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("Action","AttachVolume");
        req.setParameter("VolumeId",TEST_VOLUME_ID);
        req.setParameter("InstanceId",TEST_VOLUME_INSTANCE_ID);
        req.setParameter("Device",TEST_VOLUME_DEVICE);
        req.setParameter("AWSAccessKeyId",TEST_AWS_ACCESS_KEY_ID);
        req.setParameter("Signature",TEST_SIGNATURE);
        req.setParameter("SignatureMethod",TEST_SIGNATURE_METHOD);
        req.setParameter("Version",TEST_VERSION);
        req.setParameter("SignatureVersion",TEST_SIGNATURE_VERSION);
        req.setParameter("Timestamp",TEST_TIMESTAMP);
        MockHttpServletResponse res = new MockHttpServletResponse();
        restServlet.doGetOrPost(req,res);
        assertEquals(HttpServletResponse.SC_OK, res.getStatus());
    }

}
