package com.itspawan.ioc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class to represent XML bean definition.
 */
public class XMLBean {

    private Map<String, String> attributesMap = new HashMap<String, String>();

    private List<Node> childNodes = new ArrayList<Node>();

    public void setAttributesMap(Map<String, String> attributesMap) {
        this.attributesMap = attributesMap;
    }


    public String getTagAttribute(String key) {
        return attributesMap.get(key);
    }

    public List<Node> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(List<Node> childNodes) {
        this.childNodes = childNodes;
    }

    class Node {

        private String key;
        private String value;
        private boolean isPrimitive;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isPrimitive() {
            return isPrimitive;
        }

        public void setPrimitive(boolean isPrimitive) {
            this.isPrimitive = isPrimitive;
        }

    }

}
