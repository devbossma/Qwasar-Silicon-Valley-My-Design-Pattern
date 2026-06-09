package dev.saberlabs.persistence;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.persistence.records.RestoredCoffeeShopState;
import dev.saberlabs.persistence.mappers.CustomerPersistenceMapper;
import dev.saberlabs.persistence.mappers.OrderPersistenceMapper;
import dev.saberlabs.persistence.repositories.implementations.file.FileCustomerRepository;
import dev.saberlabs.persistence.repositories.implementations.file.FileOrderRepository;
import dev.saberlabs.persistence.repositories.CustomerRepository;
import dev.saberlabs.persistence.repositories.OrderRepository;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Facade for saving and restoring CoffeeShop state through repositories.
 * This class abstracts away the details of how customers and orders are persisted,
 * allowing the CoffeeShop to interact.
 * *
 * with a simple interface for saving and restoring its state.
 * The facade uses mappers to convert between the domain models (Customer and Order) and the stored representations (StoredCustomer and StoredOrder) used by the repositories.
 * - The saveState():  method extracts the customers from the orders to ensure that only customers with existing orders are saved.
 * - restoreState():  method retrieves all customers and orders from the repositories and updates the CoffeeShop's state accordingly.
 * - clearSavedState():  method allows for clearing all persisted data from the repositories, which can be useful for testing or resetting the state of the application.
 * This class is designed to be flexible and can be easily extended to support additional persistence mechanisms or data formats by implementing new repositories and mappers as needed.
 */
public class CoffeeShopPersistenceFacade {

    private final CoffeeShop coffeeShop;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerPersistenceMapper customerMapper;
    private final OrderPersistenceMapper orderMapper;

    public CoffeeShopPersistenceFacade(@NotNull Path dataDirectory) {
        this(
                CoffeeShop.getInstance(),
                new FileCustomerRepository(dataDirectory.resolve("customers.json")),
                new FileOrderRepository(dataDirectory.resolve("orders.json"))
        );
    }

    public CoffeeShopPersistenceFacade(
            @NotNull CoffeeShop coffeeShop,
            @NotNull CustomerRepository customerRepository,
            @NotNull OrderRepository orderRepository
    ) {
        this.coffeeShop = Objects.requireNonNull(coffeeShop, "Coffee shop cannot be null");
        this.customerRepository = Objects.requireNonNull(customerRepository, "Customer repository cannot be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "Order repository cannot be null");
        this.customerMapper = new CustomerPersistenceMapper();
        this.orderMapper = new OrderPersistenceMapper();
    }

    /**
     * Saves the current state of the coffee shop, including all customers and orders, to the repositories.
     * The customers are extracted from the orders to ensure that only customers with existing orders are saved.
     * This method should be called after any changes to the coffee shop state that need to be persisted.
     */
    public void saveState() {
        List<Order> orders = coffeeShop.getOrders();
        Map<String, Customer> customersById = new LinkedHashMap<>();
        for (Order order : orders) {
            customersById.put(order.getCustomer().getId(), order.getCustomer());
        }
        saveState(customersById.values(), orders);
    }

    /**
     * Saves the provided customers and orders to the repositories. This method can be used to save a specific state
     * of the coffee shop, such as after restoring from a previous state or when only a subset of the state needs to be saved.
     * The customers should be consistent with the orders to ensure data integrity.
     * This method is typically called internally by the saveState() method, but can also be used directly if needed.
     * @param customers the collection of customers to save, which should correspond to the customers associated with the orders
     * @param orders the collection of orders to save.
     */
    public void saveState(@NotNull Collection<Customer> customers, @NotNull Collection<Order> orders) {
        Objects.requireNonNull(customers, "Customers cannot be null");
        Objects.requireNonNull(orders, "Orders cannot be null");

        customerRepository.saveAll(customers.stream()
                .map(customerMapper::toStoredCustomer)
                .toList());

        orderRepository.saveAll(orders.stream()
                .map(orderMapper::toStoredOrder)
                .toList());
    }

    /**
     * Restores the state of the coffee shop from the repositories. This method retrieves all customers and orders from the repositories,
     * converts them back to their respective models, and updates the coffee shop's state accordingly.
     * The customers are restored first to ensure that the orders can be correctly associated with their respective customers.
     * @return the restored state of the coffee shop
     */
    public @NotNull RestoredCoffeeShopState restoreState() {
        List<Customer> customers = customerRepository.findAll().stream()
                .map(customerMapper::toCustomer)
                .toList();

        Map<String, Customer> customersById = new LinkedHashMap<>();
        for (Customer customer : customers) {
            customersById.put(customer.getId(), customer);
        }

        List<Order> orders = orderRepository.findAll().stream()
                .map(storedOrder -> orderMapper.toOrder(storedOrder, customersById))
                .toList();

        coffeeShop.restoreOrders(orders);
        coffeeShop.syncCustomerCounter(customers);
        return new RestoredCoffeeShopState(customers, orders);
    }

    public void clearSavedState() {
        customerRepository.clear();
        orderRepository.clear();
    }
}
