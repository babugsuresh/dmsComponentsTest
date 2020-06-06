package hmrc.cds.dms;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.ibm.wsdl.PortTypeImpl;
import org.apache.commons.*;

@Controller
public class DmsComponentsTestController {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private static final String BASEDIR = null;
	private static final String BASE_URL = "http://10.102.81.254:5558";
	private static final String USERNAME = "7898083";
	private static final String PASSWORD = "Syamala@2432";
	private static final String ENCODING = "utf-8";

	InputStream is = getClass().getResourceAsStream("/application.yml");

	Yaml yaml = new Yaml();
	// Reader yamlFile = new FileReader("src/main/resources/application.yml");
	Reader yamlFile = new InputStreamReader(is);

	@SuppressWarnings("unchecked")
	Map<String, Object> yamlMaps = (Map<String, Object>) yaml.load(yamlFile);
	static List<QueuesDataBean> qdb;

	List<FinalQueuesStatusReport> frb = new ArrayList<FinalQueuesStatusReport>();

	static {

		qdb = new ArrayList<QueuesDataBean>();

		qdb.add(new QueuesDataBean("DMS.QA.EXT_PARTY_VALIDATION_IN", "TMM(Tariff)", "Inbound", "Q"));
		qdb.add(new QueuesDataBean("DMS.QA.EXT_PARTY_VALIDATION_IN_BO", "TMM(Tariff)", "", "BO"));
		qdb.add(new QueuesDataBean("DMS.QA.EXT_PARTY_VALIDATION_OUT", "TMM(Tariff)", "Outbound", "Q"));
		qdb.add(new QueuesDataBean("DMS.QA.EXT_PARTY_VALIDATION_OUT_BO", "TMM(Tariff)", "", "BO"));

		qdb.add(new QueuesDataBean("DMS.QA.ADMIN_FIN_OBLIGATION_RCV", "IPS-ETMP", "Inbound", "Q"));
		qdb.add(new QueuesDataBean("DMS.QA.ADMIN_FIN_OBLIGATION_RCV_BO", "IPS-ETMP", "", "BO"));
		qdb.add(new QueuesDataBean("DMS.QA.ADMIN_FIN_OBLIGATION", "IPS-ETMP", "Outbound", "Q"));
		qdb.add(new QueuesDataBean("DMS.QA.ADMIN_FIN_OBLIGATION_BO", "IPS-ETMP", "", "BO"));

		qdb.add(new QueuesDataBean("DMS.QA.QUOTA_MGMT", "Tariff(Quota)", "Inbound", "Q"));
		qdb.add(new QueuesDataBean("DMS.QA.QUOTA_MGMT_BO", "Tariff(Quota)", "", "BO"));
		qdb.add(new QueuesDataBean("DMS.QA.QUOTA_MGMT_OUT", "Tariff(Quota)", "Outbound", "Q"));
		qdb.add(new QueuesDataBean("DMS.QA.QUOTA_MGMT_OUT_BO", "Tariff(Quota)", "", "BO"));

		qdb.add(new QueuesDataBean("DMS.QA.NOTIF_CASH_DEPOSIT", "IPS*", "Inbound", "Q"));
		qdb.add(new QueuesDataBean("DMS.QA.NOTIF_CASH_DEPOSIT_BO", "IPS*", "", "BO"));

	}

	private static String getContentRestUrl(final Long contentId, final String[] expansions)
			throws UnsupportedEncodingException {
		final String expand = URLEncoder.encode(StringUtils.join(expansions, ","), ENCODING);

		return String.format("%s/rest/api/content/%s?expand=%s&os_authType=basic&os_username=%s&os_password=%s",
				BASE_URL, contentId, expand, URLEncoder.encode(USERNAME, ENCODING),
				URLEncoder.encode(PASSWORD, ENCODING));
	}

	@GetMapping("/test")
	public String test(@RequestParam(name = "name", required = false, defaultValue = "World") String name,
			Model model) {
		model.addAttribute("name", name);
		return "verifydmscomponents";
	}

