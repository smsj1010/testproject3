package org.ozsoft.xmldb.exist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.ozsoft.xmldb.Collection;
import org.ozsoft.xmldb.Resource;
import org.ozsoft.xmldb.XmldbConnector;
import org.ozsoft.xmldb.XmldbException;

/**
 * Connector for the eXist XML database, using its REST interface. <br />
 * <br />
 * 
 * Implemented using the Apache Commons HttpClient library.
 * 
 * @author Oscar Stigter
 */
public class ExistConnector implements XmldbConnector {
    
    private static final int STATUS_ERROR = 400;

    private static final int ERROR_NOT_FOUND = 404;

    private static final String PARAMETER_STRING_DELIMITER = "\"";

    private static final String PARAMETER_SEPERATOR = ", ";

    private static final int BUFFER_SIZE = 8192;

    private static final Logger LOG = Logger.getLogger(ExistConnector.class);

    private final String servletUri;

    /**
     * Constructor.
     * 
     * @param host
     *            The host running an eXist instance.
     * @param port
     *            The port eXist is running on.
     */
    public ExistConnector(String host, int port) {
        if (host == null || host.length() == 0) {
            throw new IllegalArgumentException("Null or empty host");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port");
        }
        servletUri = String.format("http://%s:%d/exist/rest", host, port);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#retrieveResource(java.lang.String)
     */
    @Override
    public byte[] retrieveResource(String uri) throws XmldbException {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(servletUri + uri);
        byte[] content = null;
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode >= STATUS_ERROR) {
                String msg = String.format("Could not retrieve resource '%s' (HTTP status code: %d)", uri, statusCode);
                throw new XmldbException(msg);
            }
            InputStream is = getMethod.getResponseBodyAsStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int length = 0;
            while ((length = is.read(buffer)) > 0) {
                baos.write(buffer, 0, length);
            }
            content = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            String msg = String.format("Could not retrieve resource '%s'", uri);
            throw new XmldbException(msg, e);
        }
        return content;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.example.exist.XmldbConnector#retrieveXmlDocument(java.lang.String)
     */
    @Override
    public Document retrieveXmlDocument(String uri) throws XmldbException {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(servletUri + uri);
        Document doc = null;
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode >= STATUS_ERROR) {
                if (statusCode == ERROR_NOT_FOUND) {
                    throw new XmldbException(String.format("XML document not found: '%s'", uri));
                } else {
                    throw new XmldbException(String.format("Could not retrieve XML document '%s' (HTTP status code: %d)", uri, statusCode));
                }
            }
            InputStream is = getMethod.getResponseBodyAsStream();
            SAXReader reader = new SAXReader();
            doc = reader.read(is);
        } catch (Exception e) {
            String msg = String.format("Could not retrieve XML document '%s'", uri);
            throw new XmldbException(msg, e);
        }
        return doc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#storeResource(java.lang.String,
     * java.io.File)
     */
    @Override
    public void storeResource(String uri, File file) throws XmldbException {
        try {
            InputStream is = new FileInputStream(file);
            storeResource(uri, is);
        } catch (IOException e) {
            String msg = String.format("Could not store resource '%s'", uri);
            throw new XmldbException(msg, e);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.ozsoft.xmldb.XmldbConnector#importResources(java.lang.String, java.io.File)
     */
    public void importResources(String uri, File dir) throws XmldbException {
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (!name.startsWith(".")) {
                String resourceUri = String.format("%s/%s", uri, name);
                if (file.isDirectory()) {
                    importResources(resourceUri, file);
                } else {
                    storeResource(resourceUri, file);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#storeResource(java.lang.String,
     * org.dom4j.Document)
     */
    @Override
    public void storeResource(String uri, Document doc) throws XmldbException {
        String content = doc.asXML();
        storeResource(uri, content);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#storeResource(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void storeResource(String uri, String content) throws XmldbException {
        InputStream is = new ByteArrayInputStream(content.getBytes());
        storeResource(uri, is);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#storeResource(java.lang.String,
     * java.io.InputStream)
     */
    @Override
    public void storeResource(String uri, InputStream is) throws XmldbException {
        HttpClient httpClient = new HttpClient();
        PutMethod putMethod = new PutMethod(servletUri + uri);
        RequestEntity entity = new InputStreamRequestEntity(is);
        ((EntityEnclosingMethod) putMethod).setRequestEntity(entity);
        try {
            int statusCode = httpClient.executeMethod(putMethod);
            if (statusCode >= STATUS_ERROR) {
                String msg = String.format("Could not store resource '%s' (HTTP status code: %d)", uri, statusCode);
                throw new XmldbException(msg);
            }
        } catch (Exception e) {
            String msg = String.format("Could not store resource '%s'", uri);
            throw new XmldbException(msg, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#deleteResource(java.lang.String)
     */
    @Override
    public void deleteResource(String uri) throws XmldbException {
        HttpClient httpClient = new HttpClient();
        DeleteMethod deleteMethod = new DeleteMethod(servletUri + uri);
        try {
            int statusCode = httpClient.executeMethod(deleteMethod);
            if (statusCode >= STATUS_ERROR) {
                String msg = String.format("Could not delete resource '%s' (HTTP status code: %d)", uri, statusCode);
                throw new XmldbException(msg);
            }
        } catch (Exception e) {
            String msg = String.format("Could not delete resource '%s'", uri);
            throw new XmldbException(msg, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ozsoft.xmldb.XmldbConnector#retrieveCollection(java.lang.String)
     */
    @Override
    public Collection retrieveCollection(String uri) throws XmldbException {
        Collection col = null;
        try {
            Element result = retrieveXmlDocument(uri).getRootElement();
            if (result.getName().equals("result")) {
                Element el = result.element("collection");
                if (el != null) {
                    String name = el.attributeValue("name");
                    col = new Collection(name);
                    for (Object child : el.elements()) {
                        el = (Element) child;
                        name = el.attributeValue("name");
                        Resource res = null;
                        if (el.getName().equals("collection")) {
                            res = new Collection(name);
                        } else {
                            res = new Resource(name);
                        }
                        col.addResource(res);
                    }
                }
            }
        } catch (XmldbException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Collection not found: '%s'", uri));
            }
        }
        return col;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#executeQuery(java.lang.String)
     */
    @Override
    public String executeQuery(String query) throws XmldbException {
        // Build request body.
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<query xmlns=\"http://exist.sourceforge.net/NS/exist\">\n");
        sb.append("  <text><![CDATA[\n");
        sb.append(query).append("\n");
        sb.append("  ]]></text>\n");
        sb.append("  <properties>\n");
        sb.append("    <property name=\"indent\" value=\"yes\"/>\n");
        sb.append("  </properties>\n");
        sb.append("</query>\n");
        String body = sb.toString();
        LOG.trace("POST request:\n" + body);

        // Create POST method.
        HttpClient httpClient = new HttpClient();
        PostMethod postMethod = new PostMethod(servletUri + "/db");
        ByteArrayInputStream bais = new ByteArrayInputStream(body.getBytes());
        RequestEntity entity = new InputStreamRequestEntity(bais, "text/xml; charset=UTF-8");
        ((EntityEnclosingMethod) postMethod).setRequestEntity(entity);

        // Execute method.
        String response = null;
        try {
            int statusCode = httpClient.executeMethod(postMethod);
            if (statusCode >= STATUS_ERROR) {
                String msg = String.format("Could not execute query '%s' (HTTP status code: %d)", query, statusCode);
                throw new XmldbException(msg);
            }

            // Read response body.
            Reader reader = new InputStreamReader(postMethod.getResponseBodyAsStream());
            StringBuilder sb2 = new StringBuilder();
            char[] buffer = new char[BUFFER_SIZE];
            int read = 0;
            while ((read = reader.read(buffer)) > 0) {
                sb2.append(buffer, 0, read);
            }
            reader.close();
            response = sb2.toString();
            LOG.trace("POST response:\n" + response);
        } catch (Exception e) {
            throw new XmldbException("Error while executing query", e);
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    LOG.warn("Could not close stream", e);
                }
            }
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#callModule(java.lang.String,
     * java.util.Map)
     */
    @Override
    public String callModule(String uri, Map<String, String> params) throws XmldbException {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(servletUri + uri);
        if (params != null && params.size() > 0) {
            NameValuePair[] nameValuePairs = new NameValuePair[params.size()];
            int i = 0;
            for (Entry<String, String> param : params.entrySet()) {
                nameValuePairs[i++] = new NameValuePair(param.getKey(), param.getValue());
            }
            getMethod.setQueryString(nameValuePairs);
        }
        String result = null;
        try {
            // Execute query.
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode >= STATUS_ERROR) {
                String msg = String.format("Could not call module '%s' (HTTP status code: %d)", uri, statusCode);
                throw new XmldbException(msg);
            }

            // Read response body.
            Reader reader = new InputStreamReader(getMethod.getResponseBodyAsStream());
            StringBuilder sb2 = new StringBuilder();
            char[] buffer = new char[BUFFER_SIZE];
            int read = 0;
            while ((read = reader.read(buffer)) > 0) {
                sb2.append(buffer, 0, read);
            }
            reader.close();
            result = sb2.toString();
            LOG.trace("Query result:\n" + result);
        } catch (IOException e) {
            String msg = String.format("Error executing XQuery module '%s'", uri);
            throw new XmldbException(msg, e);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.example.exist.XmldbConnector#callFunction(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String[])
     */
    @Override
    public String callFunction(String moduleNamespace, String moduleUri, String functionName, String... params)
            throws XmldbException {
        StringBuilder sb = new StringBuilder();
        final int paramCount = params.length;
        for (int i = 0; i < paramCount; i++) {
            String parameter = params[i];
            sb.append(PARAMETER_STRING_DELIMITER);
            sb.append(parameter);
            sb.append(PARAMETER_STRING_DELIMITER);
            if (i < paramCount - 1) {
                sb.append(PARAMETER_SEPERATOR);
            }
        }
        String paramString = sb.toString();
        String query = String.format(
                "import module namespace tns=\"%s\" at \"xmldb:exist://%s\"; tns:%s(%s)",
                moduleNamespace, moduleUri, functionName, paramString);
        return executeQuery(query);
    }

}
