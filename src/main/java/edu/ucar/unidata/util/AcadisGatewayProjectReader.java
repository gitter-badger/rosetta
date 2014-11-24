package edu.ucar.unidata.util;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lesserwhirls on 2/14/14.
 */
public class AcadisGatewayProjectReader {

    //private String server = "cadis.prototype.ucar.edu";
    private String server = "www.aoncadis.org";
    private int port = 443;
    private String scheme = "https";
    protected static Logger logger = Logger.getLogger(AcadisGatewayProjectReader.class);


    private HttpHost gatewayHost;
    private HttpClientBuilder gatewayClientBuilder;
    private String userAgent;
    private URI uri;
    private HttpResponse latestResponse;
    private String wgetText;
    private Map<String, String> inventory;

    private String dsShortName;

    private HttpHost makeHost() {
        // create client for given host
        HttpHost httpHost = new HttpHost(
                server, port, scheme);

        return httpHost;
    }

    private HttpClientBuilder makeHttpClientBuilder() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        return httpClientBuilder;
    }

    private CloseableHttpClient makeHttpClient() {
        gatewayClientBuilder = makeHttpClientBuilder();

        // finally, build the http client
        CloseableHttpClient httpclient =
                gatewayClientBuilder.build();

        return httpclient;
    }

    private URI makeWgetUri(String dsShortName) throws URISyntaxException {
        URI uri = new URIBuilder()
                .setScheme(gatewayHost.getSchemeName())
                .setHost(gatewayHost.getHostName())
                .setPath("/dataset/" + dsShortName + "/file.wget")
                .build();
        return uri;
    }

    private URI makeDatasetIdUri(String datasetId) throws URISyntaxException {
        URI uri = new URIBuilder()
                .setScheme(gatewayHost.getSchemeName())
                .setHost(gatewayHost.getHostName())
                .setPath("/dataset/id/" + datasetId)
                .build();

        return uri;
    }


    private String makeUserAgent() {
        String rosettaVersion = "0.2-SNAPSHOT";
        String userAgent = "Unidata/Rosetta_"+ rosettaVersion;

        return userAgent;
    }


    private CloseableHttpClient gatewayGet(URI uri) throws IOException {

        CloseableHttpClient httpClient = makeHttpClient();
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("User-Agent", userAgent);

        latestResponse = httpClient.execute(gatewayHost,
                httpGet);

        return httpClient;

    }


    private void createInventory() {
        inventory = new HashMap<>();
        String name, downloadUrl;
        for (String line: wgetText.split("\n")) {
            if (line.contains("logicalFileId")) {
                String[] lineArray = line.split(" ");
                int numParts = lineArray.length;
                if (numParts == 4) {
                    name = lineArray[0];
                    downloadUrl = lineArray[1];
                    name = name.replaceAll("'", "");
                    downloadUrl = downloadUrl.replaceAll("'", "");
                } else {
                    logger.error("Cannot find name...check that format of wget script from the gateway hasn't changed!");
                    name="NO DATA FOUND";
                    downloadUrl="None";
                }

                inventory.put(name, downloadUrl);
            }
        }
    }

    public Map<String, String> getInventory() {
        return inventory;
    }

    public AcadisGatewayProjectReader(String datasetId) {
        String dsShortName = null;
        try {
            init(datasetId);
        } catch (URISyntaxException e) {
            logger.error("Could not create an AcadisGatewayProjectReader for the dataset with a short name of " + dsShortName);
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDatasetShortName(URI datasetUri) {
        String datasetPath = datasetUri.getPath();
        int dsShortNameStart = datasetPath.lastIndexOf("/") + 1;
        int dsShortNameEnd = datasetPath.lastIndexOf(".html");
        String datasetShortName = datasetPath.substring(dsShortNameStart, dsShortNameEnd);

        return datasetShortName;
    }

    private URI resolveDataset(String datasetId) throws IOException {
        List<URI> redirects = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient;
        URI redirectUri = null;
        try {
            URI datasetIdUri = makeDatasetIdUri(datasetId);
            httpClient = makeHttpClient();
            HttpClientContext context = HttpClientContext.create();
            HttpGet httpGet = new HttpGet(datasetIdUri);
            response = httpClient.execute(httpGet, context);
            // get all redirection locations
            redirects = context.getRedirectLocations();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(response != null) {
                response.close();
            }
            if (redirects != null) {
                if (redirects.size() > 0) {
                    redirectUri = redirects.get(0);
                }
            }
        }

        return redirectUri;

    }

    private void init(String datasetId) throws URISyntaxException, IOException {

        userAgent = makeUserAgent();

        gatewayHost = makeHost();

        URI datasetUri = resolveDataset(datasetId);
        dsShortName = getDatasetShortName(datasetUri);

        uri = makeWgetUri(dsShortName);
    }


    public String getDatasetShortName() {
        return this.dsShortName;
    }
    public Boolean read() {
        boolean successful = false;
        try {
            // get request for wget script
            CloseableHttpClient httpClient = gatewayGet(uri);

            int status = latestResponse.getStatusLine().getStatusCode();
            if (status == 200) {
                wgetText = EntityUtils.toString(latestResponse.getEntity());
                httpClient.close();
                createInventory();
                successful = true;

            }
        } catch (Exception exc) {
            logger.debug(exc.getMessage());
        } finally {

            return successful;
        }
    }


    public static void main(String [] args) throws IOException, URISyntaxException {
        // ef653b66-a09f-11e3-b343-00c0f03d5b7c
        String datasetId = "ef653b66-a09f-11e3-b343-00c0f03d5b7c";

        AcadisGatewayProjectReader projectReader = new AcadisGatewayProjectReader(datasetId);
        projectReader.read();
        Map<String, String> inventory = projectReader.getInventory();
        for (String name : inventory.keySet()) {
            String downloadUrl = inventory.get(name);
            System.out.println("name: " + name + " access: " + downloadUrl);
        }
    }


}
