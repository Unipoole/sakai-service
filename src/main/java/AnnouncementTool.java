
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.axis.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.announcement.api.AnnouncementMessage;
import org.sakaiproject.announcement.api.AnnouncementMessageHeader;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.announcement.api.AnnouncementService.*;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The web service dedicated to retrieving announcements.
 * 
 * @author OpenCollab
 * @since 1.0.0
 */
public class AnnouncementTool {

    static final Log LOG = LogFactory.getLog(AnnouncementTool.class);
    private static final String ANNOUNCE_TOOL_ID = "sakai.announcements";
    private final SessionManager sessionManager;
    private final SiteService siteService;
    private final AnnouncementService announcementService;

    /**
     * Setup dependencies
     */
    public AnnouncementTool() {
        announcementService = (AnnouncementService) ComponentManager.get(AnnouncementService.class.getName());
        sessionManager = (SessionManager) ComponentManager.get(SessionManager.class.getName());
        siteService = (SiteService) ComponentManager.get(SiteService.class.getName());
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
     * Retrieve a sites announcements when given the sessionId and the siteId.
     *
     * An announcement tool for a site/module has channel(s) Channel has messages.
     *
     * @param sessionId e.g. 7ee79c0d-1e58-4307-a322-654215331ab6
     * @param siteId e.g. AFL1501-13-S1-43T
     * @return xml representation of the announcement. If the site does not have announcements it returns an empty XML
     * element.
     * <?xml version="1.0" encoding="UTF-8"?>
     * <list>
     * <message date="13-Aug-2013 14:00" from="Opencollab Admin" id="095ca24c-3f1f-4624-988a-3f4b051a6492" order="8"
     * subject="Getting started with Discussions"
     * url="http://domain.com/access/announcement/msg/AFL1501-13-S1-43T/main/095ca24c-3f1f-4624-988a-3f4b051a6492" >
     * <groups/>
     * <body>
     * Body text
     * </body>
     * <attachments>
     * <attachment name="IMG_20130808_164529.jpg" size="558456" type="image/jpeg"
     * url="http://domain.com/access/content/attachment/AFL1501-13-S1-43T/Announcements/ad3f533f-b7a5-4f98-b12b-3ceec4389d4d/IMG_20130808_164529.jpg"
     * / >
     * </attachments>
     * </message>
     * </list>
     */
    public String getAnnouncementsForSite(String sessionId, String siteId, final String fromDate) throws AxisFault {
        establishSession(sessionId);
        Document dom = Xml.createDocument();
        Element list = (Element) dom.createElement("list");
        dom.appendChild(list);
        //get get list of channels
        List<String> channelList = this.makeReferenceAnnouncements(siteId);
        if (channelList == null) {
            return Xml.writeDocumentToString(dom);
        }
        try {
            Date filterDate = null;
            Date messageDate = null;
            long time;
            if(fromDate != null && fromDate.trim().length() != 0){
                filterDate = new Date(Long.parseLong(fromDate));
            }
            for (String ref : channelList) {
                if (ref == null) {
                    continue;
                }
                //get all messages for the channel

                List<AnnouncementMessage> annMsgs = announcementService.getMessages(ref, null, true, true);
                for (AnnouncementMessage aMessage : annMsgs) {
                    AnnouncementMessageHeader amh = aMessage.getAnnouncementHeader();
                    //build message node
                    Element message = dom.createElement("message");
                    message.setAttribute("id", aMessage.getId());
                    message.setAttribute("subject", amh.getSubject());
                    message.setAttribute("from", amh.getFrom().getDisplayName());
                    time = getXMLTime(amh.getDate());
                    messageDate = new Date(time);
                    message.setAttribute("date", String.valueOf(time));
                    message.setAttribute("url", aMessage.getUrl());
                    message.setAttribute("order", String.valueOf(aMessage.getHeader().getMessage_order()));
                    message.setAttribute("begin_date", "");
                    message.setAttribute("end_date", "");
                    message.setAttribute("mod_date", "");
                    try {
                        message.setAttribute("begin_date", String.valueOf(getXMLTime(aMessage.getProperties().getTimeProperty(AnnouncementService.RELEASE_DATE))));
                    } catch (Exception e) {
                    }
                    try {
                        message.setAttribute("end_date", String.valueOf(getXMLTime(aMessage.getProperties().getTimeProperty(AnnouncementService.RETRACT_DATE))));
                    } catch (Exception e) {
                    }
                    try {
                        time = getXMLTime(aMessage.getProperties().getTimeProperty(AnnouncementService.MOD_DATE));
                        messageDate = new Date(time);
                        message.setAttribute("mod_date", String.valueOf(time));
                    } catch (Exception e) {
                    }
                    //check if we want this message
                    if(filterDate != null && messageDate.before(filterDate)){
                        continue;
                    }
                    //build group node
                    List<String> groupsList = (List<String>) amh.getGroups();
                    message = this.getGroups(dom, groupsList, message);

                    Node bodyNode = dom.createElement("body");
                    message.appendChild(bodyNode);
                    bodyNode.appendChild(dom.createTextNode(aMessage.getBody()));

                    //get all attachments for a message
                    List<Reference> attachments = aMessage.getHeader().getAttachments();
                    if(attachments != null && !attachments.isEmpty()){
                        message.appendChild(this.getAttachments(dom, attachments));
                    }
                    //check if we want this message
                    if(filterDate == null || messageDate.after(filterDate)){
                        list.appendChild(message);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Could not retrieve announcements.", e);
            throw new AxisFault("Could not retrieve announcements.", e);
        }
        return Xml.writeDocumentToString(dom);
    }

    /**
     * Helper method to construct a list of references for a siteId
     *
     * @param siteId
     * @return List of references or null
     */
    private List<String> makeReferenceAnnouncements(String siteId) {
        if (siteHasAnnouncementTool(siteId)) {
            List<String> refList = new ArrayList<String>();
            List<String> idList = announcementService.getChannelIds(siteId);
            for (String id : idList) {
                refList.add(announcementService.channelReference(siteId, id));
            }
            return refList;
        } else {
            return null;
        }
    }

    /**
     * Helper method to determine whether a site has an announcement tool.
     *
     * @param siteId
     * @return
     */
    private boolean siteHasAnnouncementTool(String siteId) {
        try {
            return siteService.getSite(siteId).getToolForCommonId(ANNOUNCE_TOOL_ID) != null;
        } catch (IdUnusedException e) {
            return false;
        }
    }

    /**
     * Given a list of attachments parse through the list and return an XML element.
     *
     * @param dom XML Document
     * @param attachments List of attachments
     * @param message XML Element
     * @return Element message
     */
    private Element getAttachments(Document dom, List<Reference> attachments) {
        Element attach = dom.createElement("attachments");
        for (Reference attRef : attachments) {
            Entity ent = attRef.getEntity();
            ResourceProperties resourceProperties = ent.getProperties();
            String id = ent.getId();
            String name = resourceProperties.getProperty(resourceProperties.getNamePropDisplayName());
            String url = ent.getUrl();
            String size = resourceProperties.getProperty(resourceProperties.getNamePropContentLength());
            String type = resourceProperties.getProperty(resourceProperties.getNamePropContentType());

            Element a = dom.createElement("attachment");
            a.setAttribute("id", id);
            a.setAttribute("url", url);
            a.setAttribute("name", name);
            a.setAttribute("size", size);
            a.setAttribute("type", type);
            attach.appendChild(a);
        }
        return attach;
    }

    /**
     * @param dom XML Document
     * @param groupList list of group names which the announcement is for
     * @param message XML Element
     * @return message XML Element
     */
    private Element getGroups(Document dom, List<String> groupList, Element message) {
        Node groups = dom.createElement("groups");
        for (String groupAnn : groupList) {
            Element group = dom.createElement("group");
            groups.appendChild(group);
            group.setNodeValue(groupAnn);
        }
        message.appendChild(groups);
        return message;
    }
    
    private long getXMLTime(Time time){
        return time.getTime();
    }
}
