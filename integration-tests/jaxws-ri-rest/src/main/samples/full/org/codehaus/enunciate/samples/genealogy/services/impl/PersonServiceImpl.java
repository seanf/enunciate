/*
 * Copyright 2006-2008 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.enunciate.samples.genealogy.services.impl;

import com.sun.jersey.multipart.FormDataParam;
import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.samples.genealogy.data.*;
import org.codehaus.enunciate.samples.genealogy.services.PersonService;
import org.codehaus.enunciate.samples.genealogy.services.ServiceException;
import org.joda.time.DateTime;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Ryan Heaton
 */
@WebService (
  endpointInterface = "org.codehaus.enunciate.samples.genealogy.services.PersonService"
)
@com.sun.xml.ws.developer.StreamingAttachment (
  parseEagerly = true,
  memoryThreshold = 40000L
)
@Path ("")
@StatusCodes({@ResponseCode ( code = 401, condition = "If authentication is needed.")})
public class PersonServiceImpl implements PersonService {

  public Person storePerson(Person person) {
    return person;
  }

  public PersonExt readExtPerson(String id) {
    return null;
  }

  public Collection<Person> readPersons(Collection<String> personIds) {
    if (personIds == null) {
      return null;
    }

    ArrayList<Person> persons = new ArrayList<Person>(personIds.size());
    for (String personId : personIds) {
      Person person = new Person();
      person.setId(personId);
      persons.add(person);

      Event event = new Event();
      event.setDate(new DateTime(1L));
      person.setEvents(Arrays.asList(event));
    }

    return persons;
  }

  public void deletePerson(String PErsonId, String message) throws ServiceException {
    if (PErsonId == null) {
      throw new ServiceException("a person id must be supplied", "no person id.");
    }
  }

  public Map<RelationshipType, Person> readFamily(String personId) throws ServiceException {
    HashMap<RelationshipType, Person> pedigree = new HashMap<RelationshipType, Person>();
    Person person = new Person();
    person.setId("parent");
    pedigree.put(RelationshipType.parent, person);
    Person spouse = new Person();
    spouse.setId("spouse");
    pedigree.put(RelationshipType.spouse, spouse);
    Person child = new Person();
    child.setId(personId);
    pedigree.put(RelationshipType.child, child);
    return pedigree;
  }

  public RootElementMapWrapper storeGenericProperties(RootElementMapWrapper map) throws ServiceException {
    return map;
  }

  public void uploadFiles(DataHandler[] files, String length) throws ServiceException {
    String[] params = length.split(";");
    int fileCount = Integer.parseInt(params[0]);
    if (files.length != fileCount) {
      throw new RuntimeException("File length doesn't match.");
    }

    for (int i = 0; i < files.length; i++) {
      DataHandler file = files[i];
      int fileLength = Integer.parseInt(params[i + 1]);
      byte[] bytes = new byte[fileLength];
      try {
        InputStream in = file.getInputStream();
        int len = in.read(bytes);
        if (len < fileLength) {
          throw new RuntimeException("Non-matching file length.  Was " + len + " expected " + fileLength);
        }
        if (in.read() >= 0) {
          throw new RuntimeException("Non-matching file length.  Was bigger than " + fileLength);
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void postMultipart( InputStream file1, InputStream file2 ) {
  }
}