	public String generateJMSHTMLData() {

		System.out.println("Started Executing generateJMSHTMLData");

		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> JMSEnvironments = (List<Map<String, Object>>) yamlMaps.get("JMSEnvironments");

		List<FinalQueuesStatusReport> frb = new ArrayList<FinalQueuesStatusReport>();
		String key = null;
		Object value = null;
		for (Map<String, Object> entry : JMSEnvironments) {
			for (Map.Entry<String, Object> innerentry : entry.entrySet()) {
				key = innerentry.getKey();
				value = innerentry.getValue();

				int x = 0;
				FinalQueuesStatusReport fqsr = doPCF(key, value);
				frb.add(fqsr);
			}

			System.out.println("key: " + key + ", value: " + value);
		}

		System.out.println("List of FinalQueuesStatusReport: " + frb);

		String h1 = "<table style=\"width:100%\">\r\n" + "  <tr>\r\n" + "    <th>Environment</th>";

		String h2 = "</tr>\r\n" + "  <tr>\r\n" + "    <td></td>";

		String x = "";
		String y = "";
		String z = "";
		String a = "";
		String b = "";
		StringBuilder builder = new StringBuilder();
		String data = "";

		for (FinalQueuesStatusReport rp : frb) {

			if (!(rp.getQheader() == null)) {

				// Removing duplicates
				Set<String> s = new LinkedHashSet<String>(rp.getQheader().getSystemNames());

				for (String systemNames : s) {

					if (systemNames.endsWith("*")) {
						x = x + "<th colspan=\"2\" style=\"text-align:center\">"
								+ systemNames.substring(0, systemNames.length() - 1) + "</th>";
					} else {
						x = x + "<th colspan=\"4\" style=\"text-align:center\">" + systemNames + "</th>";
					}

				}

				for (String typeOfQueue : rp.getQheader().getTypeOfQueue()) {
					if (!typeOfQueue.isEmpty()) {

						y = y + "<th colspan=\"2\" style=\"text-align:center\">" + typeOfQueue
								+ "</th>";

					}

				}

				for (String types : rp.getQheader().getType()) {

					z = z + "<th>" + types + "</th>";
				}
			}
			if (!(rp.getqDepthStatus() == null)) {
				for (QueuesDepthStatus s : rp.getqDepthStatus()) {
					
					boolean down = false;

					for (Integer i : s.getDepths()) {

						String ss = String.valueOf(i);

						if (ss == String.valueOf(i)) {

							b = b + "<td></td>";
							down = true;

						} else {

							if (i > 0) {
								b = b + "<td class=\"highlight-red\" data-highlight-colour=\"red\"><b>" + i
										+ "</b></td>";
							} else {
								b = b + "<td>" + i + "</td>";
							}

						}

					}
					
					if(down) {
						String c = "<ac:emoticon ac:name=\"cross\" />";
						a = "<td><b>" + s.envName + "</b>&nbsp;"+c+"</td>" + b + "</tr>\r\n" + "   <tr>";
					}else {
						String c = "<ac:emoticon ac:name=\"tick\" />";
						a = "<td><b>" + s.envName + "</b>&nbsp;"+c+"</td>" + b + "</tr>\r\n" + "   <tr>";
					}

					

					builder.append(a);
					b = "";

				}
			}

		}

		data = builder.toString();

		String h3 = "</tr>  \r\n" + "   <tr>\r\n";

		String finalHML = h1 + x + h2 + y + h2 + z + h3 + data + "</tr>";
		// System.out.println(finalHML);

		return finalHML;

	}

	/**
	 * Handle connecting to the queue manager, issuing PCF command then looping
	 * through PCF response messages and disconnecting from the queue manager.
	 */
	private FinalQueuesStatusReport doPCF(String key, Object obj) {
		
		MQQueueManager qMgr = null;

		PCFMessageAgent agent = null;
		PCFMessage request = null;
		PCFMessage[] responses = null;
		Map<String, Integer> queueNameDepth = new HashMap<String, Integer>();

		Hashtable<String, Object> mqht = new Hashtable<String, Object>();

		mqht.put(CMQC.CHANNEL_PROPERTY, "DMSQM.SVRCONN");
		mqht.put(CMQC.HOST_NAME_PROPERTY, obj);
		mqht.put(CMQC.PORT_PROPERTY, 1414);

		String qMgrName = "DMSQM";

		FinalQueuesStatusReport fqsr = new FinalQueuesStatusReport();
		frb.add(fqsr);

		QueuesDepthStatus qds = new QueuesDepthStatus();
		List<QueuesDepthStatus> qDepthStatus = new ArrayList<QueuesDepthStatus>();
		List<Integer> depths = new ArrayList<Integer>();
		try {

			qMgr = new MQQueueManager(qMgrName, mqht);
			log.debug("successfully connected to " + qMgrName);

			agent = new PCFMessageAgent(qMgr);
			log.debug("successfully created agent");

			// https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_9.1.0/com.ibm.mq.ref.adm.doc/q087800_.htm
			request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);

			/**
			 * You can explicitly set a queue name like "TEST.Q1" or use a wild card like
			 * "TEST.*"
			 */
			request.addParameter(CMQC.MQCA_Q_NAME, "DMS.*");

			// Add parameter to request only local queues
			request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);

			// Add parameter to request only queue name and current depth
			request.addParameter(CMQCFC.MQIACF_Q_ATTRS, new int[] { CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH });

			responses = agent.send(request);

