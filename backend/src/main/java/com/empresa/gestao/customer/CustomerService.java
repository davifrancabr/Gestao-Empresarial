package com.empresa.gestao.customer;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.common.exception.BusinessRuleException;
import com.empresa.gestao.common.exception.ResourceNotFoundException;
import com.empresa.gestao.common.util.CpfValidator;
import com.empresa.gestao.common.util.PageUtils;
import com.empresa.gestao.common.util.TextNormalizer;
import com.empresa.gestao.customer.dto.CustomerRequest;
import com.empresa.gestao.customer.dto.CustomerResponse;
import com.empresa.gestao.sale.SaleMapper;
import com.empresa.gestao.sale.SaleRepository;
import com.empresa.gestao.sale.dto.SaleSummaryResponse;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;

    public CustomerService(CustomerRepository customerRepository, StoreRepository storeRepository,
                           SaleRepository saleRepository, SaleMapper saleMapper) {
        this.customerRepository = customerRepository;
        this.storeRepository = storeRepository;
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(UUID storeId, String search, boolean includeInactive,
                                               Integer page, Integer size, String sort) {
        String term = com.empresa.gestao.movement.MovementService.searchTerm(search);
        String digits = TextNormalizer.onlyDigits(search);
        String digitsTerm = digits == null ? null : "%" + digits + "%";
        Pageable pageable = PageUtils.create(page, size, sort, Map.of(
                "name", "name",
                "createdAt", "createdAt"
        ), "name,asc");
        return PageResponse.from(
                customerRepository.findAll(
                        CustomerSpecifications.filtered(storeId, includeInactive, term, digitsTerm),
                        pageable),
                this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID storeId, UUID id) {
        return toResponse(find(storeId, id));
    }

    @Transactional
    public CustomerResponse create(UUID storeId, CustomerRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada."));
        Customer customer = new Customer();
        customer.setStore(store);
        apply(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(UUID storeId, UUID id, CustomerRequest request) {
        Customer customer = find(storeId, id);
        apply(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void archive(UUID storeId, UUID id) {
        Customer customer = find(storeId, id);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> purchases(UUID storeId, UUID customerId,
                                                       Integer page, Integer size) {
        find(storeId, customerId);
        Pageable pageable = PageUtils.create(page, size, "createdAt,desc", Map.of(
                "createdAt", "createdAt",
                "total", "total"
        ), "createdAt,desc");
        return PageResponse.from(saleRepository.findByCustomer(storeId, customerId, pageable),
                saleMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public Customer findActive(UUID storeId, UUID id) {
        return customerRepository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado ou arquivado."));
    }

    private Customer find(UUID storeId, UUID id) {
        return customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
    }

    private void apply(Customer customer, CustomerRequest request) {
        String name = TextNormalizer.cleanText(request.name());
        if (name == null) {
            throw new BusinessRuleException("Informe o nome do cliente.");
        }
        customer.setName(name);
        customer.setNormalizedName(TextNormalizer.normalizeName(name));

        String cpf = TextNormalizer.onlyDigits(request.cpf());
        if (cpf != null && !CpfValidator.isValid(cpf)) {
            throw new BusinessRuleException("Informe um CPF válido ou deixe o campo vazio.");
        }
        customer.setCpf(cpf);
        customer.setPhone(TextNormalizer.cleanText(request.phone()));
        customer.setEmail(TextNormalizer.cleanText(request.email()));

        if (request.active() != null) {
            customer.setActive(request.active());
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getStore().getId(),
                customer.getName(),
                customer.getCpf(),
                customer.getPhone(),
                customer.getEmail(),
                customer.isActive(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
