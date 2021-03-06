
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.axis.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etudes.api.app.melete.MeleteResourceService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.tool.api.SessionManager;
import org.etudes.api.app.melete.ModuleObjService;
import org.etudes.api.app.melete.ModuleService;
import org.etudes.api.app.melete.ModuleShdatesService;
import org.etudes.api.app.melete.SectionObjService;
import org.etudes.api.app.melete.SectionResourceService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The web service dedicated to retrieving melete data.
 *
 * @author OpenCollab
 * @since 1.0.0
 */
public class MeleteTool {

    private static final Log LOG = LogFactory.getLog(MeleteTool.class);
    private final SessionManager sessionManager;
    private final ModuleService moduleService;
    private final ContentHostingService contentService;

    public MeleteTool() {
        sessionManager = (SessionManager) ComponentManager.get(SessionManager.class.getName());
        moduleService = (ModuleService) ComponentManager.get(ModuleService.class);
        contentService = (ContentHostingService) ComponentManager.get(ContentHostingService.class);
    }

    /**
     * Get melete course module for site
     *
     * @param sessionId
     * @param siteId
     * @return
     */
    public String getMeleteCourseModule(String sessionId, String siteId, String fromDate) throws AxisFault {
        List<ModuleObjService> modules;
        this.establishSession(sessionId);
        Document dom = Xml.createDocument();
        Node list = dom.createElement("list");
        dom.appendChild(list);
        Date filterDate = null;
        try {
            if (!StringUtils.isEmpty(fromDate)) {
                filterDate = new Date(Long.parseLong(fromDate));
            }
            modules = moduleService.getModules(siteId);
            for (ModuleObjService module : modules) {
                Element moduleEle = dom.createElement("module");
                moduleEle.setAttribute("id", module.getModuleId().toString());
                moduleEle.setAttribute("title", module.getTitle());
                moduleEle.setAttribute("description", module.getDescription());
                moduleEle.setAttribute("version", String.valueOf(module.getVersion()));
                moduleEle.setAttribute("keywords", module.getKeywords());
                Date updateDate = module.getCreationDate();
                moduleEle.setAttribute("creation-date", convertDate(updateDate));
                moduleEle.setAttribute("user-id", module.getUserId());
                moduleEle.setAttribute("modify-user-id", module.getModifyUserId());
                moduleEle.setAttribute("whats-next", module.getWhatsNext());
                moduleEle.setAttribute("open", "");
                moduleEle.setAttribute("due", "");
                try {
                    ModuleShdatesService moduleShdatesService = module.getModuleshdate();
                    moduleEle.setAttribute("open", convertDate(moduleShdatesService.getStartDate()));
                    moduleEle.setAttribute("due", convertDate(moduleShdatesService.getEndDate()));
                    moduleEle.setAttribute("allow-until", convertDate(moduleShdatesService.getAllowUntilDate()));
                } catch (Exception e) {
                    LOG.error("Unable to convert the dates", e);
                }
                if (null != module.getModificationDate()) {
                    updateDate = module.getModificationDate();
                    moduleEle.setAttribute("modification-date", convertDate(updateDate));
                } else {
                    moduleEle.setAttribute("modification-date", "");
                }
                Node seq = dom.createElement("seq-xml");
                seq.setTextContent(module.getSeqXml());
                moduleEle.appendChild(seq);

                Map sections = module.getSections();
                parseSection(dom, list, sections, filterDate);

                if (filterDate == null || filterDate.before(updateDate)) {
                    list.appendChild(moduleEle);
                }
            }
        } catch (Exception e) {
            LOG.error("Error retrieving the Melete module.", e);
            throw new AxisFault("Error retrieving the Melete module.", e);
        }
        return Xml.writeDocumentToString(dom);
    }

    /**
     * Get the Session related to the given sessionid
     *
     * @param sessionid	the id of the session to retrieve
     * @return	the session, if it is active
     * @throws AxisFault	if session is inactive
     */
    private Session establishSession(String sessionid) throws AxisFault {
        Session s = null;
        try {
            s = sessionManager.getSession(sessionid);

            if (s == null) {
                throw new AxisFault("Session \"" + sessionid + "\" is not active");
            }
            s.setActive();
            sessionManager.setCurrentSession(s);
        } catch (Exception e) {
            LOG.error(this.getClass().getName(), e);
        }
        return s;
    }

