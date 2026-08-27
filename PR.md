# My Framework
Framework with Custom Annotations

## Objective
Design a flexible framework for handling client interactions in various business applications, inspired by the coffee shop application. You will utilize Java Reflection and custom annotations to allow dynamic handling of different business objects and interactions.
## Assignment Overview:
1. **Framework Design**: Design a framework that can handle various client interactions for different business types.
2. **Utilize Reflection**: Implement reflection to dynamically manage classes and methods associated with client interactions.
3. **Create Custom Meta-Annotations**: Create a unique annotations for a specific functionalities in the framework.
4. **Extend the Coffee Shop Application**: Use the framework to implement client interactions in the coffee shop application.

## Requirements:
---
1. **Framework Structure**
   Create a new package for your framework, containing:
   - **InteractionHandler:** A class to manage interactions.
   - **BusinessObject:** An interface that different business classes (like CoffeeShop, Bookstore, etc.) will implement.
   - **ReflectionUtil:** A utility class to handle reflection-related tasks.
   - Custom meta-annotation classes.
2. **BusinessObject Interface**:
   Define a common interface that different business types will implement.
   Example:
   ```java 
    public interface BusinessObject {
        void processRequest(String request);
    }   
   ```
3. **Custom Meta-Annotation**:
   Create a meta-annotation called `@RequestMappingMeta`.
Example:
   ```java
    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;
    
    @Target(ElementType.ANNOTATION_TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequestMappingMeta {
    }
   ```
4. **Define Concrete Annotations using the Meta-Annotation**:
   Create a specific request-handling annotations using `@RequestMappingMeta`
- Example for Handling Orders:
    ```java
     @RequestMappingMeta
     @Target(ElementType.METHOD)
     @Retention(RetentionPolicy.RUNTIME)
     public @interface OrderHandler {
     }
    ```
- Example for Handling Chat:
    ```java
     @RequestMappingMeta
     @Target(ElementType.METHOD)
     @Retention(RetentionPolicy.RUNTIME)
     public @interface ChatHandler {
     }
    ```
5. **InteractionHandler Class**:
   Modify this class to use reflection to detect any annotation that is annotated with `@RequestMappingMeta`.
Example:
   ```java
    public class InteractionHandler {

        public void handleInteraction(BusinessObject businessObject, String requestType, String request) {
            Method[] methods = businessObject.getClass().getMethods();
    
            for (Method method : methods) {
                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation.annotationType().isAnnotationPresent(RequestMappingMeta.class)) {
                        try {
                            method.invoke(businessObject, request);
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            System.out.println("No handler found for request type: " + requestType);
        }
    }
   ```
6. **ReflectionUtil Class**:
   Create utility methods for managing class loading and method invocation.
Example:
```java
import java.lang.reflect.Method;

public class ReflectionUtil {

    public static void invokeMethod(Object obj, String methodName, String parameter) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, parameter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
7. **Update Coffee Shop Application**:
   Refactor the existing coffee shop application to use the new framework and implement the BusinessObject interface.
Example:
```java
public class CoffeeShop implements BusinessObject {

    @OrderHandler
    public void handleOrder(String orderDetails) {
        System.out.println("Processing order: " + orderDetails);
    }

    @ChatHandler
    public void handleChat(String message) {
        System.out.println("Chat message: " + message);
    }

    @Override
    public void processRequest(String request) {
    }
}
```
8. **Create a Test Client**:
    Implement a test client to demonstrate the framework's capabilities and demonstrate how to use the framework with different business types.
Example:
```java
public class BusinessTestClient {
    public static void main(String[] args) {
        InteractionHandler handler = new InteractionHandler();
        CoffeeShop coffeeShop = new CoffeeShop();

        handler.handleInteraction(coffeeShop, "order", "1 Cappuccino");
        handler.handleInteraction(coffeeShop, "chat", "Hello, barista!");
        handler.handleInteraction(coffeeShop, "feedback", "Great service!");
    }
}
```