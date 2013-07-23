package edu.ucar.unidata.rosetta.service;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

public class ServerInfoBean implements ServletContextAware  {
    /**
     * Bean containing Server Information, including info from the MANIFEST file.
     */
    private static class ServerInfo {
        private String version = null;
        private String buildDate = null;
        private ServletContext servletContext = null;

        private void init() {
            String name = "/META-INF/MANIFEST.MF";
            Properties props = new Properties();
            try {
                props.load(servletContext.getResourceAsStream(name));
                this.version = (String) props.get("Implementation-Version");
                this.buildDate = (String) props.get("Built-On");

                if (this.version == null) {
                    this.version = "localDevel";
                }

                if (this.buildDate == null) {
                    this.buildDate = "localDevel";
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private ServerInfo(ServletContext servletContext) {
            setServletContext(servletContext);
            this.init();
        }

        private void setServletContext(ServletContext servletContext){
            this.servletContext = servletContext;
        }

        public String getVersion(){
            return this.version;
        }

        public String getBuildDate(){
            return this.buildDate;
        }

    }

    public ServerInfoBean() {}

    private static ServerInfo serverInfo;
    private ServletContext servletContext = null;

    public void init() {
      serverInfo = new ServerInfo(this.servletContext);
    }

    public void setServletContext(ServletContext servletContext){
        this.servletContext = servletContext;
    }

    public static String getVersion() {
        return serverInfo.getVersion();
    }

    public static String getBuildDate() {
        return serverInfo.getBuildDate();
    }

}