package cc.ruixing.framework.tarn.util.xml;

import org.dom4j.*;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;


public class XMLManagerDom4j implements XMLManager {

	/**
	 * 安全加固(Tarn 2.0+)：统一使用禁用外部实体的 SAXReader，防 XXE
	 * （文件读取 / SSRF）。保留 DOCTYPE 解析能力，仅阻断外部实体与外部 DTD 加载，
	 * 兼容存量带内部 DTD 的配置文件。
	 */
	private static SAXReader secureReader() {
		SAXReader reader = new SAXReader();
		String[] features = {
				"http://xml.org/sax/features/external-general-entities",
				"http://xml.org/sax/features/external-parameter-entities",
				"http://apache.org/xml/features/nonvalidating/load-external-dtd"
		};
		for (String feature : features) {
			try {
				reader.setFeature(feature, false);
			} catch (Exception ignore) {
				// 个别 SAX 实现不支持时跳过，不影响主流程
			}
		}
		return reader;
	}

	@Override
	public Document xmlFileToDom(String strXmlFilePath) throws XMLManagerException {
		if (isEmpty(strXmlFilePath)) {
			throw new XMLManagerException("The xml file path is null or ''");
		}
		Document docImpl = null;
		try {
			docImpl = secureReader().read(new File(strXmlFilePath));
		} catch (DocumentException e) {
			throw new XMLManagerException(e);
		}
		return docImpl;
	}

	private boolean isEmpty(String path) {
		return (path == null || path.length() == 0) ? true : false;
	}

	private boolean isNotEmpty(String arg) {
		return (arg != null && arg.length() > 0) ? true : false;
	}

	@Override
	public Document xmlStrToDom(final String strXmlContent) throws XMLManagerException {
		if (isEmpty(strXmlContent)) {
			throw new XMLManagerException("The xml content is null or ''");
		}
		String s = strXmlContent;
		s = s.trim();
		if (s.length() > 0) {
			if (s.charAt(0) != '<')
				throw new XMLManagerException("The String format is not a xml format");
		}
		Document docImpl = null;
		try {
			// 安全加固(Tarn 2.0+)：替换 DocumentHelper.parseText（其默认开启外部实体，存在 XXE 风险）
			docImpl = secureReader().read(new StringReader(s));
		} catch (Exception e) {
			throw new XMLManagerException(e);
		}
		return docImpl;
	}

	@Override
	public Element getRootElement(Document doc) throws XMLManagerException {
		if (doc == null) {
			throw new XMLManagerException("The document is null, connot get it's root element");
		}
		return doc.getRootElement();
	}

	@Override
	public String getNodeAttribute(Node nNode, String strNodeAttributeName) {
		if (nNode == null) {
			return null;
		}
		if (nNode.getNodeType() == XMLManager.ELEMENT_NODE) {
			Element e = (Element) nNode;
			return e.attributeValue(strNodeAttributeName);
		}
		return null;
	}

