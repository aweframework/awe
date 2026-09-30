package com.almis.awe.test.model;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.FilterColumn;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 *
 * @author pvidal
 *
 */
@Service
public class UserList extends ServiceConfig {

  /**
   * Load users Json file
   * @return User list
   * @throws AWException Error retrieving user list
   */
  public ServiceData loadUsersJsonFile() throws AWException {

    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    final long simulateNumFile = 10;
    int j = 1;

    try {

      // Read json file
      Resource resource = new ClassPathResource("static/10000_complex.json");
      if (resource.exists()) {
        InputStream resourceInputStream = resource.getInputStream();
        // create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // convert json string to object
        List<User> userList = objectMapper.readValue(resourceInputStream, new TypeReference<>() {
        });

        // Build dataList
        for (int i = 0; i < simulateNumFile; i++) {
          for (User user : userList) {
            dataList.addRow(user.toDatalistRow(j++));
          }
        }

        dataList.setRecords((long) userList.size() * simulateNumFile);
        serviceData.setDataList(dataList);
      }
    } catch (Exception ex) {
      throw new AWException("Error reading json file", ex);
    }
    return serviceData;
  }

  /**
   * Load tree Json file
   * @return User list
   * @throws AWException Error retrieving user list
   */
  public ServiceData loadTreeJsonFile() throws AWException {

    ServiceData serviceData = new ServiceData();

    try {

      // Read json file
      Resource resource = new ClassPathResource("static/tree_data.json");
      if (resource.exists()) {
        InputStream resourceInputStream = resource.getInputStream();
        // create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // convert json string to object
        List<TreeData> treeDataList = objectMapper.readValue(resourceInputStream, new TypeReference<>() {});

        // Get datalist
        DataList dataList = DataListUtil.fromBeanList(treeDataList);

        // Build dataList
        serviceData.setDataList(dataList);
      }
    } catch (Exception ex) {
      throw new AWException("Error reading json file", ex);
    }
    return serviceData;
  }

  /**
   * Load tree Json file
   * @return User list
   * @throws AWException Error retrieving user list
   */
  public ServiceData longSuggest(String suggest) throws AWException {

    ServiceData serviceData = new ServiceData();

    try {

      // Read json file
      Resource resource = new ClassPathResource("static/10000_complex.json");
      if (resource.exists()) {
        InputStream resourceInputStream = resource.getInputStream();
        // create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // convert json string to object
        List<User> userList = objectMapper.readValue(resourceInputStream, new TypeReference<>() {});

        // Get datalist
        DataList dataList = DataListUtil.fromBeanList(userList);
        DataListUtil.filterContains(dataList, new FilterColumn("name", suggest));

        // Build dataList
        serviceData.setDataList(dataList);
      }
    } catch (Exception ex) {
      throw new AWException("Error reading json file", ex);
    }
    return serviceData;
  }

}