			for (int i = 0; i < responses.length; i++) {
				if (((responses[i]).getCompCode() == CMQC.MQCC_OK)
						&& ((responses[i]).getParameterValue(CMQC.MQCA_Q_NAME) != null)) {
					String name = responses[i].getStringParameterValue(CMQC.MQCA_Q_NAME);
					if (name != null)
						name = name.trim();

					int depth = responses[i].getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH);
					queueNameDepth.put(name, depth);

				}
			}

			QueuesHeader qheader = new QueuesHeader();

			List<String> systemNames = new ArrayList<String>();
			List<String> typeOfQueue = new ArrayList<String>();
			List<String> type = new ArrayList<String>();

			for (QueuesDataBean qd : qdb) {
				for (Map.Entry<String, Integer> entry : queueNameDepth.entrySet()) {

					if (entry.getKey().equalsIgnoreCase(qd.getQueueName())) {

						if (key.equalsIgnoreCase("DIT1")) {
							systemNames.add(qd.getSystemName());
							typeOfQueue.add(qd.getType());

							type.add(qd.qType);
							qheader.setType(type);

							qheader.setSystemNames(systemNames);
							qheader.setTypeOfQueue(typeOfQueue);

							fqsr.setQheader(qheader);
						}

						depths.add(entry.getValue());

					}

				}
			}
			qds.setDepths(depths);
			qDepthStatus.add(qds);
			qds.setEnvName(key);
			fqsr.setqDepthStatus(qDepthStatus);

		} catch (MQException e) {
			log.error("CC=" + e.completionCode + " : RC=" + e.reasonCode);

			//Handle null depths scenario and JMS server down scenario
			Integer x = null;
			if (qdb.size() > 0) {
				for (int i = 0; i < qdb.size(); i++) {
					depths.add(x);
				}

			}
			qds.setDepths(depths);
			qDepthStatus.add(qds);
			qds.setEnvName(key);
			fqsr.setqDepthStatus(qDepthStatus);

		} catch (IOException e) {
			log.error("IOException:" + e.getLocalizedMessage());
		} catch (MQDataException e) {
			log.error("MQDataException:" + e.getLocalizedMessage());
		} finally {
			try {
				if (agent != null) {
					agent.disconnect();
					log.debug("disconnected from agent");
				}
			} catch (MQDataException e) {
				log.error("CC=" + e.completionCode + " : RC=" + e.reasonCode);
			}

			try {
				if (qMgr != null) {
					qMgr.disconnect();
					log.debug("disconnected from " + qMgrName);
				}
			} catch (MQException e) {
				log.error("CC=" + e.completionCode + " : RC=" + e.reasonCode);
			}
		}
		return fqsr;
	}

	@SuppressWarnings("deprecation")
	@Scheduled(cron = "0 0 8-19 * * MON-FRI") // on the hour 8AM-to-7PM weekdays
	@GetMapping("/publishConfluenceData")
	public String publishConfluenceData() throws ClientProtocolException, IOException, JSONException {

		final long pageId = 50430757;

		@SuppressWarnings("deprecation")
		HttpClient client = new DefaultHttpClient();

		// Get current page version
		String pageObj = null;
		HttpEntity pageEntity = null;
		try {

			String temp = getContentRestUrl(pageId, new String[] { "body.storage", "version", "ancestors" });
			System.out.println("Final URL: " + temp);

			HttpGet getPageRequest = new HttpGet(
					getContentRestUrl(pageId, new String[] { "body.storage", "version", "ancestors" }));

			getPageRequest.addHeader(
					BasicScheme.authenticate(new UsernamePasswordCredentials("user", "password"), "UTF-8", false));

			HttpResponse getPageResponse = client.execute(getPageRequest);
			pageEntity = getPageResponse.getEntity();
			pageObj = IOUtils.toString(pageEntity.getContent());

			// System.out.println("Get Page Request returned " +
			// getPageResponse.getStatusLine().toString());
			// System.out.println("");
			// System.out.println(pageObj);
		} finally {
			if (pageEntity != null) {
				EntityUtils.consume(pageEntity);
			}
		}

		// Parse response into JSON
		JSONObject page = new JSONObject(pageObj);

		List<ReportBean> rbs = getReportBeanData("all");

		// System.out.println("Printing from publishConfluenceData: "+rb);

		String jmsHTML = generateJMSHTMLData();

		String h1 = "<table>\r\n" + "<tr>\r\n" + "<th>Emotion</th>\r\n" + "<th>Details</th>\r\n" + "</tr>\r\n"
				+ "<tr>\r\n" + "<td><ac:emoticon ac:name=\"tick\" /></td>\r\n" + "<td>System is Up and Running</td>\r\n"
				+ "</tr>\r\n" + "<tr>\r\n" + "<td><ac:emoticon ac:name=\"cross\" /></td>\r\n"
				+ "<td>Application or Queue Server is Down</td>\r\n" + "</tr>\r\n" + "<tr>\r\n"
				+ "<td><ac:emoticon ac:name=\"information\" /></td>\r\n"
				+ "<td>Fault Response Received try with different data,<p>For More Details use <a href=\"https://dmgr.dmsplat3.n.cit.corp.hmrc.gov.uk:9444/dmsApp\">DMS Components App</a> from D4D.</p></td>\r\n"
				+ "</tr>\r\n"
				+ "</table><H1 style=\"text-align: center;\"><b>HTTP/s Integrated End Points Availability</b></H1><table>\r\n"
				+ "   <tr>\r\n" + "      <th>SYSTEM NAME</th>\r\n" + "      <th>SERVICE NAME</th>\r\n"
				+ "      <th>OPERATION NAME</th>";

		String x = null;
		String y = "";
		StringBuilder headerBuilder = new StringBuilder();
		StringBuilder builder1 = new StringBuilder();

		List<String> ls = new ArrayList<String>();
		List<String> ls1 = new ArrayList<String>();

		for (ReportBean rb : rbs) {

			StringBuilder localBuilder = new StringBuilder();
			String envs = "<th>" + rb.getEnvName() + "</th>";
			// System.out.println("ENv Names: " + rb.getEnvName());
			for (ServiceName sn : rb.getServiceNames()) {
				// System.out.println("ServiceName inside: "+sn.getServiceName()+", env name:
				// "+rb.getEnvName());

				/*
				 * if(sn.getOperationNames().isEmpty()) { String status =
				 * "<ac:emoticon ac:name=\"cross\" />"; y = "\n<td>" + status + "</td>"; for
				 * (String s : ls) { String k = s + y; ls1.add(k); ls.remove(s); break; } }
				 */

				for (OperationName on : sn.getOperationNames()) {
					// System.out.println("OperationName inside: "+on.getOperationName()+", env
					// name: "+rb.getEnvName());

					if (!rb.getEnvName().equalsIgnoreCase("DIT1")) {
						String status = null;
						if (on.getStatus().equalsIgnoreCase("Running")) {
							status = "<ac:emoticon ac:name=\"tick\" />";
						} else if (on.getStatus().equalsIgnoreCase("Fault")) {
							status = "<ac:emoticon ac:name=\"information\" />";
						} else {
							status = "<ac:emoticon ac:name=\"cross\" />";
						}
						y = "\n<td>" + status + "</td>";
						for (String s : ls) {
							String k = s + y;
							ls1.add(k);
							ls.remove(s);
							break;
						}
					} else {
						// System.out.println("Indise DIT1");
						String status = null;
						if (on.getStatus().equalsIgnoreCase("Running")) {
							status = "<ac:emoticon ac:name=\"tick\" />";
						} else if (on.getStatus().equalsIgnoreCase("Fault")) {
							status = "<ac:emoticon ac:name=\"information\" />";
						} else {
							status = "<ac:emoticon ac:name=\"cross\" />";
						}

						x = "\n</tr>\r\n" + "   <tr>\r\n" + "      <td><b>" + sn.getSystemName() + "</b></td>\r\n"
								+ "      <td>"
								+ sn.getServiceName().substring(0, sn.getServiceName().lastIndexOf("."))
										.replaceAll("\\SOAP.*?\\b", "")
								+ "</td>\r\n" + "      <td>" + on.getOperationName() + "</td>\r\n" + "	  <td>" + status
								+ "</td>";

						ls.add(x);
					}

					localBuilder.append(x);
				}

			}

			// System.out.println("Listof elements: "+ls1);
			ls.addAll(ls1);
			ls1.removeAll(ls1);
			builder1.append(envs);

		}
		headerBuilder.append(builder1.toString());

		StringBuilder rowsBuilder = new StringBuilder();
		String xx = "";
		StringBuilder localBuilder = new StringBuilder();

		for (String ss : ls) {
			xx = ss;
			localBuilder.append(xx);
		}
		xx = localBuilder.toString();
		rowsBuilder.append(xx);

		String s1 = "</tr></table><H1 style=\"text-align: center;\"><b>JMS Queues Status, Depth and Availability</b> (<i>Coming Soon..</i>)</H1>";

		String s3 = "<H1>Appendix A: </H1><table style=\"width:500px\">\r\n" + "  <tr>\r\n" + "    <th>Acronym</th>\r\n"
				+ "    <th>What is Stands For</th>\r\n" + "    <th>Other Relevant Info</th>\r\n" + "  </tr>\r\n"
				+ "  <tr>\r\n" + "    <td>XRS</td>\r\n" + "    <td>Exchange Rate Service</td>\r\n" + "    <td></td>\r\n"
				+ "  </tr>\r\n" + "  <tr>\r\n" + "    <td>ILMS</td>\r\n"
				+ "    <td>Internal License Management System	</td>\r\n" + "    <td></td>\r\n" + "  </tr>\r\n"
				+ "  <tr>\r\n" + "    <td>CDCM</td>\r\n" + "    <td>Customs Declaration Case Management	</td>\r\n"
				+ "    <td></td>\r\n" + "  </tr>\r\n" + "  <tr>\r\n" + "    <td>ILE</td>\r\n"
				+ "    <td>Inventory Linking Environment/Exports</td>\r\n" + "    <td></td>\r\n" + "  </tr>\r\n"
				+ "  <tr>\r\n" + "    <td>SPS</td>\r\n" + "    <td>Securing Payment Services</td>\r\n"
				+ "    <td></td>\r\n" + "  </tr>\r\n" + "  <tr>\r\n" + "    <td>IPS</td>\r\n"
				+ "    <td>Immediate Payment Service</td>\r\n" + "    <td></td>\r\n" + "  </tr>\r\n" + "  <tr>\r\n"
				+ "    <td>PDS</td>\r\n" + "    <td>Party Data Service	</td>\r\n" + "    <td></td>\r\n" + "  </tr>\r\n"
				+ "  <tr>\r\n" + "    <td>RDS</td>\r\n" + "    <td>Reference Data Service	</td>\r\n"
				+ "    <td></td>\r\n" + "  </tr>\r\n" + "  <tr>\r\n" + "    <td>Q</td>\r\n"
				+ "    <td>Regular Queue</td>\r\n" + "    <td></td>\r\n" + "  </tr>\r\n" + "  <tr>\r\n"
				+ "    <td>BO</td>\r\n" + "    <td>Backout Queue</td>\r\n" + "    <td></td>\r\n" + "  </tr>\r\n"
				+ "  <tr>\r\n" + "    <td>ETMP</td>\r\n" + "    <td>Enterprise Tax Management Platform</td>\r\n"
				+ "    <td></td>\r\n" + "  </tr>\r\n" + "  <tr>\r\n" + "    <td>DIS</td>\r\n"
				+ "    <td>Declaration Information Service	</td>\r\n" + "    <td></td>\r\n" + "  </tr>\r\n"
				+ "</table>";

		String s2 = "</table>" + s3
				+ "<p>**This page updates on <b>Hourly</b> basis (from 8AM-to-7PM weekdays), if you are looking for a realtime data please launch<a href=\"https://dmgr.dmsplat3.n.cit.corp.hmrc.gov.uk:9444/dmsApp/publishConfluenceData\" >Publish Confluence Data</a> from D4D.</p><p>**For more details about DMS MQ Queues - <a href=\"http://10.102.81.254:8090/display/CDOS/DMS+MQ+Queue+Definitions\" >DMS MQ Queue Definitions</a></p>";

		String finalHTML = h1 + headerBuilder.toString() + rowsBuilder.toString() + s1 + jmsHTML + s2;

		System.out.println(finalHTML);

		page.getJSONObject("body").getJSONObject("storage").put("value", finalHTML);

		int currentVersion = page.getJSONObject("version").getInt("number");
		page.getJSONObject("version").put("number", currentVersion + 1);

		// Send update request
		HttpEntity putPageEntity = null;

		try {
			HttpPut putPageRequest = new HttpPut(getContentRestUrl(pageId, new String[] {}));

			StringEntity entity = new StringEntity(page.toString(), ContentType.APPLICATION_JSON);
			putPageRequest.setEntity(entity);

			HttpResponse putPageResponse = client.execute(putPageRequest);
			putPageEntity = putPageResponse.getEntity();

			// System.out.println("Put Page Request returned : \n" +
			// page.getJSONObject("body").getJSONObject("storage").getString("value"));
			// System.out.println("");
			System.out.println(IOUtils.toString(putPageEntity.getContent()));
		} finally {
			EntityUtils.consume(putPageEntity);
		}
		return "success";

	}

	private List<ReportBean> getReportBeanData(String envSel) {
		String envSelected = envSel;

		// Yaml yaml = new Yaml();
		// Reader yamlFile = new FileReader("src/main/resources/application.yml");
		// Reader yamlFile = new InputStreamReader(is);

		// @SuppressWarnings("unchecked")
		// Map<String, Object> yamlMaps = (Map<String, Object>) yaml.load(yamlFile);

		@SuppressWarnings("unchecked")
		List<String> envs = (List<String>) yamlMaps.get("Environments");

		System.out.println("\n----JESUS is my GOD----" + envs.toString());

		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> systemMapping = (List<Map<String, Object>>) yamlMaps.get("SystemMapping");

		List<OperationName> operationNames = new ArrayList<OperationName>();
		List<ServiceName> serviceNames = new ArrayList<ServiceName>();
		List<ReportBean> rb = new ArrayList<ReportBean>();

		// Tempt list to ensure wsdls are not called again
		List<String> list = new ArrayList<String>();

		String endpoint = null;
		String systemtocall = null;
		boolean loopBack = false;
		boolean oneenv = false;
		int index = 0;

		// InputStream wsdlPath = getContextClassLoader().getResourceAsStream("/wsdls/99
		// Assembled adapters 3.2.8.11");

		// File fileD = ResourceUtils.getFile("classpath:/wsdls/99 Assembled adapters
		// 3.2.8.11");

		// InputStream is = getClass().getResourceAsStream("/application.yml");
		// InputStream filex = this.getClass().getResourceAsStream("/wsdls/99 Assembled
		// adapters 3.2.8.11");
		URL dirUrl = getClass().getResource("/wsdls/99 Assembled adapters 3.2.8.11");
		// URL url = DmsComponentsTestController.class.getResource("resources/wsdls/99
		// Assembled adapters 3.2.8.11/");
		if (dirUrl == null) {
			log.info("No WSDL path provided");
		} else {
			// File dir = new File(url.toURI());
			File directory = null;
			try {
				directory = new File(dirUrl.toURI());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			File wsdlDir = null;
			File file;

			// Setting only one env selected
			if (envs.contains(envSelected)) {
				log.info("Env Selected: " + envSelected + ", Index for Selected: " + envs.indexOf(envSelected));
				oneenv = true;
			}

			exitloop: for (String env : envs) {

				// Setting selected env and its index for rite end point call purpose
				if (oneenv) {
					index = envs.indexOf(envSelected);
					env = envSelected;
				}

				ReportBean reportBean = new ReportBean();

				// when it comes back for new env clear this
				if (!list.isEmpty()) {
					list.removeAll(list);
				}

				// iterate at nested dir level
				Iterator<File> iter = FileUtils.iterateFiles(directory, new String[] { "wsdl" }, true);
				while (iter.hasNext()) {

					file = (File) iter.next();
					wsdlDir = new File(BASEDIR, file.getParent());

					// iterate at wsdl dir level and look for wsdl only and no more traverse through
					// wsdl dir
					Iterator<File> itr = FileUtils.iterateFiles(wsdlDir, new String[] { "wsdl" }, false);
					while (itr.hasNext()) {
						file = (File) itr.next();
						File wsdlFile = new File(wsdlDir, file.getPath());
						if (!list.contains(wsdlFile.getName())) {

							outerloop: for (Map<String, Object> featureService : systemMapping) {
								for (Map.Entry<String, Object> entry : featureService.entrySet()) {
									String key = entry.getKey();
									if (wsdlFile.getName().equalsIgnoreCase(key)) {
										String value = (String) entry.getValue();
										systemtocall = value;
										list.add(wsdlFile.getName());
										loopBack = false;
										break outerloop;

									} else {
										loopBack = true;
									}
								}
							}

							if (!loopBack) {
								log.info("Env Executing for: " + env + ", System to call for: " + wsdlFile.getName()
										+ ", WSDL Service is: " + systemtocall + ", index of environment: "
										+ envs.indexOf(env));

								// one env selected scenario
								if (oneenv) {
									endpoint = getEndPoint(env, index, systemtocall, yamlMaps);
								} else {
									endpoint = getEndPoint(env, envs.indexOf(env), systemtocall, yamlMaps);
								}

								ServiceName services = new ServiceName();
								if (!(endpoint == null)) {
									String systemHostURI = systemtocall + "URI";
									endpoint = endpoint + yamlMaps.get(systemHostURI);
									log.info("Endpoint URL to call for: " + systemtocall + " = " + endpoint);

									List<Operation> operationList = getPortTypeOperations(file.getPath());

									log.info("Total Number of operations in " + wsdlFile.getName() + " : "
											+ operationList.size());

									try {
										operationNames = getOperations(operationList, endpoint);
									} catch (UnsupportedOperationException | SOAPException | IOException
											| URISyntaxException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

								} else {
									// log.info("No External System End Points are Configured for: " +
									// systemtocall);
								}

								services = new ServiceName();
								if (systemtocall.equalsIgnoreCase("ILMS")) {
									services.setSystemName("AuthService");
								} else {
									services.setSystemName(systemtocall);
								}

								services.setServiceName(wsdlFile.getName());
								services.setOperationNames(operationNames);
								serviceNames.add(services);

							} else {
								// log.info("No External System is configured for WSDL: " + wsdlFile.getName());
							}

						}

					}

				}

				// Just to avoid duplicate adding
				List<ServiceName> tempServiceList = new ArrayList<ServiceName>();
				tempServiceList.addAll(serviceNames);

				reportBean = new ReportBean();
				reportBean.setEnvName(env);
				reportBean.setServiceNames(tempServiceList);

				// exit main env level loop if one env only selected
				if (envSelected.equalsIgnoreCase("all")) {
					rb.add(reportBean);
				} else {
					rb.add(reportBean);
					break exitloop;
				}

				serviceNames.removeAll(serviceNames);

			}

		}
		// log.info("Final ReportBean: " + rb);

		return rb;

	}

	@GetMapping("/verifydmscomponents")
	public String verifydmscomponents(
			@RequestParam(name = "name", required = false, defaultValue = "all") String envSelected, Model model)
			throws IOException, UnsupportedOperationException, SOAPException, URISyntaxException {

		List<ReportBean> rb = getReportBeanData(envSelected);
		// Now form a HTML data based on final ReportBean
		String body = generateHTMLfile(rb);

		// Pass this HTML data to Thymeleaf html page
		model.addAttribute("body", body);
		// publishConfluenceData();
		return "report";

	}

	private String generateHTMLfile(List<ReportBean> rb) throws IOException {

		String body = "";

		String tag1 = "<section>  <h1>";
		String tag2 = "</h1><div class=\"tbl-header\">\r\n"
				+ "    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\r\n" + "      <thead>\r\n"
				+ "        <tr><b>\r\n" + "          <th><b>System Name<b/></th>\r\n"
				+ "          <th><b>Service Name<b/></th>\r\n" + "          <th><b>Operation Name<b/></th>\r\n"
				+ "          <th><b>Status<b/></th>\r\n" + "          <th><b>Response XML<b/></th>\r\n"
				+ "        </tr>\r\n" + "      </thead>\r\n" + "    </table>\r\n" + "  </div>\r\n"
				+ "  <div class=\"tbl-content\">\r\n"
				+ "    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\r\n" + "      <tbody>";

		String tag3 = "</tbody>\r\n" + "    </table>\r\n" + "  </div>\r\n" + "</section>";

		String tag4 = "<tr><td>";
		String tag5 = "</td></tr>";
		String tag6 = "</td><td>";
		String green = "<span class=\"label success\">";
		String red = "<span class=\"label danger\">";
		String tag7 = "</span>";

		String tag8 = "<button type=\"button\" class=\"collapsible\">";

		// To Make response button to be clickable
		String js = "<script>\r\n" + "var coll = document.getElementsByClassName(\"collapsible\");\r\n" + "var i;\r\n"
				+ "\r\n" + "for (i = 0; i < coll.length; i++) {\r\n"
				+ "  coll[i].addEventListener(\"click\", function() {\r\n"
				+ "    this.classList.toggle(\"active\");\r\n" + "    var content = this.nextElementSibling;\r\n"
				+ "    if (content.style.display === \"block\") {\r\n" + "      content.style.display = \"none\";\r\n"
				+ "    } else {\r\n" + "      content.style.display = \"block\";\r\n" + "    }\r\n" + "  });\r\n"
				+ "}\r\n" + "</script>";

		StringBuilder builder = new StringBuilder();

		for (ReportBean r : rb) {
			String toPublish = "";
			String dataToLoad = "";
			StringBuilder localBuilder = new StringBuilder();
			for (ServiceName sn : r.getServiceNames()) {
				for (OperationName op : sn.getOperationNames()) {
					String sname = sn.getServiceName().substring(0, sn.getServiceName().lastIndexOf("."))
							.replaceAll("\\SOAP.*?\\b", "");
					String color = null;
					if (op.getStatus().equalsIgnoreCase("Running")) {
						color = green + op.getStatus() + tag7;
					} else {
						color = red + op.getStatus() + tag7;
					}
					String cc = "<div class=\"content\">Testing</div>";
					dataToLoad = tag4 + "<b>" + sn.getSystemName() + "</b>" + tag6 + sname + tag6
							+ op.getOperationName() + tag6 + color + tag6 + tag8 + "Response" + "</button>" + cc + tag5;
					localBuilder.append(dataToLoad);
				}
			}

			toPublish = localBuilder.toString();
			toPublish = tag1 + r.getEnvName() + tag2 + toPublish + tag3;
			builder.append(toPublish);
		}

		String finalDataToPublish = builder.toString();

		// Creating final body
		body = finalDataToPublish + js;

		return body;

	}

	private String getEndPoint(String env, int index, String system, Map<String, Object> yamlMaps) {
		String endpoint = null;

		String systemHost = system + "Hosts";

		@SuppressWarnings("unchecked")
		List<String> systemHosts = (List<String>) yamlMaps.get(systemHost);

		if (systemHosts == null || systemHosts.isEmpty()) {
			return endpoint;
		} else {
			if (!(systemHosts == null) || !(systemHosts.isEmpty())) {
				for (String host : systemHosts) {
					if (index == systemHosts.indexOf(host)) {
						endpoint = host;
					}

				}
				log.info("No of Endpoints configured for system : " + system + "=" + systemHosts.size());
			}
		}

		return endpoint;

	}

	private String getRequestEnvelopeToString(String operation) throws URISyntaxException {
		String requestenvelope = "empty";

		// InputStream requestsPath =
		// getContextClassLoader().getResourceAsStream("/requests");
		URL reqUrl = getClass().getResource("/requests");
		// URL url = this.getClass().getResource("resources/requests");
		if (reqUrl == null) {
			log.info("No requests xmls path provided");
		} else {
			// File dir = new File(url.toURI());
			// File directory = new File(url.toURI());
			File requestsDir = new File(reqUrl.toURI());

			String[] requestsnames;
			requestsnames = requestsDir.list();

			for (String requestsname : requestsnames) {

				if (requestsname.equalsIgnoreCase(operation + ".xml")) {
					File requestFile = new File(requestsDir, requestsname);
					requestenvelope = readLineByLine(requestFile.toString());
				}
			}
		}

		return requestenvelope;
	}

	@SuppressWarnings("unchecked")
	private static List<Operation> getPortTypeOperations(String wsdlUrl) {
		List<Operation> operationList = new ArrayList<Operation>();

		try {
			WSDLFactory factory = WSDLFactory.newInstance();
			WSDLReader reader = factory.newWSDLReader();

			// To avoid writing log while reading
			reader.setFeature("javax.wsdl.verbose", false);
			reader.setFeature("javax.wsdl.importDocuments", false);

			// pass the location/url to the reader for parsing and get list of operations
			Definition wsdlInstance = reader.readWSDL(wsdlUrl);

			Map<String, PortTypeImpl> defMap = wsdlInstance.getAllPortTypes();
			Collection<PortTypeImpl> collection = defMap.values();
			for (PortTypeImpl portType : collection) {
				operationList.addAll(portType.getOperations());
			}

		} catch (WSDLException e) {
			System.out.println("get wsdl operation fail.");
			e.printStackTrace();
		}
		return operationList;
	}

	private static String readLineByLine(String filePath) {
		StringBuilder contentBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
			stream.forEach(s -> contentBuilder.append(s).append("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentBuilder.toString();
	}

	public List<OperationName> getOperations(List<Operation> operationList, String endpoint)
			throws UnsupportedOperationException, SOAPException, IOException, URISyntaxException {
		String soapenvelope = null;

		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		OperationName operationName = new OperationName();
		List<OperationName> operationNames = new ArrayList<OperationName>();
		int i = 0;
		for (Operation opname : operationList) {
			log.info("List of Operations: " + opname.getName());

			soapenvelope = getRequestEnvelopeToString(opname.getName());

			if (!soapenvelope.equalsIgnoreCase("empty")) {

				log.info("Request XML available for Operation: " + opname.getName());
				System.setProperty("java.net.useSystemProxies", "true");
				// log.info("SOAP Request Payload: " + soapenvelope);

				log.info("SOAP endpoint to Call: " + endpoint);

				InputStream is = new ByteArrayInputStream(soapenvelope.getBytes());
				SOAPMessage request = MessageFactory.newInstance().createMessage(null, is);
				MimeHeaders headers = request.getMimeHeaders();

				headers.setHeader("Content-Type", "application/xml");
				request.saveChanges();

				try {
					// log.info("\nInside TRY block");
					SOAPMessage soapResponse = soapConnection.call(request, endpoint);
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					Source sourceContent = soapResponse.getSOAPPart().getContent();

					log.info("Response SOAP Message = ");
					// log.info("\n");
					StreamResult result = new StreamResult(System.out);
					transformer.transform(sourceContent, result);
					log.debug("::" + sourceContent);

					operationName = new OperationName();
					operationName.setOperationName(opname.getName());

					if (!soapResponse.getSOAPBody().hasFault()) {
						operationName.setStatus("Running");
					}
					// Handle Connection timeout / Conn refused
					else if (soapResponse.getSOAPBody() == null) {
						operationName.setStatus("Down");
						// Handle SOAPFault Exception
					} else {
						operationName.setStatus("Fault");
					}
					// operationName.setResponse(sw.toString());
					operationName.setResponse("TODO" + i++);
					operationNames.add(operationName);

				} catch (Exception e) {
					// Covering all down scenario
					operationName = new OperationName();
					operationName.setOperationName(opname.getName());
					operationName.setStatus("Down");
					operationNames.add(operationName);
					log.error("Exception in calling Operation: " + opname.getName());
					// e.printStackTrace();
				}

			} else {
				// log.info("No Request XML is provided for: " + opname.getName());
			}

		}

		return operationNames;
	}

}