
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.axis.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.assessment.data.dao.grading.StudentGradingSummaryData;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentAccessControlIfc;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacade;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacadeQueries;
import org.sakaiproject.tool.assessment.services.GradingService;
import org.sakaiproject.tool.assessment.services.assessment.PublishedAssessmentService;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.assessment.facade.AssessmentGradingFacade;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Retrieve the Unisa Faq tool content. It is a simple tool with no API. So we
 * will be talking directly to the database. It does not have any user
 * permissions.
 *
 * @author OpenCollab
 * @since 1.0.0
 */
public class SamigoTool {

    private static final Log LOG = LogFactory.getLog(SamigoTool.class);
    private SessionManager sessionManager;
    private PublishedAssessmentService publishedAssessmentService = new PublishedAssessmentService();
    private GradingService gradingService = new GradingService();
    private SiteService siteService;
    private ToolManager toolManager;
    private SecurityService securityService;
    private UserDirectoryService userDirectoryService;

    public SamigoTool() {
        toolManager = (ToolManager) ComponentManager.get(ToolManager.class.getName());
        siteService = (SiteService) ComponentManager.get(SiteService.class.getName());
        sessionManager = (SessionManager) ComponentManager.get(SessionManager.class.getName());
        securityService = (SecurityService) ComponentManager.get(SecurityService.class.getName());
        userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class.getName());
    }

    /**
     * Get the Session related to the given sessionid
     *
     * @param sessionid	the id of the session to retrieve
     * @return	the session, if it is active
     * @throws AxisFault	if session is inactive
     */
    private Session establishSession(String sessionid) throws AxisFault {
        Session s = sessionManager.getSession(sessionid);

        if (s == null) {
            throw new AxisFault("Session \"" + sessionid + "\" is not active");
        }
        s.setActive();
        sessionManager.setCurrentSession(s);
        return s;
    }

    /**
     *
     * @param sessionId
     * @param username
     * @param siteId
     * @param fromDate
     * @param pageTitle the text name of the page e.g. Self Assessments, this
     * can change based on Unisa's requirements so it must be editable.
     * @return
     * @throws AxisFault
     */
    public String getAvailableAssignments(String sessionId, String username, String siteId, String fromDate, String pageTitle) throws AxisFault {
        establishSession(sessionId);
        String studentId = username;
        String userId = getSakaiUserId(sessionId, username, siteId);
        Document dom = Xml.createDocument();
        Element listNode = dom.createElement("list");
        dom.appendChild(listNode);

        // ========================
        // Samigo internal sakai ID
        // ========================
        Element testUrl = dom.createElement("assessment_tool_url");
        listNode.appendChild(testUrl);
        testUrl.setTextContent(getSamigoToolId(sessionId, siteId, pageTitle));

        // ========================
        // Submitted/scored assesments
        // ========================
        //get the submittted assessments as the getTakableList is not accurate.
        Element submittedAssessments = dom.createElement("scored");
        listNode.appendChild(submittedAssessments);

        Map<Long, AssessmentGradingFacade> submittedAssesmentsMap = new HashMap<Long, AssessmentGradingFacade>();
        ArrayList submittedAssessmentsArray = publishedAssessmentService.getBasicInfoOfLastSubmittedAssessments(userId, null, true);
        ArrayList allSubmittedAssessmentsArray = publishedAssessmentService.getBasicInfoOfLastOrHighestOrAverageSubmittedAssessmentsByScoringOption(userId, siteId, true);

        for (Object obj : submittedAssessmentsArray) {
            AssessmentGradingFacade agf = (AssessmentGradingFacade) obj;
            publishedAssessmentService.getBasicInfoOfLastOrHighestOrAverageSubmittedAssessmentsByScoringOption(userId, siteId, true);
            submittedAssesmentsMap.put(agf.getPublishedAssessmentId(), agf);
        }

        // ========================
        // Available assesments
        // ========================
        HashMap h = publishedAssessmentService.getTotalSubmissionPerAssessment(studentId, siteId);

        ArrayList publishedAssessmentList = publishedAssessmentService.getBasicInfoOfAllPublishedAssessments(
                studentId, PublishedAssessmentFacadeQueries.TITLE, true, siteId);

        List list = gradingService.getUpdatedAssessmentList(studentId, siteId);
        List updatedAssessmentNeedResubmitList = new ArrayList();
        List updatedAssessmentList = new ArrayList();
        if (list != null && list.size() == 2) {
            updatedAssessmentNeedResubmitList = (List) list.get(0);
            updatedAssessmentList = (List) list.get(1);
        }

        Element availableAssessments = dom.createElement("available_assesments");
        listNode.appendChild(availableAssessments);
        // filter out the one that the given user do not have right to access
        ArrayList<PublishedAssessmentFacade> takeableList = getTakeableList(publishedAssessmentList, h, updatedAssessmentNeedResubmitList, updatedAssessmentList, studentId, siteId);
        if (publishedAssessmentList != null) {
            Date filterDate = null;
            Date assesmentsDate = null;
            if (fromDate != null && fromDate.trim().length() != 0) {
                filterDate = new Date(Long.parseLong(fromDate));
            }
            for (PublishedAssessmentFacade assessments : takeableList) {
                if (assessments != null) {
                    assesmentsDate = assessments.getLastModifiedDate();
                    //check if we want this message
                    // Available assignments
                    boolean returnAll = (filterDate == null) ? true : false;
                    System.out.println("filterDate : " + filterDate + " | assesmentsDate : " + assesmentsDate);
                    if (returnAll || assesmentsDate.after(filterDate)) {
                        //exclude submitted assignments
                        if (!submittedAssesmentsMap.containsKey(assessments.getPublishedAssessmentId())) {
                            Element assessmentElement = dom.createElement("assignment");
                            assessmentElement.setAttribute("title", assessments.getTitle());
                            assessmentElement.setAttribute("time-limit", assessments.getTimeLimit() == null || assessments.getTimeLimit() == 0 ? null : assessments.getTimeLimit().toString());
                            assessmentElement.setAttribute("due-date", assessments.getDueDate() == null ? null : Long.toString(assessments.getDueDate().getTime()));
                            assessmentElement.setAttribute("id", (assessments.getPublishedAssessmentId() == null ? "null" : assessments.getPublishedAssessmentId().toString()));
                            availableAssessments.appendChild(assessmentElement);
                        }
                    }
                    // Submitted and or scored assignments
                    AssessmentGradingFacade agf = submittedAssesmentsMap.get(assessments.getPublishedAssessmentId());
                    if (null != agf) {
                        if (returnAll || agf.getSubmittedDate().after(filterDate)) {
                            //exclude submitted assignments
                            Element assessmentElement = dom.createElement("assignment");
                            assessmentElement.setAttribute("title", assessments.getTitle());
                            assessmentElement.setAttribute("id", agf.getPublishedAssessmentId().toString());
                            assessmentElement.setAttribute("stats", String.valueOf(agf.getStatus()));
                            assessmentElement.setAttribute("recorded_score", String.valueOf(agf.getFinalScore()));
                            assessmentElement.setAttribute("feedback_available", String.valueOf(agf.getComments()));
                            assessmentElement.setAttribute("scores", "");
                            
                            List<AssessmentGradingFacade> al = new ArrayList<AssessmentGradingFacade>();
                            for (Object attemptObj : allSubmittedAssessmentsArray) {
                                AssessmentGradingFacade att = (AssessmentGradingFacade) attemptObj;
                                if (agf.getPublishedAssessmentId().intValue() == att.getPublishedAssessmentId().intValue()) {
                                    al.add(att);
                                }
                            }
                            if (!al.isEmpty() && al.size() > 0) {
                                Element scores = dom.createElement("scores");
                                for (AssessmentGradingFacade attt : al) {
                                    Element score = dom.createElement("score");
                                    score.setAttribute("individual_score", String.valueOf(attt.getFinalScore()));
                                    score.setAttribute("time", String.valueOf(attt.getTimeElapsed()));
                                    score.setAttribute("submitted", String.valueOf(attt.getSubmittedDate()));
                                    scores.appendChild(score);
                                }
                                assessmentElement.appendChild(scores);
                            }
                            submittedAssessments.appendChild(assessmentElement);
                        }
                    }
                }
            }
        }
        return Xml.writeDocumentToString(dom);
    }

    private ArrayList getTakeableList(ArrayList assessmentList, HashMap h, List updatedAssessmentNeedResubmitList, List updatedAssessmentList, String studentId, String moduleId) {
        ArrayList takeableList = new ArrayList();

        HashMap numberRetakeHash = gradingService.getNumberRetakeHash(studentId);
        HashMap actualNumberRetake = gradingService.getActualNumberRetakeHash(moduleId);
        for (int i = 0; i < assessmentList.size(); i++) {
            PublishedAssessmentFacade f = (PublishedAssessmentFacade) assessmentList.get(i);
            if (f.getReleaseTo() != null && !("").equals(f.getReleaseTo())
                    && f.getReleaseTo().indexOf("Anonymous Users") == -1) {
                if (isAvailable(f, h, numberRetakeHash, actualNumberRetake, updatedAssessmentNeedResubmitList, updatedAssessmentList)) {
                    takeableList.add(f);
                }
            }
        }
        return takeableList;
    }

    private boolean isAvailable(PublishedAssessmentFacade f, HashMap h, HashMap numberRetakeHash, HashMap actualNumberRetakeHash, List updatedAssessmentNeedResubmitList, List updatedAssessmentList) {
        boolean returnValue = false;
        //1. prepare our significant parameters
        Integer status = f.getStatus();
        Date currentDate = new Date();
        Date startDate = f.getStartDate();
        Date retractDate = f.getRetractDate();
        Date dueDate = f.getDueDate();

        if (!Integer.valueOf(1).equals(status)) {
            return false;
        }

        if (startDate != null && startDate.after(currentDate)) {
            return false;
        }

        if (retractDate != null && retractDate.before(currentDate)) {
            return false;
        }

        if (updatedAssessmentNeedResubmitList.contains(f.getPublishedAssessmentId()) || updatedAssessmentList.contains(f.getPublishedAssessmentId())) {
            return true;
        }

        boolean acceptLateSubmission = AssessmentAccessControlIfc.ACCEPT_LATE_SUBMISSION.equals(f.getLateHandling());
        int maxSubmissionsAllowed = 9999;
        if ((Boolean.FALSE).equals(f.getUnlimitedSubmissions())) {
            maxSubmissionsAllowed = f.getSubmissionsAllowed().intValue();
        }

        int numberRetake = 0;
        if (numberRetakeHash.get(f.getPublishedAssessmentId()) != null) {
            numberRetake = (((StudentGradingSummaryData) numberRetakeHash.get(f.getPublishedAssessmentId())).getNumberRetake()).intValue();
        }
        int totalSubmitted = 0;

        //boolean notSubmitted = false;
        if (h.get(f.getPublishedAssessmentId()) != null) {
            totalSubmitted = ((Integer) h.get(f.getPublishedAssessmentId())).intValue();
        }
        if (retractDate == null || retractDate.after(currentDate)) {
            if (startDate == null || startDate.before(currentDate)) {
                if (dueDate != null && dueDate.before(currentDate)) {
                    if (acceptLateSubmission) {
                        if (totalSubmitted == 0) {
                            return true;
                        }
                    }
                    int actualNumberRetake = 0;
                    if (actualNumberRetakeHash.get(f.getPublishedAssessmentId()) != null) {
                        actualNumberRetake = ((Integer) actualNumberRetakeHash.get(f.getPublishedAssessmentId())).intValue();
                    }
                    if (actualNumberRetake < numberRetake) {
                        returnValue = true;
                    }
                } else {
                    if (totalSubmitted < maxSubmissionsAllowed + numberRetake) {
                        returnValue = true;
                    }
                }
            }
        } else {
            if (totalSubmitted < maxSubmissionsAllowed + numberRetake) {
                returnValue = true;
            }
        }
        return returnValue;
    }

    private Integer getSubmissionAllowed(Long publishedAssessmentId, HashMap publishedAssessmentHash) {
        PublishedAssessmentFacade p = (PublishedAssessmentFacade) publishedAssessmentHash.
                get(publishedAssessmentId);
        if (p != null) {
            return p.getSubmissionsAllowed();
        } else {
            return Integer.valueOf(-1);
        }
    }

    //#######################################################################################
    public String getSamigoToolId(String sessionid, String siteid, String pageTitle) throws AxisFault {
        Session session = establishSession(sessionid);
        String tooldata = "";
        //check if site exists
        Site site;
        try {
            site = siteService.getSite(siteid);
        } catch (Exception e) {
            LOG.warn("Error looking up site: " + siteid, e);
            throw new AxisFault("Error looking up site: " + siteid, e);
        }

        String userId = session.getUserId();

        //check if super user
        boolean isSuperUser = false;
        if (securityService.isSuperUser(userId)) {
            isSuperUser = true;
        }

        //if not super user, check user is a member of the site, and get their Role
        Role role;
        if (!isSuperUser) {
            Member member = site.getMember(userId);
            if (member == null || !member.isActive()) {
                LOG.warn("User: " + userId + " does not exist in site : " + siteid);
                throw new AxisFault("User: " + userId + " does not exist in site : " + siteid);
            }
            role = member.getRole();
        }



        //get list of pages in the site, if none, return empty list
        List<SitePage> pages = site.getPages();
        if (pages.isEmpty()) {
            return "<site id=\"" + site.getId() + "\"><site>";
        }

        //site node
        //pages node
        for (SitePage page : pages) {
            //page node
            //pageTitle
            if (StringUtils.trim(page.getTitle()).equals(StringUtils.trim(pageTitle))) {
                //get tools in page
                List<ToolConfiguration> tools = page.getTools();
                boolean includePage = true;
                for (ToolConfiguration toolConfig : tools) {
                    //if we not a superAdmin, check the page properties
                    //if any tool on this page is hidden, skip the rest of the tools and exclude this page from the output
                    //this makes the behaviour consistent with the portal

                    //if not superUser, process  tool function requirements
                    if (!isSuperUser) {

                        //skip processing tool if we've skipped tools previously on this page
                        if (!includePage) {
                            continue;
                        }

                        //skip this tool if not visible, ultimately hiding the whole page
                        if (!toolManager.isVisible(site, toolConfig)) {
                            includePage = false;
                            break;
                        }
                    }

                    //if we got this far, add the details about the tool to the document
                    //tool uuid
                    tooldata = toolConfig.getId();
                }
            }
        }
        return tooldata;
    }

    public String getSakaiUserId(String sessionId, String username, String siteId) throws AxisFault {
        establishSession(sessionId);
        User user;
        try {
            user = userDirectoryService.getUserByEid(username);
        } catch (UserNotDefinedException e) {
            LOG.error("WS getUserDetails() failed for user: " + username, e);
            throw new AxisFault("Could not find user: " + username);
        }
        return user.getId();
    }
}