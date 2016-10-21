package com.itspawan.ioc;

public class IOCTester {


    public static void main(String[] args) throws Exception {
        XMLReader reader = new XMLReader();
        reader.init();

        Person person = (Person) reader.getBean("person");
        System.out.println(person.getAddress().getLocation());
        System.out.println(person.getAddress().getPincode());

    }

}
