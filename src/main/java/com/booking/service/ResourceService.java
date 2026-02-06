package com.booking.service;

import com.booking.dto.common.PageResponse;
import com.booking.dto.resource.*;
import com.booking.entity.Resource;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getAllResources(
            ResourceFilterRequest filter, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        
        Specification<Resource> spec = buildSpecification(filter);
        Page<Resource> resourcePage = resourceRepository.findAll(spec, pageable);

        List<ResourceResponse> content = resourcePage.getContent()
                .stream()
                .map(this::mapToResourceResponse)
                .collect(Collectors.toList());

        return PageResponse.<ResourceResponse>builder()
                .content(content)
                .page(resourcePage.getNumber())
                .size(resourcePage.getSize())
                .totalElements(resourcePage.getTotalElements())
                .totalPages(resourcePage.getTotalPages())
                .first(resourcePage.isFirst())
                .last(resourcePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
        return mapToResourceResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(CreateResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .isActive(true)
                .build();

        Resource savedResource = resourceRepository.save(resource);
        return mapToResourceResponse(savedResource);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, UpdateResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));

        if (request.getName() != null) {
            resource.setName(request.getName());
        }
        if (request.getLocation() != null) {
            resource.setLocation(request.getLocation());
        }
        if (request.getCapacity() != null) {
            resource.setCapacity(request.getCapacity());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            resource.setIsActive(request.getIsActive());
        }

        Resource savedResource = resourceRepository.save(resource);
        return mapToResourceResponse(savedResource);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
        
        // Soft delete - deactivate instead of removing
        resource.setIsActive(false);
        resourceRepository.save(resource);
    }

    private Specification<Resource> buildSpecification(ResourceFilterRequest filter) {
        Specification<Resource> spec = Specification.where(null);

        if (filter.getName() != null && !filter.getName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
        }

        if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("location")), "%" + filter.getLocation().toLowerCase() + "%"));
        }

        if (filter.getCapacityMin() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("capacity"), filter.getCapacityMin()));
        }

        if (filter.getIsActive() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), filter.getIsActive()));
        } else {
            // By default, show only active resources
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), true));
        }

        return spec;
    }

    private ResourceResponse mapToResourceResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .location(resource.getLocation())
                .capacity(resource.getCapacity())
                .description(resource.getDescription())
                .isActive(resource.getIsActive())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
