
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package facebooklinkfinder;

//~--- non-JDK imports --------------------------------------------------------

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//~--- JDK imports ------------------------------------------------------------

//imports in the program
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ajith
 */
public class Facebooklinkfinder {

    /** Encode Format */
    private static final String ENCODE_FORMAT = "UTF-8";

    /** Call Type */
    private static final String callType = "web";

    // Please provide your consumer key here
    private static String consumer_key =
        "dj0yJmk9RFFwYVpaeXJwa29IJmQ9WVdrOU4zWkROa3haTXpBbWNHbzlNVGM1TURBeU1EazJNZy0tJnM9Y29uc3VtZXJzZWNyZXQmeD0xOA--";

    // Please provide your consumer secret here
    private static String       consumer_secret = "bf6cca46d109013c0a0c0da0050a44651936da0b";
    private static final Logger log             = Logger.getLogger(Facebooklinkfinder.class);
    protected static String     yahooServer     = "http://yboss.yahooapis.com/ysearch/";
    private OAuthConsumer       consumer        = null;
    private String              responseBody    = "";

    private static String replaceurl(String content) {
        content = content.replace("/", "%2F");
        content = content.replace(":", "%3A");

        return content;
    }

    public int returnHttpData(String query, BufferedWriter outcsv, String company)
            throws UnsupportedEncodingException, Exception {
        int    status = 0;
        String newquery;
        String params = callType;

        newquery = replaceurl(query);
        params   = params.concat("?q=" + newquery + "&count=5&sites=facebook.com%2Ctwitter.com%2Clinkedin.com");

        String        url      = yahooServer + params;
        OAuthConsumer consumer = new DefaultOAuthConsumer(consumer_key, consumer_secret);

        setOAuthConsumer(consumer);
        URLDecoder.decode(url, ENCODE_FORMAT);
        System.out.println("url" + url);

        int responseCode = sendGetRequest(url);

        homepagelinks(query, outcsv, company);

        return status;
    }

    public int sendGetRequest(String url)
            throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
                   OAuthCommunicationException {
        int responseCode = 500;

        try {
            HttpURLConnection uc = getConnection(url);

            responseCode = uc.getResponseCode();

            if ((200 == responseCode) || (401 == responseCode) || (404 == responseCode)) {
                BufferedReader rd = new BufferedReader(new InputStreamReader((responseCode == 200)
                        ? uc.getInputStream()
                        : uc.getErrorStream()));
                StringBuffer   sb = new StringBuffer();
                String         line;

                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }

                String response = sb.toString();

                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray  ja   = json.getJSONObject("bossresponse").getJSONObject("web").getJSONArray("results");
                    String     str  = "";

                    for (int i = 0; i < ja.length(); i++) {
                        JSONObject j = ja.getJSONObject(i);

                        str = j.getString("url");
                        System.out.println(str);

//                      if (i < 2) {
//                          System.out.print(", ");
//                      }
                    }
                } catch (Exception e) {
                    System.err.println("Something went wrong...");
                    e.printStackTrace();
                }

                rd.close();
                setResponseBody(sb.toString());
            }
        } catch (MalformedURLException ex) {
            throw new IOException(url + " is not valid");
        } catch (IOException ie) {
            throw new IOException("IO Exception " + ie.getMessage());
        }

        return responseCode;
    }

    public HttpURLConnection getConnection(String url)
            throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
                   OAuthCommunicationException {
        try {
            URL               u  = new URL(url);
            HttpURLConnection uc = (HttpURLConnection) u.openConnection();

            if (consumer != null) {
                try {
                    consumer.sign(uc);
                } catch (OAuthMessageSignerException e) {
                    throw e;
                } catch (OAuthExpectationFailedException e) {
                    throw e;
                } catch (OAuthCommunicationException e) {
                    throw e;
                }

                uc.connect();
            }

            return uc;
        } catch (IOException e) {
            log.error("Error signing the consumer", e);

            throw e;
        }
    }

    public void setOAuthConsumer(OAuthConsumer consumer) {
        this.consumer = consumer;
    }

