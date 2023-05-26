package com.qansoft.code.springbootoperator;

import com.qansoft.code.springbootoperator.models.V1MyCrd;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OperatorReconciler implements Reconciler {

    private final SharedIndexInformer<V1MyCrd> myCrdInformer;
    private final CoreV1Api coreV1Api;

    protected final String pretty = "true";
    protected final String dryRun = null;
    protected final String fieldManager = "";
    protected final String fieldValidation = "";

    @Autowired
    public OperatorReconciler(SharedIndexInformer<V1MyCrd> myCrdInformer, CoreV1Api coreV1Api) {
        this.myCrdInformer = myCrdInformer;
        this.coreV1Api = coreV1Api;
    }

    /**
     * Create dummy config map as a test
     * @param request
     * @return
     */
    @Override
    public Result reconcile(Request request) {
        String requestName = request.getName();
        String namespace = request.getNamespace();
        String key = namespace + "/" + requestName;
        V1MyCrd customResource = myCrdInformer.getIndexer().getByKey(key);
        // Let's create a configmap
        V1ConfigMap configMap = createConfigMap(customResource);

        // Set owner reference to custom resource
        // so the config map can be removed after custom resource is removed
        configMap
            .getMetadata()
            .addOwnerReferencesItem(
                new V1OwnerReference()
                    .kind(customResource.getKind())
                    .apiVersion(customResource.getApiVersion())
                    .controller(true)
                    .uid(customResource.getMetadata().getUid())
                    .name(requestName)
            );

        try {
            coreV1Api.createNamespacedConfigMap(namespace, configMap, pretty, dryRun, fieldManager, fieldValidation);
            System.out.println("Created config map");
        } catch (ApiException e) {
            // Config map might exist, so update it
            try {
                coreV1Api.replaceNamespacedConfigMap(
                    configMap.getMetadata().getName(),
                    namespace,
                    configMap,
                    pretty,
                    dryRun,
                    fieldManager,
                    fieldValidation
                );
            } catch (ApiException ex) {
                // Other error
                throw new RuntimeException(ex);
            }
        }

        return new Result(false);
    }

    private V1ConfigMap createConfigMap(V1MyCrd customResource) {
        Map<String, String> data = new HashMap<>();
        data.put("text", customResource.getSpec().getConfigMapText());

        V1ConfigMap configMap = new V1ConfigMap()
                .apiVersion("v1")
                .metadata(new V1ObjectMeta().name("my-config"))
                .data(data);

        return configMap;
    }
}