	@Override
	public String getNodeValue(Node sNode, boolean isTrim) {
		if (sNode == null) {
			return null;
		}
		try {
			String sv = null;
			if (sNode.getNodeType() == XMLManager.ELEMENT_NODE || sNode.getNodeType() == XMLManager.CDATA_SECTION_NODE) {
				sv = sNode.getText();
				if (sv != null && isTrim) {
					sv = sv.trim();
				}
			}
			return sv;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Node findChildNode(String strChildNodeName, Node nParent) {
		if (nParent == null) {
			return null;
		}
		Element e = (Element) nParent;
		return e.element(strChildNodeName);
	}

	@Override
	public Node findNode(Node nFromNode, String strNodeName) {
		if (nFromNode == null) {
			return null;
		}
		if (nFromNode.getName().equals(strNodeName)) {
			return nFromNode;
		}
		Element e = (Element) nFromNode;
		Iterator it = e.elementIterator();
		Node n = null;
		int nodeType = 1;
		String nodeName = null;
		Node returnNode = null;
		while (returnNode == null) {
			while (it.hasNext()) {
				n = (Element) it.next();
				nodeType = n.getNodeType();
				nodeName = n.getName();
				if (nodeType == XMLManager.ELEMENT_NODE && nodeName.equals(strNodeName)) {
					returnNode = n;
					break;
				} else {
					returnNode = findNode(n, strNodeName);
					if (returnNode != null) {
						break;
					}
				}
			}
		}
		return returnNode;
	}

	@Override
	public List findChildNodes(Node nParent, String strNodeName) {
		if (nParent == null) {
			return null;
		}
		Element e = (Element) nParent;
		return e.elements(strNodeName);
	}

	@Override
	public String getChildNodeValue(Node nParent, String strChildNodeName, boolean isTrim) {
		if (nParent == null) {
			return null;
		}
		if (isNotEmpty(strChildNodeName)) {
			Node cNode = findChildNode(strChildNodeName, nParent);
			return getNodeValue(cNode, true);
		} else {
			return null;
		}
	}

	@Override
	public String getNodeValue(Document dmDom, String strNodeName, boolean isTrim) {
		if (dmDom == null) {
			return null;
		}
		Node sNode = this.findNode(dmDom.getRootElement(), strNodeName);
		return getNodeValue(sNode, true);
	}

	@Override
	public String convertToString(Document dmDom) throws XMLManagerException {
		if (dmDom == null) {
			throw new XMLManagerException("The document is null, cannot convert to String");
		}
		return dmDom.asXML();
	}

	@Override
	public String convertDomToString(Document dmDom) throws XMLManagerException {
		return convertToString(dmDom);
	}

	@Override
	public void setNodeAttribute(Node node, String attriName, String value) {
		Element e = (Element) node;
		Attribute attr = e.attribute(attriName);
		if (attr != null) {
			e.remove(attr);
		}
		e.addAttribute(attriName, value);
	}

	@Override
	public void removeChildNode(Node parentNode, String childName) {
		if (parentNode == null || childName == null) {
			return;
		}
		List childNodes = findChildNodes(parentNode, childName);
		Element pe = (Element) parentNode;
		if (childNodes != null && childNodes.size() > 0) {
			Node ce = null;
			for (int i = 0, len = childNodes.size(); i < len; i++) {
				ce = (Node) childNodes.get(i);
				pe.remove(ce);
			}
		}
		childNodes = null;
	}

	@Override
	public Element populateElement(Document dom, Node parentNode, String eleName, String eleValue, boolean isCData) {
		return populateElement(parentNode, eleName, eleValue, isCData);
	}

	@Override
	public Element populateElement(Node parentNode, String eleName, String eleValue, boolean isCData) {
		Element childElement = DocumentHelper.createElement(eleName);
		Element eParentNode = (Element) parentNode;
		eParentNode.add(childElement);
		if (isNotEmpty(eleValue)) {
			if (isCData) {
				CDATA cdata = DocumentHelper.createCDATA(eleValue);
				childElement.add(cdata);
			} else {
				Text text = DocumentHelper.createText(eleValue);
				childElement.add(text);
			}
		}
		return childElement;
	}

	@Override
	public Element populateCDATAElement(Document dom, Node parentNode, String eleName, String eleValue) {
		return populateCDATAElement(parentNode, eleName, eleValue);
	}

	@Override
	public Element populateCDATAElement(Node parentNode, String eleName, String eleValue) {
		return populateElement(parentNode, eleName, eleValue, true);
	}

	@Override
	public Element populateTextElement(Document dom, Node parentNode, String eleName, String eleValue) {
		return populateTextElement(parentNode, eleName, eleValue);
	}

	@Override
	public Element populateTextElement(Node parentNode, String eleName, String eleValue) {
		return populateElement(parentNode, eleName, eleValue, false);
	}

	@Override
	public Node find(List<Node> nodes, String nodeName) {
		String ndName = null;
		for (Node nd : nodes) {
			ndName = nd.getName();
			if (ndName.equals(nodeName)) {
				return nd;
			}
		}
		return null;
	}

	@Override
	public void renameElementName(Node objectEle, String newName) {
		Element e = (Element) objectEle;
		e.setName(newName);
	}

	@Override
	public void renameElementName(Document doc, Node objectEle, String newName) {
		renameElementName(objectEle, newName);
	}

	@Override
	public void appendUnderElement(Node nAppendTo, Node nAppendNode) {
		Element e = (Element) nAppendTo;
		String ndName = nAppendNode.getName();
		Node nd = findChildNode(ndName, nAppendTo);
		if (nd != null) {
			e.remove(nd);
		}
		e.add(nAppendNode);
	}

	@Override
	public void appendUnderRoot(Document dmDom, Node nAppendNode) {
		appendUnderElement(dmDom.getRootElement(), nAppendNode);
	}

	@Override
	public Document createDocument(String rootName) {
		Element eRoot = DocumentHelper.createElement(rootName);
		return DocumentHelper.createDocument(eRoot);
	}
}
