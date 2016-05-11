import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.axis.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.AssignmentSubmissionEdit;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * AssignmentTool.jws
 *
 * The web service dedicated to retrieving Assignments
 *
 */
public class AssignmentTool {

    private final EntityManager entityManager;
    private final TimeService timeService;
    private final SessionManager sessionManager;
    private final AssignmentService assignmentService;
    private final UserDirectoryService userDirectoryService;
    private final ContentHostingService contentHostingService;
    private static final String ASSIGNMENT_TOOL_ID = "sakai.assignment.grades";
    private static final String TOOL_NAME = "Assignments";
    static final Log LOG = LogFactory.getLog(AssignmentTool.class);

    public AssignmentTool() {
        timeService = (TimeService) ComponentManager.get(TimeService.class.getName());
        entityManager = (EntityManager) ComponentManager.get(EntityManager.class.getName());
        sessionManager = (SessionManager) ComponentManager.get(SessionManager.class.getName());
        assignmentService = (AssignmentService) ComponentManager.get(AssignmentService.class.getName());
        userDirectoryService = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class.getName());
        contentHostingService = (ContentHostingService) ComponentManager.get(ContentHostingService.class.getName());
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
     * ======================= Start Data Retrieval Section
     * ========================
     */
    public String getAssignmentsForContext(String sessionId, String context, String fromDate) throws AxisFault {
        if (context.isEmpty() || sessionId.isEmpty()) {
            return "<assignments/>";
        }
        establishSession(sessionId);

        Document dom = Xml.createDocument();
        Element assignments = (Element) dom.createElement("assignments");
        dom.appendChild(assignments);

        Date filterDate = null;
        if (fromDate != null && fromDate.trim().length() != 0) {
            filterDate = new Date(Long.parseLong(fromDate));
        }

        try {
            List<Assignment> assignmentsList = assignmentService.getListAssignmentsForContext(context);

            if (assignmentsList != null) {
                for (Assignment assignment : assignmentsList) {
                    long time = assignment.getDueTime().getTime();
                    Date dueDate = new Date(time);
                    /* check if we want this assignment */
                    if (filterDate != null && filterDate.after(dueDate)) {
                        continue;
                    }

                    Element assignmentEl = dom.createElement("assignment");

                    buildAssignmentElement(assignmentEl, assignment);

                    buildAttachmentElement(assignmentEl, dom, assignment.getContent().getAttachments());

                    assignments.appendChild(assignmentEl);
                }
            }
            return Xml.writeDocumentToString(dom);

        } catch (Exception e) {
            LOG.error("Could not retrieve assignments.", e);
            throw new AxisFault("Could not retrieve assignments.", e);
        }
    }

    public String getUserSubmissionsForContextAssignments(String sessionId, String userId, String context, String fromDate) throws AxisFault, UserNotDefinedException {
        if (context.isEmpty() || sessionId.isEmpty() || userId.isEmpty()) {
            return "<submissions/>";
        }
        establishSession(sessionId);

        Document dom = Xml.createDocument();
        Element submissions = (Element) dom.createElement("submissions");
        dom.appendChild(submissions);

        List<String> assignmentIds = getAssignmentIdsForContext(context, fromDate);

        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return "<submissions/>";
        }

        for (String assignmentId : assignmentIds) {
            Element submission = getUserSubmissionForAssignment(userId, assignmentId, dom);
            if (submission != null) {
                submissions.appendChild(submission);
            }
        }