    /**
     * Parse section
     */
    private void parseSection(Document dom, Node node, Map<Integer, SectionObjService> sections, Date filterDate) throws AxisFault {
        Date updateDate = null;
        for (Map.Entry<Integer, SectionObjService> entry : sections.entrySet()) {
            Element sectionNode = dom.createElement("section");
            Integer sectionId = entry.getKey();
            SectionObjService section = entry.getValue();
            sectionNode.setAttribute("id", String.valueOf(sectionId));
            sectionNode.setAttribute("title", section.getTitle());
            String contentType = section.getContentType();
            sectionNode.setAttribute("content-type", contentType);
            sectionNode.setAttribute("version", String.valueOf(section.getVersion()));
            sectionNode.setAttribute("user-id", section.getUserId());
            sectionNode.setAttribute("modify-user-id", section.getModifyUserId());
            sectionNode.setAttribute("instructor", section.getInstr());
            updateDate = section.getCreationDate();
            sectionNode.setAttribute("creation-date", String.valueOf(updateDate.getTime()));
            updateDate = section.getModificationDate();
            if (updateDate == null) {
                sectionNode.setAttribute("modification-date", "");
            } else {
                sectionNode.setAttribute("modification-date", String.valueOf(updateDate.getTime()));
            }
            sectionNode.setAttribute("parent-module-id", String.valueOf(section.getModuleId()));
            if (contentType == null || contentType.trim().length() == 0 || contentType.equals("notype")) {
                sectionNode.setAttribute("content", "");
            } else {//typeEditor and typeUpload
                SectionResourceService srs = section.getSectionResource();
                MeleteResourceService mrs = srs.getResource();
                String resourceId = mrs.getResourceId();
                try {
                    ContentResource cr = contentService.getResource(resourceId);
                    parseResource(dom, sectionNode, contentType, cr, section.getSectionId().toString());
                } catch (Exception j) {
                    LOG.error("Unable to parse the content resource. ", j);
                    //throw new AxisFault("Unable to retrieve/parse the content resource.");
                }
            }
            if (filterDate == null || filterDate.before(updateDate)) {
                node.appendChild(sectionNode);
            }
        }
    }

    /**
     * Helper method to parse content resource objects to xml
     */
    private Document parseResource(Document dom, Node list, String contentType, ContentResource res, String parentId) throws ServerOverloadException {
        Element resource = dom.createElement("resource");
        String[] ref = res.getReference().split("/");
        boolean isCollection = res.isCollection();
        resource.setAttribute("id", res.getId());
        resource.setAttribute("parent-id", parentId);
        resource.setAttribute("name", ref[ref.length - 1]);
        resource.setAttribute("description", res.getProperties().getProperty(res.getProperties().getNamePropDescription()));
        resource.setAttribute("type", res.getContentType());
        resource.setAttribute("created-by", res.getProperties().getProperty(res.getProperties().getNamePropCreator()));
        try {
            resource.setAttribute("created", convertSakaiTimeToDate(res.getProperties().getProperty(res.getProperties().getNamePropCreationDate())));
            resource.setAttribute("last-changed", convertSakaiTimeToDate(res.getProperties().getProperty(res.getProperties().getNamePropModifiedDate())));
        } catch (ParseException pe) {
            resource.setAttribute("created", "");
            resource.setAttribute("last-changed", "");
        }
        resource.setAttribute("last-changed-by", res.getProperties().getProperty(res.getProperties().getNamePropModifiedBy()));
        resource.setAttribute("size", Long.toString(res.getContentLength()));
        resource.setAttribute("url", res.getUrl());
        resource.setAttribute("is-collection", (isCollection) ? "true" : "false");
        //for contentType typeEditor we put the content inline encoded
        //for contentType typeUpload we do not set here, we get it later
        if (!isCollection && "typeEditor".equals(contentType)){
            Node eleContent = dom.createElement("content");
            resource.appendChild(eleContent);
            eleContent.appendChild(dom.createTextNode(new String(Base64.encodeBase64(res.getContent()))));
        }
        list.appendChild(resource);
        return dom;
    }

    /**
     * The beginning , end and mod dates are of an abstract Sakai specific
     * format, they can be empty or contain a long value which must be
     * reformatted to return a standard date in long format
     *
     * @param sakaiTime
     * @return
     * @throws ParseException
     */
    private String convertSakaiTimeToDate(String sakaiTime) throws ParseException {
        String outputDate = "";
        if (sakaiTime != null) {
            DateFormat inputDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            Date date = inputDate.parse(String.valueOf(sakaiTime));
            outputDate = String.valueOf(date.getTime());
        }
        return outputDate;
    }
    
    private String convertDate(Date date){
        if(date == null){
            return "";
        }
        return String.valueOf(date.getTime());
    }
}