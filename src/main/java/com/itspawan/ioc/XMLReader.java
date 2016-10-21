package com.itspawan.ioc;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;


/**
 * Class responsible for parsing xml file with beans and generating beans.
 */
public class XMLReader {

    private final Map<String, Object> beans = new HashMap<String, Object>();
    private final Map<String, XMLBean> xmlBeans = new HashMap<String, XMLBean>();
    private Document document;

    public void init() {
        parseXmlFile();
        try {
            fillBeansMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void parseXmlFile() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse("config.xml");
            parseDocument();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseDocument() {
        Element docEle = document.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName(XMLTagsAndAttributes.BEAN);
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                XMLBean xmlBean = getXMLBean(el);
                xmlBeans.put(
                        xmlBean.getTagAttribute(XMLTagsAndAttributes.NAME),
                        xmlBean);
            }
        }
    }


    private XMLBean getXMLBean(Element ele) {
        Map<String, String> attributesOfBeanTag = getAttributeForBeanTag(ele);
        List<XMLBean.Node> childNodes = new ArrayList<XMLBean.Node>();

        Map<String, String> attributesOfPropertyTag = getAttributeForPropertyTag(ele,
                XMLTagsAndAttributes.PROPERTY, childNodes);
        XMLBean bean = new XMLBean();

        bean.setAttributesMap(attributesOfBeanTag);

        if (attributesOfPropertyTag != null) {
            XMLBean property = new XMLBean();
            property.setAttributesMap(attributesOfPropertyTag);
            bean.setChildNodes(childNodes);
        }
        return bean;
    }

    private Map<String, String> getAttributeForBeanTag(Element ele) {
        String property_name = ele.getAttribute(XMLTagsAndAttributes.NAME);
        String bean_class = ele.getAttribute(XMLTagsAndAttributes.CLASS);
        String bean_lazy = ele.getAttribute(XMLTagsAndAttributes.LAZY);
        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put(XMLTagsAndAttributes.NAME, property_name);
        attributeMap.put(XMLTagsAndAttributes.CLASS, bean_class);
        attributeMap.put(XMLTagsAndAttributes.LAZY, bean_lazy);
        return attributeMap;
    }

    private Map<String, String> getAttributeForPropertyTag(Element ele,
                                                           String tagName, List<XMLBean.Node> childNodes) {
        NodeList nl = ele.getElementsByTagName(tagName);
        Map<String, String> map = null;

        if (nl != null && nl.getLength() > 0) {
            map = new HashMap<String, String>();
            for (int i = 0; i < nl.getLength(); i++) {
                XMLBean.Node node = new XMLBean().new Node();
                Element el = (Element) nl.item(i);
                String property_name = el
                        .getAttribute(XMLTagsAndAttributes.NAME);
                String referenced_bean = el
                        .getAttribute(XMLTagsAndAttributes.REF_BEAN);
                if (referenced_bean == null || referenced_bean.equals("")) {
                    referenced_bean = el
                            .getAttribute(XMLTagsAndAttributes.VALUE);
                    map.put(property_name, referenced_bean);
                    node.setKey(property_name);
                    node.setValue(referenced_bean);
                    node.setPrimitive(true);

                } else {
                    map.put(property_name, referenced_bean);
                    node.setKey(property_name);
                    node.setValue(referenced_bean);
                    node.setPrimitive(false);
                }
                childNodes.add(node);
            }
        }

        return map;
    }

    private void fillBeansMap() throws Exception {
        Collection<XMLBean> xmlBeansSet = xmlBeans.values();
        for (XMLBean bean : xmlBeansSet) {
            String isLazy = bean.getTagAttribute(XMLTagsAndAttributes.LAZY);
            if (isLazy == null || !isLazy.equalsIgnoreCase("true")) {
                createRealObject(bean);
            }
        }
    }

