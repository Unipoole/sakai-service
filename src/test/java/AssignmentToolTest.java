import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.encoding.XMLType;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author mekay
 */
public class AssignmentToolTest {
    private static final Logger log = Logger.getLogger(AssignmentToolTest.class);

    public AssignmentToolTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //

    @Test
    @Ignore
    public void getAssignmentsForContextTest() throws ParseException, RemoteException, ServiceException {
        String sessionId = getSessionId();
        Service service = new Service();
        Call nc = null;
        String assignments;
        String context = "1959bfb1-8274-454f-b7d1-0244c184de54";

        SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
        Date d = f.parse("17-March-2014");
        String filterDate = String.valueOf(d.getTime());

        nc = (Call) service.createCall();

        nc.setTargetEndpointAddress("http://localhost:8080/sakai-axis/AssignmentTool.jws?wsdl");
        nc.removeAllParameters();
        nc.setOperationName("getAssignmentsForContext");
        nc.addParameter("sessionid", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("context", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("fromDate", XMLType.XSD_STRING, ParameterMode.IN);
        nc.setReturnType(XMLType.XSD_STRING);
        assignments = (String) nc.invoke(new Object[]{sessionId, context, filterDate});

        System.out.println(assignments);
    }

    @Test
    @Ignore
    public void testGetUserSubmissionsForContextAssignments() throws ServiceException, RemoteException, ParseException {
        String sessionId = getSessionId();
        Service service = new Service();
        Call nc = null;
        String submissions = "";
        String userId = "kabelo";
        String context = "1959bfb1-8274-454f-b7d1-0244c184de54";
        
        SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy");
        Date d = f.parse("12-December-2013");
        String filterDate = String.valueOf(d.getTime());

        nc = (Call) service.createCall();

        nc.setTargetEndpointAddress("http://localhost:8080/sakai-axis/AssignmentTool.jws?wsdl");
        nc.removeAllParameters();
        nc.setOperationName("getUserSubmissionsForContextAssignments");
        nc.addParameter("sessionid", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("userId", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("context", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("filterDate", XMLType.XSD_STRING, ParameterMode.IN);
        nc.setReturnType(XMLType.XSD_STRING);
        submissions = (String) nc.invoke(new Object[]{sessionId, userId, context, filterDate});

        log.info(submissions);
    }
        
    @Test
    @Ignore
    public void createSubmissionTest() throws ServiceException, ParseException, RemoteException {
        String sessionId = getSessionId();
        Service service = new Service();
        Call nc;
        String submissionEditId;
        String assignmentId = "d27d0007-adbd-42d4-aae9-4191b91b09de";
        String context = "1959bfb1-8274-454f-b7d1-0244c184de54";
        String userId = "kabelo";

        nc = (Call) service.createCall();

        nc.setTargetEndpointAddress("http://localhost:8080/sakai-axis/AssignmentTool.jws?wsdl");
        nc.removeAllParameters();
        nc.setOperationName("createSubmission");
        nc.addParameter("sessionid", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("context", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("assignmentId", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("userId", XMLType.XSD_STRING, ParameterMode.IN);
        nc.setReturnType(XMLType.XSD_STRING);
        submissionEditId = (String) nc.invoke(new Object[]{sessionId, context, assignmentId, userId});

        System.out.println(submissionEditId);
    }

    @Test
    @Ignore
    public void editAssignmentSubmissionTest() throws ServiceException, RemoteException, FileNotFoundException, IOException {
        String sessionId = getSessionId();
        String assignmentId = "2e7f3edc-bac4-4b29-921a-f87aecf189f0";
        String userId = "sagat";
        String context = "1959bfb1-8274-454f-b7d1-0244c184de54";
        String submittedText = "Submission (Multiple attachments from client with null attachment name) ";
        String[] attachmentNames = {"all rise", "maths formula", "math symbol"};
        String[] attachmentMimeTypes = {"image/jpeg", "image/jpeg", "image/jpeg"};
        String filePath1 = "/home/kabelo/Pictures/high_council_all_rise.jpg";
        String filePath2 = "/home/kabelo/Pictures/mathformula2.jpg";
        String filePath3 = "/home/kabelo/Pictures/Math_Symbol_Clipart.jpg";
        
        File file1 = new File(filePath1);
        byte[] c1 = new byte[(int) file1.length()];
        InputStream fis1 = new FileInputStream(file1);
        fis1.read(c1);
        fis1.close();
        
        File file2 = new File(filePath2);
        byte[] c2 = new byte[(int) file2.length()];
        InputStream fis2 = new FileInputStream(file2);
        fis2.read(c2);
        fis2.close();
        
        File file3 = new File(filePath3);
        byte[] c3 = new byte[(int) file3.length()];
        InputStream fis3 = new FileInputStream(file3);
        fis3.read(c3);
        fis3.close();

        byte[][] files = new byte[3][1];
        files[0] = c1;
        files[1] = c2;
        files[2] = c3;

        String response;
        Service service = new Service();
        Call nc;

        nc = (Call) service.createCall();
        nc.setTargetEndpointAddress("http://localhost:8080/sakai-axis/AssignmentTool.jws?wsdl");
        nc.removeAllParameters();
        nc.setOperationName("editAssignmentSubmission");
        nc.addParameter("sessionid", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("context", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("assignmentId", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("userId", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("submittedText", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("attachmentNames", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("attachmentMimeTypes", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("files", XMLType.XSD_BYTE, ParameterMode.IN);
        nc.setReturnType(XMLType.XSD_STRING);
        response = (String) nc.invoke(new Object[]{sessionId, context, assignmentId, userId, submittedText, attachmentNames, attachmentMimeTypes, files});

        System.out.println(response);
    }
    
    private String getSessionId() throws ServiceException, RemoteException {
        Service service = new Service();
        String id = "kabelo";
        String pw = "kabelo";
        Call nc = null;
        String sessionId = "";

        nc = (Call) service.createCall();
        nc.setTargetEndpointAddress("http://localhost:8080/sakai-axis/SakaiLogin.jws?wsdl");
        nc.removeAllParameters();
        nc.setOperationName("login");
        nc.addParameter("id", XMLType.XSD_STRING, ParameterMode.IN);
        nc.addParameter("pw", XMLType.XSD_STRING, ParameterMode.IN);
        nc.setReturnType(XMLType.XSD_STRING);
        sessionId = (String) nc.invoke(new Object[]{id, pw});

        return sessionId;
    }
}
