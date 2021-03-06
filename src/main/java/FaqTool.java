
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.axis.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Retrieve the Unisa Faq tool content. It is a simple tool with no API. So we
 * will be talking directly to the database. It does not have any user
 * permissions.
 *
 * @author OpenCollab
 * @since 1.0.0
 */
public class FaqTool {

    private final SqlService sqlService;
    private static final Log LOG = LogFactory.getLog(FaqTool.class);

    public FaqTool() {
        sqlService = (SqlService) ComponentManager.get(SqlService.class);
    }

    /**
     * Build XML dom by selecting the Faq data from the database.
     * 
     * The categories and content is handled on the same level as we want to be 
     * able to update only one of them if a change was made
     *
     * @param siteId
     * @param fromDate
     * @return String xml dom
     * @throws Exception
     */
    public String getFaqForSite(String siteId, String fromDate) throws Exception {
        Document dom = Xml.createDocument();
        Node list = dom.createElement("list");
        dom.appendChild(list);
        Connection conn = null;
        PreparedStatement prepStmtCategory = null;
        PreparedStatement prepStmtContent = null;
        try {
            fromDate = fromDate==null||fromDate.trim().length()==0?"0":fromDate;
            conn = sqlService.borrowConnection();
            prepStmtCategory = conn.prepareStatement("select CATEGORY_ID, DESCRIPTION, MODIFIED_ON from FAQ_CATEGORY where SITE_ID = ? AND modified_on >= ?");
            prepStmtContent = conn.prepareStatement("select A.CONTENT_ID, A.CATEGORY_ID, A.QUESTION, A.ANSWER, A.MODIFIED_ON from FAQ_CONTENT A, faq_category B where a.category_id = b.category_id AND B.site_id = ? AND A.modified_on >= ?");
            prepStmtCategory.setString(1, siteId);
            prepStmtCategory.setDate(2, new java.sql.Date(Long.parseLong(fromDate)));
            ResultSet rsetCat = prepStmtCategory.executeQuery();
            
            while (rsetCat.next()) {                
                Element catElement = dom.createElement("faq");
                catElement.setAttribute("id", String.valueOf(rsetCat.getInt("CATEGORY_ID")));
                catElement.setAttribute("parentId", String.valueOf(rsetCat.getInt("CATEGORY_ID")));
                catElement.setAttribute("description", rsetCat.getString("DESCRIPTION"));
                catElement.setAttribute("modified-on", String.valueOf(rsetCat.getDate("MODIFIED_ON").getTime()));               

                list.appendChild(catElement);
            }            
            prepStmtContent.setString(1, siteId);
            prepStmtContent.setDate(2, new java.sql.Date(Long.parseLong(fromDate)));
            ResultSet rsetContent = prepStmtContent.executeQuery();
            
            while (rsetContent.next()) {
                    Element contentElement = dom.createElement("faq");
                    contentElement.setAttribute("id", String.valueOf(rsetContent.getInt("CONTENT_ID")));
                    contentElement.setAttribute("parentId", String.valueOf(rsetContent.getInt("CATEGORY_ID")));
                    contentElement.setAttribute("question", rsetContent.getString("QUESTION"));
                    contentElement.setAttribute("answer", rsetContent.getString("ANSWER"));
                    contentElement.setAttribute("modified-on", String.valueOf(rsetContent.getDate("MODIFIED_ON").getTime()));
                    
                    list.appendChild(contentElement);
                }          
            
        } catch (Exception e) {
            LOG.error("Error retrieving the faq content.", e);
            throw new AxisFault("Error retrieving the Faq data.", e);
        } finally {
            closeSqlObject(prepStmtCategory);
            closeSqlObject(prepStmtContent);
            closeSqlObject(conn);
        }
        return Xml.writeDocumentToString(dom);
    }

    /**
     * Checks if object is null and closes it
     * 
     * @param sqlObject 
     */
    private void closeSqlObject(Object sqlObject) {
        try {
            if (sqlObject != null) {
                if (sqlObject instanceof Statement) {
                    ((Statement)sqlObject).close();
                }
                if (sqlObject instanceof Connection) {
                    ((Connection)sqlObject).close();
                }
            }
        }catch(Exception e) {
            LOG.error("Error closing sql object in FaqTool.", e);
        }
    }
}