        return Xml.writeDocumentToString(dom);
    }

    public String editAssignmentSubmission(String sessionId, String context, String assignmentId, String userId, String submittedText, String[] attachmentNames, String[] attachmentMimeTypes, byte[][] content) throws AxisFault {
        if (context.isEmpty() || sessionId.isEmpty() || userId.isEmpty()) {
            return null;
        }
        // ArrayIndexOutOfBoundsException will occur if program is allowed to continue without all arrays being of equal length
        if (attachmentMimeTypes.length != attachmentNames.length || attachmentMimeTypes.length != content.length) {
            return null;
        }
        establishSession(sessionId);

        try {

            User user = userDirectoryService.getUserByEid(userId);
            AssignmentSubmission submission = assignmentService.getSubmission(assignmentId, user);

            // submission must have been created before trying to edit it
            if (submission == null) {
                createSubmission(sessionId, context, assignmentId, userId);
                submission = assignmentService.getSubmission(assignmentId, user);
            }

            AssignmentSubmissionEdit assignmentSubmissionEdit = assignmentService.editSubmission(submission.getReference());

            assignmentSubmissionEdit.setSubmitted(true);
            assignmentSubmissionEdit.setTimeSubmitted(timeService.newTime());

            if (submittedText != null) {
                assignmentSubmissionEdit.setSubmittedText(assignmentSubmissionEdit.getSubmittedText() + submittedText);
            }

            for (int i = 0; i < content.length; i++) {
                if (attachmentMimeTypes[i] == null || attachmentNames[i] == null || content[i] == null) {
                    continue;
                }
                if (attachmentMimeTypes[i].isEmpty() || attachmentNames[i].isEmpty() || content[i].length == 0) {
                    continue;
                }
                InputStream inputStream = new ByteArrayInputStream(content[i]);

                ResourceProperties resourceProperties = contentHostingService.newResourceProperties();
                resourceProperties.addProperty(resourceProperties.PROP_DISPLAY_NAME, attachmentNames[i]);
                ContentResource file = contentHostingService.addAttachmentResource(attachmentNames[i], context, TOOL_NAME, attachmentMimeTypes[i], inputStream, resourceProperties);

                Reference ref = entityManager.newReference(file.getReference());
                assignmentSubmissionEdit.addSubmittedAttachment(ref);
            }

            assignmentService.commitEdit(assignmentSubmissionEdit);

            Document dom = Xml.createDocument();
            Element submissionEdit = (Element) dom.createElement("submission_edit");
            dom.appendChild(submissionEdit);

            submissionEdit.setAttribute("id", assignmentSubmissionEdit.getId());
            submissionEdit.setAttribute("status", assignmentSubmissionEdit.getStatus());
            submissionEdit.setAttribute("grade_display", assignmentSubmissionEdit.getGradeDisplay());
            submissionEdit.setAttribute("assignment_id", assignmentSubmissionEdit.getAssignmentId());
            submissionEdit.setAttribute("resubmission_count", String.valueOf(assignmentSubmissionEdit.getResubmissionNum()));

            return Xml.writeDocumentToString(dom);
        } catch (Exception e) {
            LOG.error("Could not edit assignment submission.");
            throw new AxisFault("Could not edit assignment submission.", e);
        }
    }

    private Element getUserSubmissionForAssignment(String userId, String assignmentId, Document dom) throws AxisFault, UserNotDefinedException {
        try {
            User user = userDirectoryService.getUserByEid(userId);

            AssignmentSubmission assignmentSubmission = assignmentService.getSubmission(assignmentId, user);

            if (assignmentSubmission == null) {
                return null;
            }

            Element submission = (Element) dom.createElement("submission");

            buildSubmissionElement(submission, assignmentSubmission);

            buildAttachmentElement(submission, dom, assignmentSubmission.getSubmittedAttachments());

            return submission;

        } catch (Exception e) {
            LOG.error("Could not retrieve user submission for assignment.", e);
            throw new AxisFault("Could not retrieve user submission for assignment.");
        }
    }

    private List<String> getAssignmentIdsForContext(String context, String fromDate) throws AxisFault {
        Date filterDate = null;
        if (fromDate != null && fromDate.trim().length() != 0) {
            filterDate = new Date(Long.parseLong(fromDate));
        }

        try {
            List<Assignment> assignmentsList = assignmentService.getListAssignmentsForContext(context);
            List<String> ids = new ArrayList<String>();
            for (Assignment assignment : assignmentsList) {
                long time = assignment.getDueTime().getTime();
                Date dueDate = new Date(time);
                /* check if we want this assignment */
                if (filterDate != null && filterDate.after(dueDate)) {
                    continue;
                }
                ids.add(assignment.getId());
            }
            return ids;
        } catch (Exception e) {
            LOG.error("Could not retrieve assignment ids for context.", e);
            throw new AxisFault("Could not retrieve assignment ids for context.", e);
        }
    }

    private String createSubmission(String sessionId, String context, String assignmentId, String userId) throws AxisFault {
        establishSession(sessionId);

        try {

            User user = userDirectoryService.getUserByEid(userId);

            if (user == null) {
                return "user does not exit";
            }

            AssignmentSubmissionEdit assignmentSubmissionEdit = assignmentService.addSubmission(context, assignmentId, user.getId());

            assignmentSubmissionEdit.addSubmitter(user);
            assignmentSubmissionEdit.setSubmitted(true);

            Time subTime = timeService.newTime();
            assignmentSubmissionEdit.setTimeSubmitted(subTime);
            assignmentService.commitEdit(assignmentSubmissionEdit);
            return assignmentSubmissionEdit.getId();
        } catch (Exception e) {
            LOG.error("Could not create assignment submission.");
            throw new AxisFault("Could not create assignment submission.", e);
        }
    }

    private void buildSubmissionElement(Element submission, AssignmentSubmission assignmentSubmission) {
        submission.setAttribute("id", assignmentSubmission.getId());
        submission.setAttribute("context", assignmentSubmission.getContext());
        submission.setAttribute("feedback_comment", assignmentSubmission.getFeedbackComment());
        submission.setAttribute("feedback_text", assignmentSubmission.getFeedbackText());
        submission.setAttribute("submitted_text", assignmentSubmission.getSubmittedText());
        submission.setAttribute("grade", assignmentSubmission.getGrade());
        submission.setAttribute("graded", String.valueOf(assignmentSubmission.getGraded()));
        submission.setAttribute("grade_released", String.valueOf(assignmentSubmission.getGradeReleased()));
        submission.setAttribute("assignment_id", assignmentSubmission.getAssignmentId());
    }

    private void buildAttachmentElement(Element element, Document dom, List<Reference> attachements) {
        for (Reference reference : attachements) {
            Entity entity = reference.getEntity();
            ResourceProperties resourceProperties = entity.getProperties();
            String id = entity.getId();
            String name = resourceProperties.getProperty(resourceProperties.getNamePropDisplayName());
            String url = entity.getUrl();
            String size = resourceProperties.getProperty(resourceProperties.getNamePropContentLength());
            String type = resourceProperties.getProperty(resourceProperties.getNamePropContentType());

            Element attachment = dom.createElement("attachment");
            attachment.setAttribute("id", id);
            attachment.setAttribute("url", url);
            attachment.setAttribute("name", name);
            attachment.setAttribute("size", size);
            attachment.setAttribute("type", type);

            element.appendChild(attachment);
        }
    }

    private void buildAssignmentElement(Element element, Assignment assignment) {
        element.setAttribute("id", assignment.getId());
        element.setAttribute("title", assignment.getTitle());
        element.setAttribute("creator", assignment.getCreator());
        element.setAttribute("context", assignment.getContext());
        element.setAttribute("open_time", String.valueOf(assignment.getOpenTime()));
        element.setAttribute("due_time", String.valueOf(assignment.getDueTime()));
        element.setAttribute("drop_dead_time", String.valueOf(assignment.getDropDeadTime()));
        element.setAttribute("author_last_modified", assignment.getAuthorLastModified());
        element.setAttribute("instructions", assignment.getContent().getInstructions());
        element.setAttribute("max_grade_point_display", assignment.getContent().getMaxGradePointDisplay());
        element.setAttribute("group_project", String.valueOf(assignment.getContent().getGroupProject()));
    }
}
