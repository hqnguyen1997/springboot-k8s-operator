# Simple Spring Boot K8s Operator

This is an example of a simple K8s Operator with Java-Spring Boot.

## Requirements

* Knowledge of Spring Boot, Java
* Knowledge of Docker
* Knowledge of Kubernetes (K8s) and you can get along with it well
* A minikube cluster local installed or any other k8s cluster

## Let's get it run

Now it's time. Do following steps:

1. Create a namespace

```
k create namespace my-operator
```

2. Deploy the custom resource definition (CRD)

In `src/main/resources/static`:

```
k apply -f crd.yaml
```

3. Build docker image

**NOTE**: If you are using minikube local, make sure you are using docker daemon of minikube before you build image.
So your pod can pull local image correctly.

```
minikube docker-env eval
eval $(minikube -p minikube docker-env)
```

Everything alright? Then run this command in project root directory:

```
mvn spring-boot:build-image 
```

The image now have the name `springboot-k8s-operator`

4. Deploy role, role binding and service account

Your operator need rights to list and do things with K8s resources.
In this example, I create cluster-role and bind it to a service account to namespace
Now we give the operator his rights:

In `src/main/resources/static`:

**NOTE**: modify `namespace` of your service account in cluster-role-binding.yaml (line 8) to your created namespace.

```
k apply -f cluster-role.yaml -n my-operator
k apply -f serviceaccount.yaml -n my-operator
k apply -f cluster-role-binding.yaml -n my-operator
```

5. Finally, deploy the Operator deployment

In `src/main/resources/static`:

```
k apply -f operator-deployment.yaml -n my-operator
```

## Let's do a test

Now your operator is running in your namespace and waiting for custom resources.
Once a custom resource deployed, you will handle it in `com.qansoft.code.springbootoperator.OperatorReconciler.reconcile`.
In this example, the operator should create a config map with text set in custom resource.
Let take a look at `src/main/resources/static/test-customresource.yaml`.

Now we let's deploy it:

```
k apply -f test-customresource.yaml -n my-operator
```

Check the result:

```
k describe configmaps my-config -n my-operator
```

If you see this text or something similar, so it works:

```
Name:         my-config
Namespace:    my-operator
Labels:       <none>
Annotations:  <none>

Data
====
text:
----
Hello World

BinaryData
====

Events:  <none>
```

## Used technologies

* Java
* Spring Boot, Spring Boot Initializr
* Kubernetes
* Minikube
* Docker

## Support

If you like it, don't hesitate to give this repo a star.
If you need any support, please open an issue to describe the case.

Or reach me per email: hquan.nguyen1997@gmail.com