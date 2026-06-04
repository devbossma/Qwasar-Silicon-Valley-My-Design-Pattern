package dev.saberlabs.persistence;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.persistence.records.RestoredCoffeeShopState;
import dev.saberlabs.persistence.mappers.CustomerPersistenceMapper;
import dev.saberlabs.persistence.mappers.OrderPersistenceMapper;
import dev.saberlabs.persistence.repositories.CustomerRepository;
import dev.saberlabs.persistence.repositories.implimentatioins.file.FileCustomerRepository;
import dev.saberlabs.persistence.repositories.implimentatioins.file.FileOrderRepository;
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

    public void saveState() {
        List<Order> orders = coffeeShop.getOrders();
        Map<String, Customer> customersById = new LinkedHashMap<>();
        for (Order order : orders) {
            customersById.put(order.getCustomer().getId(), order.getCustomer());
        }
        saveState(customersById.values(), orders);
    }

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
