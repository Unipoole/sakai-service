
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.axis.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.announcement.api.AnnouncementService.*;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.Xml;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.YaftSecurityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * YaftTool.jws
 *
 * The web service dedicated to retrieving
 *
 */
public class YaftTool {

    static final Log LOG = LogFactory.getLog(YaftTool.class);
    private final SessionManager sessionManager;
    private final UserDirectoryService userDirectoryService;
    private final YaftForumService yaftForumService;
    private final Map<String, String> users = new WeakHashMap<String, String>();
    private static final String READY = "READY";
    private static final String DELETED = "DELETED";
    private static final String DRAFT = "DRAFT";

    /**
     * Setup dependencies
     */
    public YaftTool() {
        sessionManager = (SessionManager) ComponentManager.get(SessionManager.class.getName());
        yaftForumService = (YaftForumService) ComponentManager.get(YaftForumService.class.getName());
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
     * ======================= 
     * Start Data Retrieval Section
     * ========================
     * @return 
     * @throws org.sakaiproject.yaft.api.YaftSecurityException
     */
    //get deleteed discussions
    //XXX ?
    public String getDeletedData() throws YaftSecurityException {
        yaftForumService.deleteDiscussion("","","");
        return "";
    }

    /**
     * Get Forums for a site when given the following
     *
     * @param sessionId
     * @param siteId
     * @param fromDate
     * @return
     * @throws AxisFault
     * <?xml version="1.0" encoding="UTF-8"?>
     * <list>
     * <forums>
     * <forum creator_id="ff363bbb-c651-4fc6-8e1c-20f2ef37115a"
     * creator_name="Opencollab Admin" description="testing 1234" end="-1"
     * id="2a175f90-98df-4200-9f91-f05fcdd9c134"
     * last_message_date="1379924797516" messages="6" modified_by=""
     * modified_date="0" site_id="11912d34-c7ab-41e0-b777-ef347e11c959"
     * start="-1" status="READY" title="test" topics="2" unread="0"
     * url="/portal/tool/0cac12f2-7542-4901-85b0-b38cf91c923d/forums/2a175f90-98df-4200-9f91-f05fcdd9c134.html">
     * <discussions>
     * <discussion content="asdasdasd" create_date="1379676496910"
     * creator_id="ff363bbb-c651-4fc6-8e1c-20f2ef37115a"
     * forum_id="2a175f90-98df-4200-9f91-f05fcdd9c134"
     * id="224948be-7228-42c0-8cb5-73b216a77cea" last_message="1379924797516"
     * message_count="5" modified_by="" modified_date="0" page_id=""
     * site_id="11912d34-c7ab-41e0-b777-ef347e11c959" started_by="Opencollab
     * Admin" topic="Discussion 1"
     * url="/portal/tool/0cac12f2-7542-4901-85b0-b38cf91c923d/discussions/224948be-7228-42c0-8cb5-73b216a77cea.html">
     * <messages>
     * <message attachment_count="1" content="reply number 2"
     * create_date="1379683255842" creator="Opencollab Admin"
     * creator_Id="ff363bbb-c651-4fc6-8e1c-20f2ef37115a" depth="0"
     * discussion_id="224948be-7228-42c0-8cb5-73b216a77cea"
     * full_url="/portal/directtool/0cac12f2-7542-4901-85b0-b38cf91c923d/messages/4973957e-9dca-4ed3-8804-a9f7293411bc.html"
     * group_size="0" id="4973957e-9dca-4ed3-8804-a9f7293411bc" modified_by=""
     * modified_date="" parent="0" reply_count="1"
     * site_id="11912d34-c7ab-41e0-b777-ef347e11c959" status="READY" topic="Re:
     * Discussion 1">
     * <attachments>
     * <attachment id="Ovnes4j.jpg" name="Ovnes4j.jpg" size="0"
     * type="image/jpeg"
     * url="http://unipoole.opencollab.co.za:8080/access/content/group/11912d34-c7ab-41e0-b777-ef347e11c959/yaft-files/Ovnes4j.jpg"
     * />
     * </attachments>
     * </message>
     * </messages>
     * </discussion>
     * </discussions>
     * </forum>
     * </forums>
     * </list>
     */
    public String getForumsForSiteById(String sessionId, String siteId, String fromDate) throws AxisFault {
        establishSession(sessionId);

        Document dom = Xml.createDocument();
        Element list = (Element) dom.createElement("list");
        dom.appendChild(list);
        Date filterDate = null;

        if (StringUtils.isEmpty(siteId)) {
            return "<list/>";
        }

        List<Forum> forums;
        Node forumsEle = dom.createElement("forums");
        list.appendChild(forumsEle);
        try {
            forums = yaftForumService.getSiteForums(siteId, true);
            Date updateDate = null;
            if (!StringUtils.isEmpty(fromDate)) {
                filterDate = new Date(Long.parseLong(fromDate));
            }
            for (Forum forum : forums) {
                Element forumEle = dom.createElement("forum");
                forumEle.setAttribute("title", forum.getTitle());
                forumEle.setAttribute("messages", String.valueOf(forum.getMessageCount()));
                forumEle.setAttribute("creator_id", forum.getCreatorId());
                forumEle.setAttribute("creator_name", getUserDisplayName(forum.getCreatorId()));
                forumEle.setAttribute("description", forum.getDescription());
                forumEle.setAttribute("id", forum.getId());
                forumEle.setAttribute("site_id", forum.getSiteId());
                forumEle.setAttribute("start", String.valueOf(forum.getStart()));
                forumEle.setAttribute("end", String.valueOf(forum.getEnd()));
                forumEle.setAttribute("status", forum.getStatus());
                forumEle.setAttribute("url", forum.getUrl());
                forumEle.setAttribute("last_message_date", String.valueOf(forum.getLastMessageDate()));
                updateDate = new Date(forum.getLastMessageDate());
                long modifiedDate = 0;
                String modifiedBy = "";
                try {
                    modifiedDate = Long.parseLong(forum.getProperties().getNamePropModifiedDate());
                    modifiedBy = forum.getProperties().getNamePropModifiedBy();
                } catch (Exception npe) {
                    //LOG.warn("modification conversion : ", npe);
                }
                forumEle.setAttribute("modified_date", String.valueOf(modifiedDate));
                forumEle.setAttribute("modified_by", modifiedBy);
                //XXX is this right?
                forumEle.setAttribute("unread", "0");
                List<Discussion> discussions = forum.getDiscussions();
                forumEle.setAttribute("topics", String.valueOf(discussions.size()));

                if (filterDate == null || updateDate.after(filterDate)) {
                    forumsEle.appendChild(forumEle);
                    Node discussionsEle = dom.createElement("discussions");
                    forumEle.appendChild(discussionsEle);
                    //get all didcussions & child messages for this forum
                    getDiscussion(dom, discussions, discussionsEle, filterDate);
                }
            }
        } catch (Exception e) {
            LOG.error("Error retrieving Fora : ", e);
        }
        return Xml.writeDocumentToString(dom);
    }

    //XXX This will always return "" FIXME!
    public String getDiscussionById(String sessionId, String discussionId) throws AxisFault {
        String result = "";
        establishSession(sessionId);
        try {
            Discussion dis = yaftForumService.getDiscussion(discussionId, true);
        } catch (Exception ex) {
            LOG.error(ex);
        }
        return result;
    }

    /**
     * Get the discussions belonging to a forum when given
     *
     * @param dom
     * @param discussions
     * @param discussionsEle
     * @param filterDate no return , it adds the elements to the document
     *
     * <discussion content="asdasdasd" create_date="1379676496910"
     * creator_id="ff363bbb-c651-4fc6-8e1c-20f2ef37115a"
     * forum_id="2a175f90-98df-4200-9f91-f05fcdd9c134"
     * id="224948be-7228-42c0-8cb5-73b216a77cea" last_message="1379924797516"
     * message_count="5" modified_by="" modified_date="0" page_id=""
     * site_id="11912d34-c7ab-41e0-b777-ef347e11c959" started_by="Opencollab
     * Admin" topic="Discussion 1"
     * url="/portal/tool/0cac12f2-7542-4901-85b0-b38cf91c923d/discussions/224948be-7228-42c0-8cb5-73b216a77cea.html">
     * <messages>
     * <message attachment_count="1" content="reply number 2"
     * create_date="1379683255842" creator="Opencollab Admin"
     * creator_Id="ff363bbb-c651-4fc6-8e1c-20f2ef37115a" depth="0"
     * discussion_id="224948be-7228-42c0-8cb5-73b216a77cea"
     * full_url="/portal/directtool/0cac12f2-7542-4901-85b0-b38cf91c923d/messages/4973957e-9dca-4ed3-8804-a9f7293411bc.html"
     * group_size="0" id="4973957e-9dca-4ed3-8804-a9f7293411bc" modified_by=""
     * modified_date="" parent="0" reply_count="1"
     * site_id="11912d34-c7ab-41e0-b777-ef347e11c959" status="READY" topic="Re:
     * Discussion 1">
     * <attachments>
     * <attachment id="Ovnes4j.jpg" name="Ovnes4j.jpg" size="0"
     * type="image/jpeg"
     * url="http://unipoole.opencollab.co.za:8080/access/content/group/11912d34-c7ab-41e0-b777-ef347e11c959/yaft-files/Ovnes4j.jpg"
     * />
     * </attachments>
     * </message>
     * </messages>
     * </discussion>
     */
    private void getDiscussion(Document dom, List<Discussion> discussions, Node discussionsEle, Date filterDate) {
        Date updateDate = null;
        for (Discussion discus : discussions) {
            Element discussionEle = dom.createElement("discussion");
            discussionEle.setAttribute("id", discus.getId());
            discussionEle.setAttribute("forum_id", discus.getForumId());
            discussionEle.setAttribute("started_by", discus.getCreatorDisplayName());
            discussionEle.setAttribute("creator_id", discus.getCreatorId());
            discussionEle.setAttribute("page_id", discus.getPageId());
            discussionEle.setAttribute("site_id", discus.getSiteId());
            discussionEle.setAttribute("topic", discus.getSubject());
            discussionEle.setAttribute("url", discus.getUrl());
            discussionEle.setAttribute("create_date", String.valueOf(discus.getCreatedDate()));
            discussionEle.setAttribute("content", discus.getContent());
            discussionEle.setAttribute("message_count", String.valueOf(discus.getMessageCount()));
            discussionEle.setAttribute("last_message", String.valueOf(discus.getLastMessageDate()));
            try {
                discussionEle.setAttribute("assisgnment_id", (null == discus.getAssignment().getId()) ? "" : String.valueOf(discus.getAssignment().getId()));
                discussionEle.setAttribute("assisgnment_name", (null == discus.getAssignment().getName()) ? "" : discus.getAssignment().getName());
            } catch (Exception ex) {
                //LOG.warn("Error retrieving assignement data : ", ex);
            }
            updateDate = new Date(discus.getLastMessageDate());
            long modifiedDiscussionDate = 0;
            String modifiedDiscussionBy = "";
            try {
                modifiedDiscussionDate = Long.parseLong(discus.getProperties().getNamePropModifiedDate());
                modifiedDiscussionBy = discus.getProperties().getNamePropModifiedBy();
            } catch (Exception npe) {
                LOG.warn("modification conversion : ", npe);
            }
            discussionEle.setAttribute("modified_date", String.valueOf(modifiedDiscussionDate));
            discussionEle.setAttribute("modified_by", modifiedDiscussionBy);
            if (filterDate == null || updateDate.after(filterDate)) {
                discussionsEle.appendChild(discussionEle);
                //get messages
                Node messagesEle = dom.createElement("messages");
                discussionEle.appendChild(messagesEle);
                Message message = discus.getFirstMessage();
                // Add the attachments of the discussion
                Element attachments = dom.createElement("attachments");
                if (message.getAttachments().size() > 0) {
                    getAttachments(dom, message, attachments);
                }
                discussionEle.appendChild(attachments);
                discussionEle.setAttribute("attachment_count", String.valueOf(message.getAttachments().size()));
                
                List<Message> messages = message.getChildren();
                for (Message mess : messages) {
                    this.getMessage(dom, mess, messagesEle, "0", 0, filterDate);
                }
            }
        }
    }

    /**
     * Get the Message for a discussion
     *
     * @param dom
     * @param message
     * @param messagesEle
     * @param parentId
     * @param depth
     * @param filterDate
     * <message attachment_count="1" content="reply number 2"
     * create_date="1379683255842" creator="Opencollab Admin"
     * creator_Id="ff363bbb-c651-4fc6-8e1c-20f2ef37115a" depth="0"
     * discussion_id="224948be-7228-42c0-8cb5-73b216a77cea"
     * full_url="/portal/directtool/0cac12f2-7542-4901-85b0-b38cf91c923d/messages/4973957e-9dca-4ed3-8804-a9f7293411bc.html"
     * group_size="0" id="4973957e-9dca-4ed3-8804-a9f7293411bc" modified_by=""
     * modified_date="" parent="0" reply_count="1"
     * site_id="11912d34-c7ab-41e0-b777-ef347e11c959" status="READY" topic="Re:
     * Discussion 1">
     * <attachments>
     * <attachment id="Ovnes4j.jpg" name="Ovnes4j.jpg" size="0"
     * type="image/jpeg"
     * url="http://unipoole.opencollab.co.za:8080/access/content/group/11912d34-c7ab-41e0-b777-ef347e11c959/yaft-files/Ovnes4j.jpg"
     * />
     * </attachments>
     * </message>
     */
    private void getMessage(Document dom, Message message, Node messagesEle, String parentId, int depth, Date filterDate) {
        Date updateDate = null;
        Element messageEle = dom.createElement("message");
        try {
            messageEle.setAttribute("id", message.getId());
            messageEle.setAttribute("parent", message.getParent());
            messageEle.setAttribute("status", message.getStatus());
            messageEle.setAttribute("topic", message.getSubject());
            messageEle.setAttribute("creator", message.getCreatorDisplayName());
            messageEle.setAttribute("creator_id", message.getCreatorId());
            messageEle.setAttribute("create_date", String.valueOf(message.getCreatedDate()));
            messageEle.setAttribute("content", message.getContent());
            messageEle.setAttribute("discussion_id", message.getDiscussionId());
            messageEle.setAttribute("parent", parentId);
            messageEle.setAttribute("depth", String.valueOf(depth++));
            messageEle.setAttribute("full_url", message.getFullUrl());
            messageEle.setAttribute("site_id", message.getSiteId());
            messageEle.setAttribute("group_size", String.valueOf(message.getGroups().size()));
            messageEle.setAttribute("attachment_count", String.valueOf(message.getAttachments().size()));
            messageEle.setAttribute("reply_count", String.valueOf(message.getChildren().size()));
            updateDate = new Date(message.getCreatedDate());

            String modifiedMessageDate = "";
            String modifiedMessageBy = "";
            /*Program goes away when I attempt the following. Don't think sakai-axis can handle this much inheritance.
             try {
             modifiedMessageDate = message.getProperties().getNamePropModifiedDate();
             modifiedMessageBy = message.getProperties().getNamePropModifiedBy();
             } catch (Exception npe) {
             LOG.warn("modification conversion : " , npe);
             }
             */
            messageEle.setAttribute("modified_date", modifiedMessageDate);
            messageEle.setAttribute("modified_by", modifiedMessageBy);
            Element attachments = dom.createElement("attachments");
            if (message.getAttachments().size() > 0) {
                getAttachments(dom, message, attachments);
            }
            messageEle.appendChild(attachments);
        } catch (Exception ex) {
            LOG.error("Parsing mesages : ", ex);
        }
        if (filterDate == null || filterDate.before(updateDate)) {
            messagesEle.appendChild(messageEle);
            if (message.getChildren().size() > 0) {
                List<Message> messages = message.getChildren();
                for (Message mess : messages) {
                    this.getMessage(dom, mess, messagesEle, message.getId(), depth, filterDate);
                }
            }
        }
    }

    /**
     * get all attachments for a message.
     *
     * @param dom
     * @param message
     * @param messageEle
     * <attachments>
     * <attachment id="Ovnes4j.jpg" name="Ovnes4j.jpg" size="0"
     * type="image/jpeg"
     * url="http://unipoole.opencollab.co.za:8080/access/content/group/11912d34-c7ab-41e0-b777-ef347e11c959/yaft-files/Ovnes4j.jpg"
     * />
     * </attachments>
     */
    private void getAttachments(Document dom, Message message, Element messageEle) {
        List<Attachment> attachments = message.getAttachments();
        int counter = 0;
        for (Attachment att : attachments) {
            Element a = dom.createElement("attachment");
            a.setAttribute("id", att.getResourceId());
            a.setAttribute("url", att.getUrl());
            a.setAttribute("name", att.getName());
            //TODO: get resource and file size
            int dataSize = 0;
            try {
                dataSize = att.getData().length;
            } catch (Exception e) {
                LOG.error("Error retrieving the attachment size, the file most likely does not exist. If this is seen in production please inform admin..", e);
            }
            a.setAttribute("size", String.valueOf(dataSize));
            a.setAttribute("type", att.getMimeType());
            messageEle.appendChild(a);
            counter++;
        }
    }

    /**
     * ======================== End Data Retrieval Section
     * ========================
     */
    /**
     * ======================== Start Data Capture Section
     * ========================
     */
    /**
     * Create a discussion when given the following.
     *
     * @param sessionId
     * @param forumId
     * @param username
     * @param password
     * @param siteId
     * @param subject
     * @param description
     * @return
     */
    public String createDiscussion(
            String sessionId,
            String siteId,
            String userId,
            String forumId,
            String subject,
            String content,
            String createdDate)  throws AxisFault, YaftSecurityException {
        establishSession(sessionId);
        String output = "";
        Message message = new Message();
        //important you must set it to en emtpy string for ID else it will try and update the message
        message.setId("");
        message.setContent(content);
        message.setCreatedDate((StringUtils.isEmpty(createdDate)) ? new Date().getTime() : new Date(Long.parseLong(createdDate)).getTime());
        message.setCreatorId(userId);
        message.setCreatorDisplayName(getUserDisplayName(userId));
        message.setSiteId(siteId);
        //Important you must set the parent to an empty string by default it will not create the discussion
        message.setParent("");
        message.setSubject(subject);
        boolean success = yaftForumService.addOrUpdateMessage(siteId, forumId, userId, message, false);
        if (success) {
            output = message.getId();
        } else {
            output = message.getId();
        }
        return output;
    }

    /**
     * Create a discussion when given the following.
     *
     * @param sessionId
     * @param forumId
     * @param username
     * @param password
     * @param siteId
     * @param subject
     * @param description
     * @return
     * @throws org.apache.axis.AxisFault
     */
    public String deleteDisucssion(
            String sessionId,
            String discussionId,
            String siteId,
            String userId) throws AxisFault {
        establishSession(sessionId);
        Discussion discussion = getDiscussionById(discussionId);
        String output = "";
        boolean deleted = false;
        try {
            deleted = yaftForumService.deleteDiscussion(discussionId, siteId , userId);
        } catch (Exception ex) {
            LOG.warn("Change discussion status to deleted : ", ex);
        }
        return String.valueOf(deleted);
    }

    private Forum getForumById(String sessionId, String siteId, String forumId) throws AxisFault {
        establishSession(sessionId);
        List<Forum> forums = yaftForumService.getSiteForums(siteId, true);
        if (null != forums) {
            for (Forum forum : forums) {
                if (forum.getId().equals(forumId)) {
                    return forum;
                }
            }
        }
        return null;
    }

    /**
     * Update a discussion when given the following
     *
     * @param sessionId
     * @param siteId
     * @param userId
     * @param forumId
     * @param discussionId
     * @param subject
     * @param content
     * @param updatedDate
     * @param start
     * @param end
     * @param status
     * @return A string with the id for now.
     * @throws AxisFault
     */
    public String updateDisucssion(
            String sessionId,
            String siteId,
            String userId,
            String forumId,
            String discussionId,
            String subject,
            String content,
            String updatedDate,
            String start,
            String end,
            String status) throws AxisFault, YaftSecurityException {
        establishSession(sessionId);
        String output = "";
        Discussion discussion = getDiscussionById(discussionId);
        if (!start.isEmpty()) {
            discussion.setStart(Long.parseLong(start));
        }
        if (!end.isEmpty()) {
            discussion.setEnd(Long.parseLong(end));
        }
        if (!status.isEmpty()) {
            discussion.setStatus(status);
        }
        discussion.setSubject(subject);
        if (!content.isEmpty()) {
            Message message = discussion.getFirstMessage();
            message.setContent(content);
            boolean successful = yaftForumService.addOrUpdateMessage(siteId, forumId, userId, message, false);
            if (successful) {
                discussion.setMessageCount(discussion.getMessageCount() + 1);
            }
        }
        discussion = yaftForumService.addDiscussion(siteId, forumId, userId, discussion, false);
        return discussion.getId();
    }

    /**
     * Create a message when given the following
     *
     * @param sessionId
     * @param siteId
     * @param forumId
     * @param userId
     * @param discussionId
     * @param messageText
     * @param subject
     * @param content
     * @param createdDate
     * @return a string with the message Id for now, most likely will return a
     * string containing an XML representation of the message.
     * @throws AxisFault
     */
    public String createMessage(
            String sessionId,
            String siteId,
            String forumId,
            String userId,
            String discussionId,
            String subject,
            String content,
            String createdDate,
            String parentId) throws AxisFault, YaftSecurityException {
        LOG.error("session ID : " + sessionId + ", siteId :" + siteId + ", forumId :" + forumId + ", userId :" + userId + ", discussionId : " + discussionId + ", subject : " + subject + ", content :" + content + ", parentId : " + parentId);
        establishSession(sessionId);
        String output = "";
        Message message = new Message();
        message.setId("");
        message.setContent(content);
        message.setCreatedDate((StringUtils.isEmpty(createdDate)) ? new Date().getTime() : new Date(Long.parseLong(createdDate)).getTime());
        message.setCreatorDisplayName(getUserDisplayName(userId));
        message.setCreatorId(userId);
        message.setSiteId(siteId);
        message.setDiscussionId(discussionId);
        if (!parentId.isEmpty()) {
            message.setParent(parentId);
        } else {
            message.setParent(discussionId);
        }
        message.setSubject(subject);
        boolean success = yaftForumService.addOrUpdateMessage(siteId, forumId, userId, message, false);
        if (success) {
            output = message.getId();
            try {
                Discussion discussion = getDiscussionById(discussionId);
                discussion.setMessageCount(discussion.getMessageCount() + 1);
                discussion = yaftForumService.addDiscussion(siteId, forumId, userId, discussion, success);
            } catch (Exception ex) {
                LOG.error(ex);
            }
        }
        return output;
    }

    public String updateMessage(String sessionId, String siteId, String forumId, String userId, String messageId, String subject, String content, String createdDate, String parentId) throws AxisFault ,YaftSecurityException {
        establishSession(sessionId);
        String output = "";
        Message message = yaftForumService.getMessage(messageId);
        if (!content.isEmpty()) {
            message.setContent(content);
        }
        if (!StringUtils.isEmpty(createdDate)) {
            message.setCreatedDate(new Date(Long.parseLong(createdDate)).getTime());
        }
        if (!userId.isEmpty()) {
            message.setCreatorDisplayName(getUserDisplayName(userId));
            message.setCreatorId(userId);
        }
        if (!subject.isEmpty()) {
            message.setSubject(subject);
        }
        if (!parentId.isEmpty()) {
            message.setParent(parentId);
        }
        boolean success = yaftForumService.addOrUpdateMessage(siteId, forumId, userId, message, false);
        output += "\n  - " + message.getId();
        if (success) {
            message = yaftForumService.getMessage(messageId);
            output = message.getContent();
        }
        return output;
    }

    public String deleteMessage(String sessionId, String siteId, String forumId, String userId, String messageId)
            throws AxisFault, YaftSecurityException {
        establishSession(sessionId);
        String output = "";
        Message message = yaftForumService.getMessage(messageId);
        yaftForumService.deleteMessage(message, siteId, forumId, userId);
        message = yaftForumService.getMessage(messageId);
        output = message.getStatus();
        return output;
    }

    /**
     * ======================= End Data Capture Section ========================
     */
    /**
     * ======================= Helpers Section ========================
     */
    /**
     * Gets the display name for a given user
     *
     * Differs from original above as that one uses the session to get the
     * displayname hence you must know this in advance or be logged in to the
     * web services with that user. This uses a userid as well so we could be
     * logged in as admin and retrieve the display name for any user.
     *
     * @param	userid	the login username (ie jsmith26) of the user you want the
     * display name for
     * @return	the display name for the user
     * @throws	AxisFault
     *
     */
    private String getUserDisplayName(String userid) {
        String name = "";
        try {
            name = users.get(userid);
            if (name != null) {
                return name;
            }
            User user = userDirectoryService.getUser(userid);
            name = user.getDisplayName();
        } catch (Exception e) {
            LOG.error("Could not get the display name: " + userid, e);
            name = "";
        }
        users.put(userid, name);
        return name;
    }

    private Discussion getDiscussionById(String discussionId) {
        return yaftForumService.getDiscussion(discussionId, true);
    }
}