    private void createRealObject(XMLBean bean) throws Exception {

        Class bean_class = createBean(bean);
        List<XMLBean.Node> listOfChildNodes = bean.getChildNodes();
        Object object = getBeanUsingCGLIB(bean_class, bean_class.newInstance());
        if (listOfChildNodes.size() > 0) {
            fillProperties(bean_class, bean, object);
        }
        String name = bean.getTagAttribute(XMLTagsAndAttributes.NAME);
        beans.put(name, object);

    }


    private Object getBeanUsingCGLIB(Class superClass, Object obj) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(superClass);
        CGLIBInterceptor interceptor = new CGLIBInterceptor(obj, this);
        Callback[] callbacks = new Callback[]{interceptor};
        enhancer.setCallbacks(callbacks);
        return enhancer.create();
    }


    private void fillProperties(Class bean_class, XMLBean bean, Object object) throws Exception {
        Method[] methods = bean_class.getDeclaredMethods();
        Field[] fields = bean_class.getDeclaredFields();
        List<XMLBean.Node> listOfChildNodes = bean.getChildNodes();

        outerloop:
        for (int i = 0; i < listOfChildNodes.size(); i++) {
            boolean isPremitiveOrString = listOfChildNodes.get(i)
                    .isPrimitive();
            String property_name = listOfChildNodes.get(i).getKey();
            String property_value = listOfChildNodes.get(i).getValue();

            if (!isPremitiveOrString) {
                fillChildBeans(object, methods, listOfChildNodes, i, property_name);
                continue outerloop;
            } else {
                fillPrimitiveFields(object, fields, property_name, property_value);
            }


        }
    }

    private void fillPrimitiveFields(Object object, Field[] fields, String property_name, String property_value) throws IllegalAccessException {
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(
                    property_name)) {
                Class field_clazz = field.getType();
                Object realValue = convertToSuitableType(
                        property_value, field_clazz);
                field.setAccessible(true);
                field.set(object, realValue);
                return;
            }
        }
    }

    private void fillChildBeans(Object object, Method[] methods, List<XMLBean.Node> listOfChildNodes, int i, String property_name) throws Exception {
        Object childBean = createChildBeanOrFetchFromMap(listOfChildNodes.get(i), property_name);
        for (Method method : methods) {
            if (isSetterForProperty(property_name, method)) {
                method.invoke(object, new Object[]{childBean});
                return;
            }
        }
    }

    private boolean isSetterForProperty(String property_name, Method method) {
        return method.getName().startsWith("set") && method.getName().toLowerCase().contains(property_name);
    }

    private Object createChildBeanOrFetchFromMap(XMLBean.Node node, String property_name) throws Exception {
        Class chileBeanClass = createBean(xmlBeans.get(node.getValue()));
        Object childBean = beans.get(property_name);
        if (childBean == null) {
            childBean = chileBeanClass.newInstance();
            childBean = getBeanUsingCGLIB(chileBeanClass, childBean);
        }
        return childBean;
    }


    /**
     * convert object to actual type. Handling int and string only for now.
     *
     * @param value
     * @param realDataType
     * @return
     */
    private Object convertToSuitableType(String value, Class realDataType) {
        final Object object;
        if (realDataType.getSimpleName().equalsIgnoreCase("int")) {
            object = Integer.parseInt(value);
        } else {
            object = value;
        }
        return object;
    }

    /**
     * load java object from class loader
     *
     * @param bean
     * @return
     * @throws Exception
     */
    private Class createBean(XMLBean bean) throws Exception {
        String clazz = bean.getTagAttribute(XMLTagsAndAttributes.CLASS);
        Class bean_class = XMLReader.class.getClassLoader().loadClass(clazz);
        return bean_class;
    }


    /**
     * Get bean if available or create one lazily
     *
     * @param key
     * @return
     * @throws Exception
     */
    public Object getBean(String key) throws Exception {
        Object object = beans.get(key);

        if (object == null) {
            return lazyGetterOfBean(key);
        }
        return object;
    }

    /**
     * get beans with lazy true
     *
     * @param key
     * @return
     * @throws Exception
     */
    private Object lazyGetterOfBean(String key) throws Exception {
        try {
            createRealObject(xmlBeans.get(key));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getBean(key);
    }

}
