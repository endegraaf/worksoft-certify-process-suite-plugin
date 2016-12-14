package org.jenkinsci.plugins.worksoftcertifyprocesssuite;

import hudson.Launcher;
import hudson.Extension;
import hudson.Proc;
import hudson.model.*;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ExecuteCertify extends Builder {

    private final String project;
    private final String folder;
    private final String folderPath;
    String layoutPath;
    private final String target;

    public String getProject() {
        return project;
    }
    
    public String getFolder() {
    	return folder;
    }

       public String getFolderPath() {
        return folderPath;
    }
       public String getTarget() {
    	   return target;
       }

    @DataBoundConstructor
    public ExecuteCertify(String project,String folder,String folderPath,String target) {
        this.project = project;
        this.folder=folder;
        this.folderPath = folderPath;
        this.target=target;
    }
    FindLayout fl = new FindLayout();

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
        String username = getDescriptor().getUsername();
        String password = getDescriptor().getPassword();
        String databaseName = getDescriptor().getDatabaseName();
        String databaseServer = getDescriptor().getDatabaseServer();
        boolean sqlauth = getDescriptor().getSqlauth();
        String dusername = getDescriptor().getDatabaseUsername();
        String dpassword = getDescriptor().getDatabasePassword();
        String url = "jdbc:sqlserver://" + databaseServer + ";databaseName=" + databaseName + ";integratedSecurity=true";
        String urls = "jdbc:sqlserver://" + databaseServer + ";databaseName=" + databaseName;
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn;
            if (sqlauth) {
                conn = DriverManager.getConnection(urls, dusername, dpassword);
            } else {
                conn = DriverManager.getConnection(url);
            }
            Statement stm = conn.createStatement();
            String sql = "SELECT * from CMP_ProcessInfos";
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()) {
                String command = null;
                String fol = rs.getString("FolderName");
                String pro = rs.getString("ProjectName");
                if (fol.equals(folder) && pro.equals(project)) {
                    String process = rs.getString("Name");
                    String lay = rs.getString("LayoutName");
                    int fid = rs.getInt("ProcessFolderID");
                    if (lay == null) {
                        command = "Certify.exe  /usecertifyconfig+ /stepdelay=0  /Process=\"" + folderPath + "\\" + process + "\"   /Project=\"" + project + "\"   /VerifyObjects=Disabled /outputlocation=\"" + target + "\" /createoutputlocation+ /user=\"" + username + "\" /password=\"" + password + "\"";
                    } else {
                        int lid = rs.getInt("LayoutFolderID");
                        String lf = rs.getString("LayoutFolderName");
                        String recordset = rs.getString("RecordSetName");
                        int rid = rs.getInt("RecordSetMode");
                        String mode = fl.getRecordsetMode(rid);
                        layoutPath = lay;
                        layoutPath = fl.getLayoutPath(lid, conn, lay);
                        command = "Certify.exe  /usecertifyconfig+ /stepdelay=0  /Process=\"" + folderPath + "\\" + process + "\"   /Project=\"" + project + "\"  /Recordset=\"" + recordset + "\" /RecordsetsMode=\"" + mode + "\" /Layout=\"" + layoutPath + "\"   /VerifyObjects=Disabled  /outputlocation=\"" + target + "\" /createoutputlocation+ /user=\"" + username + "\" /password=\"" + password + "\"";

                    }
                }
                int exitCode = 0;
                if (command != null) {
                    Proc proc = launcher.launch(command, build.getEnvVars(), listener.getLogger(), build.getProject().getWorkspace());
                    exitCode = proc.join();
                    if (exitCode == 0) {
                        listener.getLogger().println("Execution Success");
                    } else {
                        listener.getLogger().println("Execution Failed");
                    }
                }
            }
            conn.close();
            stm.close();
            rs.close();

        } catch (ClassNotFoundException | SQLException | IOException ex) {
            listener.getLogger().println("Build Failed due to:\n" + ex.getMessage());
            build.setResult(Result.FAILURE);
        } catch (InterruptedException ex) {
            listener.getLogger().println("User Aborted the process");
        }
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String username;
        private String password;
        private String databaseServer;
        private String databaseName;
        private boolean sqlauth;
        private String databaseUsername;
        private String databasePassword;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Worksoft Certify ProcessSuite";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            username = formData.getString("username");
            password = formData.getString("password");
            databaseServer = formData.getString("databaseServer");
            databaseName = formData.getString("databaseName");
            sqlauth = formData.getBoolean("sqlauth");
            databaseUsername = formData.getString("databaseUsername");
            databasePassword = formData.getString("databasePassword");
            save();
            return super.configure(req,formData);
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getDatabaseServer() {
            return databaseServer;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public boolean getSqlauth() {
            return sqlauth;
        }

        public String getDatabaseUsername() {
            return databaseUsername;
        }

        public String getDatabasePassword() {
            return databasePassword;
        }

    }
}
