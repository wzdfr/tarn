package cc.ruixing.framework.tarn.util.xml;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.List;


public interface XMLManager {

	public static final int ELEMENT_NODE = 1;

	public static final int CDATA_SECTION_NODE = 4;

	public Document xmlFileToDom(String strXmlFilePath) throws XMLManagerException;

	public Document xmlStrToDom(String strXmlContent) throws XMLManagerException;

	public String convertToString(Document dmDom) throws XMLManagerException;

	public String convertDomToString(Document dmDom) throws XMLManagerException;

	public Element getRootElement(Document doc) throws XMLManagerException;

	public String getNodeAttribute(Node nNode, String strNodeAttributeName);

	public String getNodeValue(Node sNode, boolean isTrim);

	public Node findChildNode(String strChildNodeName, Node nParent);

	public Node findNode(Node nFromNode, String strNodeName);

	public Node find(List<Node> nodes, String nodeName);

	public List<Node> findChildNodes(Node nParent, String strNodeName);

	public String getChildNodeValue(Node nParent, String strChildNodeName, boolean isTrim);

	public String getNodeValue(Document dmDom, String strNodeName, boolean isTrim);

	public void setNodeAttribute(Node node, String attriName, String value);

	public void removeChildNode(Node parentNode, String childName);

	/**
	 * @deprecated <p>
	 *             Note: use the method
	 *             {@link gov.wenzhou.pingyi.business.util.xml.XMLManager#populateElement(org.dom4j.Node, String, String, boolean)}
	 *             instead
	 *             </p>
	 * @param dom
	 * @param parentNode
	 * @param eleName
	 * @param eleValue
	 * @param isCData
	 * @return
	 */
	public Element populateElement(Document dom, Node parentNode, String eleName, String eleValue, boolean isCData);

	public Element populateElement(Node parentNode, String eleName, String eleValue, boolean isCData);

	/**
	 * @deprecated <p>
	 *             Note: use the method
	 *             {@link gov.wenzhou.pingyi.business.util.xml.XMLManager#populateCDATAElement(org.dom4j.Node, String, String)}
	 *             instead
	 *             </p>
	 * @param dom
	 * @param parentNode
	 * @param eleName
	 * @param eleValue
	 * @return
	 */
	public Element populateCDATAElement(Document dom, Node parentNode, String eleName, String eleValue);

	public Element populateCDATAElement(Node parentNode, String eleName, String eleValue);

	/**
	 * @deprecated <p>
	 *             Note: use the method
	 *             {@link gov.wenzhou.pingyi.business.util.xml.XMLManager#populateTextElement(org.dom4j.Node, String, String)}
	 *             instead
	 *             </p>
	 * @param dom
	 * @param parentNode
	 * @param eleName
	 * @param eleValue
	 * @return
	 */
	public Element populateTextElement(Document dom, Node parentNode, String eleName, String eleValue);

	public Element populateTextElement(Node parentNode, String eleName, String eleValue);

	/**
	 * @deprecated <p>
	 *             Note: use the method
	 *             {@link gov.wenzhou.pingyi.business.util.xml.XMLManager#renameElementName(org.dom4j.Node, String)} instead
	 *             </p>
	 * @param doc
	 * @param objectEle
	 * @param newName
	 */
	public void renameElementName(Document doc, Node objectEle, String newName);

	public void renameElementName(Node objectEle, String newName);

	/**
	 * Adds the node new child to the end of the list of the nAppendTo node. if
	 * the new child is already in the tree,it is first removed,
	 * 
	 * @param nAppendTo
	 *            the parent node to append.
	 * @param nAppendNode
	 *            the newchild to be appended.
	 */
	public void appendUnderElement(Node nAppendTo, Node nAppendNode);

	/**
	 * Adds the node new child to the end of the list of the root element of the
	 * document. if the new child is already in the tree,It is first removed.
	 * 
	 * @param dmDom
	 *            The document to add new child under root element.
	 * @param nAppendNode
	 *            The new child to be appended.
	 */
	public void appendUnderRoot(Document dmDom, Node nAppendNode);

	/**
	 * create a document that only contains a root element
	 * 
	 * @param rootName
	 *            the root element's name
	 * @return Document
	 */
	public Document createDocument(String rootName);
}
