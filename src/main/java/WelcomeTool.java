
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.axis.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlService;

/**
 * Retrieve the Unisa Welcome tool content. It is a simple tool with no api. So
 * we will be talking directly to the database. It does not have any user
 * permissions.
 *
 * @author OpenCollab
 * @since 1.0.0
 */
public class WelcomeTool {

    private final SqlService sqlService;
    private static final Log LOG = LogFactory.getLog(WelcomeTool.class);

    public WelcomeTool() {
        sqlService = (SqlService) ComponentManager.get(SqlService.class);
    }

    /**
     * Returns the Welcome content string for a specific site after a date
     *
     * @param siteId
     * @return
     * @throws Exception
     */
    public String getWelcomeForSite(String siteId, String fromDate) throws Exception {
        Connection conn = null;
        PreparedStatement prepStmtWelcome = null;
        String content = "";
        try {
            fromDate = fromDate==null||fromDate.trim().length()==0?"0":fromDate;
            conn = sqlService.borrowConnection();
            prepStmtWelcome = conn.prepareStatement("select CONTENT from WELCOME_CONTENT where SITE_ID = ? AND modified_on >= ?");
            
            prepStmtWelcome.setString(1, siteId);
            prepStmtWelcome.setDate(2, new java.sql.Date(Long.parseLong(fromDate)));
            ResultSet rset = prepStmtWelcome.executeQuery();
            
            if (rset.next()) {
                content = rset.getString("CONTENT");
            }
        } catch (Exception e) {
            LOG.error("Error retrieving the Welcome content : " + e.getMessage() + " : " + Thread.currentThread().getStackTrace()[1], e);
            throw new AxisFault("Error retrieving the Welcome data.", e);
        } finally {
            closeSqlObject(prepStmtWelcome);
            closeSqlObject(conn);
        }
        return content;
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
                    ((Statement) sqlObject).close();
                }
                if (sqlObject instanceof Connection) {
                    ((Connection) sqlObject).close();
                }
            }
        } catch (Exception e) {
            LOG.error("Error closing sql object in WelcomeTool.", e);
        }
    }
}