//  private static void print(String msg, Object... args) {
//      System.out.println(String.format(msg, args));
//  }
//
//  private static String trim(String s, int width) {
//      if (s.length() > width)
//          return s.substring(0, width-1) + ".";
//      else
//          return s;
//  }
    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        if (null != responseBody) {
            this.responseBody = responseBody;
        }
    }

    private int fbtrueorfalse(String linkabshref, String company) {
        int flag     = 0;
        int priority = 0;

        System.out.println("linkabshref = " + linkabshref);
        linkabshref = linkabshref.toLowerCase();

        if (linkabshref.contains("facebook.com/" + company)) {
            flag     = 1;
            priority = 1;
        } else if (linkabshref.contains("facebook.com/pages/" + company)) {
            flag     = 1;
            priority = 0;
        }

        flag = flag + priority;

        return flag;
    }

    private int linktrueorfalse(String linkabshref, String company) {
        int flag     = 0;
        int priority = 0;

        if (linkabshref.contains("linkedin.com/company/" + company)) {
            priority = 1;
            flag     = 1;
        }

        flag = flag + priority;

        return flag;
    }

    private int twttrueorfalse(String linkabshref, String company) {
        int flag     = 0;
        int priority = 0;

        if (linkabshref.contains("twitter.com/" + company)) {
            priority = 1;
            flag     = 1;
        }

        flag = flag + priority;

        return flag;
    }

    public void homepagelinks(String url1, BufferedWriter outcsv, String company)
            throws UnsupportedEncodingException, Exception {
        outcsv.flush();
        System.err.println("String url = " + url1);

        Document doc   = Jsoup.connect(url1).get();
        Elements links = doc.select("a[href]");
        Document doc1  = Jsoup.parse(url1);
        int      i     = 0;

        for (Element link : links) {

            // System.out.println("link: " );
            // System.out.println(link);
            String linkabshref = link.attr("abs:href");

            // String newlinkabshref = linkabshref.toLowerCase();
            // System.out.println(newlinkabshref);
            if (linkabshref.contains("facebook.com/")) {
                System.out.println(linkabshref);
                outcsv.append(linkabshref + ", ");

                int result = fbtrueorfalse(linkabshref.toLowerCase(), company);

                if (result == 2) {
                    outcsv.append("true" + ", " + "high" + ", ");
                } else if (result == 1) {
                    outcsv.append("true" + ", " + "low" + ", ");
                } else if (result == 0) {
                    outcsv.append("false" + ", " + "nil" + ", ");
                }
            }

            if (linkabshref.contains("twitter.com/")) {
                System.out.println(linkabshref);
                outcsv.append(linkabshref + ", ");

                int result = twttrueorfalse(linkabshref.toLowerCase(), company);

                if (result == 2) {
                    outcsv.append("true" + ", " + "high" + ", ");
                } else if (result == 1) {
                    outcsv.append("true" + ", " + "low" + ", ");
                } else if (result == 0) {
                    outcsv.append("false" + ", " + "nil" + ", ");
                }
            }

            if ((linkabshref.contains("www.linkedin.com/"))) {
                System.out.println(linkabshref);
                outcsv.append(linkabshref + ", ");

                int result = linktrueorfalse(linkabshref.toLowerCase(), company);

                if (result == 2) {
                    outcsv.append("true" + ", " + "high" + ", ");
                } else if (result == 1) {
                    outcsv.append("true" + ", " + "low" + ", ");
                } else if (result == 0) {
                    outcsv.append("false" + ", " + "nil" + ", ");
                }
            }
        }

        outcsv.append("\n");

        return;
    }

    public static void main(String[] args) throws IOException, UnsupportedEncodingException {
        BasicConfigurator.configure();

        BufferedReader input  = new BufferedReader(new FileReader("input.txt"));
        BufferedWriter outcsv = new BufferedWriter(new FileWriter("outout.csv"));

        try {
            String content = "";

            outcsv.write("");

            while ((content = input.readLine()) != null) {
                String url = content;

                
                System.out.println(url);

                String company = null;

                String protocol = "http://www.";

                if ((content.startsWith("http://www.")) != true) {
                    content = protocol.concat(content);
                }

                if (content.startsWith("http://www.") || (content.startsWith("www.")) || (content.endsWith(".com"))
                        || (content.endsWith(".com")) || (content.endsWith(".in")) || (content.endsWith(".co.in"))) {
                    company = content.replace("http://www.", "");
                }

                company = company.replace(".com", "");
                company = company.replace(".in", "");
                company = company.replace(".co.in", "");
                company = company.replace(".org", "");
                System.out.println(content);
                System.out.println(company);
                outcsv.append(content + ", ");
                outcsv.append(company + ", ");

                Facebooklinkfinder facebooklinkfinder = new Facebooklinkfinder();

                facebooklinkfinder.returnHttpData(content, outcsv, company);
            }

            outcsv.close();
        } catch (Exception e) {
            System.out.println("something went wrong.........");
            System.out.println(e);
        }

        input.close();
        outcsv.close();
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
