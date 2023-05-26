package com.qansoft.code.springbootoperator;

import com.qansoft.code.springbootoperator.models.V1MyCrd;
import com.qansoft.code.springbootoperator.models.V1MyCrdList;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.time.Duration;

@SpringBootApplication
public class OperatorAwsAccessApplication {

	public static void main(String[] args) {
		SpringApplication.run(OperatorAwsAccessApplication.class, args);
	}

	@Bean
	SharedIndexInformer<V1MyCrd> bpcSharedIndexInformer(SharedInformerFactory sharedInformerFactory, ApiClient apiClient) {
		GenericKubernetesApi<V1MyCrd, V1MyCrdList> api = new GenericKubernetesApi<>(
				V1MyCrd.class,
				V1MyCrdList.class,
				"com.qansoft",
				"v1",
				"mycrds",
				apiClient
		);

		return sharedInformerFactory.sharedIndexInformerFor(api, V1MyCrd.class, 0);
	}

	@Bean
	ApiClient apiClient() throws IOException {
		return Config.defaultClient();
	}

	@Bean
	CoreV1Api coreV1Api(ApiClient apiClient) {
		return new CoreV1Api(apiClient);
	}

	@Bean
	Controller controller(SharedInformerFactory sharedInformerFactory, SharedIndexInformer<V1MyCrd> informer, OperatorReconciler reconciler) {
		return ControllerBuilder
				.defaultBuilder(sharedInformerFactory)
				.watch(queue -> ControllerBuilder
						.controllerWatchBuilder(V1MyCrd.class, queue)
						.withResyncPeriod(Duration.ofSeconds(1))
						.withOnUpdateFilter((a, b) -> !a.getMetadata().getResourceVersion().equals(b.getMetadata().getResourceVersion()))
						.build())
				.withReconciler(reconciler)
				.withName("operatorController")
				.build();
	}

	@Bean
	CommandLineRunner runner (SharedInformerFactory sharedInformerFactory, Controller bpcController) {
		return args -> {
			System.out.println("Starting informers..");
			sharedInformerFactory.startAllRegisteredInformers();

			System.out.println("running controller..");
			bpcController.run();
		};
	}
}
