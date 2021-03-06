package com.services.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.model.ServiceInstanceBean;
import com.services.model.ServiceInstanceBean.ServiceName;
import com.services.util.ServiceInstanceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.services.util.ServiceInstanceConstant.*;

public class ServiceInstanceManager {

    private final Logger log = LoggerFactory.getLogger(ServiceInstanceManager.class);

    @Autowired
    private final ServiceInstanceBean instanceBean;
    private final ObjectMapper objectMapper;

    public ServiceInstanceManager(ObjectMapper mapper, ServiceInstanceBean serviceInstanceBean) {
        this.objectMapper = mapper;
        this.instanceBean = serviceInstanceBean;
    }

    public ServiceInstanceBean populateServiceInstanceBean(String serviceName) {
        instanceBean.setRequestedServiceName(serviceName);
        requestedServiceEndpoints(serviceName);
        return instanceBean;
    }


    private void requestedServiceEndpoints(String serviceName) {
        switch (ServiceName.valueOf(serviceName)) {

           // Generalize switch case for all services
          //  Add all the services with number of instances in this switch case
            case servicename:
                setServiceInstanceMap(SERVICE_URL, SERVICE_INSTANCES);
                break;

        }
    }


        private void setServiceInstanceMap(final String address, int numberOfInstancesExpected) {
        Map<String, Integer> servicesMap = new HashMap<>();
        Set<String> serviceStateSet = new HashSet<>();
        BufferedReader serviceInstanceReader;
        String servicePingOutput;
        JsonNode serviceInstance;
        JsonNode serviceStateNodeObj;
        String serviceState;
        String finalServiceState = null;
        int currentInstanceCount = 0;
        String instanceId;
        int port;
        URL url;
        try {
            url = new URL(address);
            for (int i = 1; i <= NO_OF_ITERATION; i++) {
                try {
                    serviceInstanceReader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                    servicePingOutput = org.apache.commons.io.IOUtils.toString(serviceInstanceReader);
                    serviceInstance = objectMapper.readTree(servicePingOutput).get(SERVICE_INSTANCE);
                    serviceStateNodeObj = objectMapper.readTree(servicePingOutput).get(SERVICE_STATE);
                    serviceState = serviceStateNodeObj != null ? serviceStateNodeObj.asText() : null;
                    instanceId = serviceInstance != null ? serviceInstance.get(INSTANCE_ID).asText() : null;
                    port = serviceInstance != null ? serviceInstance.get(PORT).asInt() : null;

                    if (instanceId != null && servicesMap.get(instanceId) != null) {
                        continue;
                    }
                    servicesMap.put(instanceId, port);
                    assert serviceState != null;
                    serviceStateSet.add(serviceState);
                    currentInstanceCount++;
                    if (servicesMap.size() == numberOfInstancesExpected) {
                        break;
                    }
                } catch (IOException ioException) {
                    log.error("Error in connection", ioException);
                } catch (Exception exception) {
                    log.error("Error in fetching instances", exception);
                }

            }
            if (serviceStateSet.contains(FAULT)) {
                finalServiceState = FAULT;

            } else if (serviceStateSet.contains(ONLINE)) {
                finalServiceState = ONLINE;

            }

        } catch (MalformedURLException malFormedException) {
            log.error("Error in URL", malFormedException);
        } catch (Exception exception) {
            log.error("Error in fetching instances", exception);
        }
        instanceBean.setServiceInstanceMap(ServiceInstanceUtil.getSortedInstanceMap(servicesMap));
        instanceBean.setExpectedServiceInstances(numberOfInstancesExpected);
        instanceBean.setActualServiceInstances(currentInstanceCount);
        instanceBean.setServiceState(finalServiceState);

    }


}
